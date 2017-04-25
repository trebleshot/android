package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.util.Log;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.TrebleShot.helper.JsonResponseHandler;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationPublisher;
import com.genonbeta.TrebleShot.receiver.FileChangesReceiver;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerService extends Service
{
	public static final String TAG = "ServerService";

	public final static String ACTION_CHECK_AVAILABLE = "com.genonbeta.TrebleShot.server.OPEN_NEW_SERVER_SOCKET";
	public final static String ACTION_CANCEL_RECEIVING = "com.genonbeta.TrebleShot.server.CANCEL_RECEIVING";

	private ServerRunnable mRunnable = new ServerRunnable();

	private NotificationPublisher mPublisher;
	private WifiManager.WifiLock mWifiLock;
	private Transaction mTransaction;
	private Receive mReceive = new Receive();
	private Thread mThread;

	@Override
	public IBinder onBind(Intent p1)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE)).createWifiLock(TAG);
		mPublisher = new NotificationPublisher(this);
		mTransaction = new Transaction(this);

		mReceive.setNotifyDelay(2000);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			if (ACTION_CHECK_AVAILABLE.equals(intent.getAction()))
			{
				Log.d(TAG, "Thread started for request; status = " + (startEngine() ? "now started" : "already started"));
			}
			else if (ACTION_CANCEL_RECEIVING.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_ACCEPT_ID))
			{
				final int acceptId = intent.getIntExtra(CommunicationService.EXTRA_ACCEPT_ID, -1);

				mTransaction.removeTransactionGroup(acceptId);
			}

		return START_STICKY;
	}

	public boolean startEngine()
	{
		if (mThread == null || Thread.State.TERMINATED.equals(mThread.getState()))
		{
			mThread = new Thread(mRunnable);
			mThread.start();

			return true;
		}

		Log.d(TAG, "Thread state = " + mThread.getState());

		return false;
	}

	private class ServerRunnable implements Runnable
	{
		@Override
		public void run()
		{
			mWifiLock.acquire();
			ArrayList<AwaitedFileReceiver> receiverList = mTransaction.getReceivers();

			do
			{
				doJob(receiverList);
				receiverList = mTransaction.getReceivers(new SQLQuery.Select(MainDatabase.TABLE_TRANSFER)
								.setWhere(MainDatabase.FIELD_TRANSFER_TYPE + "=? AND " + MainDatabase.FIELD_TRANSFER_FLAG + "=?",
										String.valueOf(MainDatabase.TYPE_TRANSFER_TYPE_OUTGOING),
										Transaction.Flag.PENDING.toString()));
			} while (receiverList.size() > 0);

			mWifiLock.release();
		}

		public void doJob(ArrayList<AwaitedFileReceiver> receiverList)
		{
			Log.d(TAG, "Receiver count " + receiverList.size());

			mReceive.multiCounter = 0;

			for (AwaitedFileReceiver receiver : receiverList)
			{
				mReceive.multiCounter++;

				Log.d(TAG, "ReceiverThread running for receiver = " + receiver.fileName);

				try
				{
					File file = new File(FileUtils.getSaveLocationForFile(getApplicationContext(), receiver.fileName));

					if (Transaction.Flag.PENDING.equals(receiver.flag) && file.isFile())
					{
						file = FileUtils.getUniqueFile(file);
						receiver.fileName = file.getName();
					}

					file.createNewFile();

					mReceive.receiveOnCurrentThread(0, file, receiver.fileSize, AppConfig.DEFAULT_BUFFER_SIZE, 10000, receiver);
				} catch (Exception e)
				{
					e.printStackTrace();
				}
				finally
				{
					mPublisher.cancelNotification(NotificationPublisher.NOTIFICATION_ID_RECEIVING);

					sendBroadcast(new Intent(FileChangesReceiver.ACTION_FILE_LIST_CHANGED)
							.putExtra(FileChangesReceiver.NOT_COMPLETE_JOB, true));
				}
			}

			sendBroadcast(new Intent(FileChangesReceiver.ACTION_FILE_LIST_CHANGED));

			Log.d(TAG, "Thread done");
		}
	}

	private class Receive extends CoolTransfer.Receive<AwaitedFileReceiver>
	{
		public int multiCounter = 0;

		@Override
		public void onError(ReceiveHandler handler, Exception error)
		{
			handler.getExtra().flag = Transaction.Flag.ERROR;

			mTransaction.updateTransaction(handler.getExtra());
			mPublisher.notifyReceiveError(handler.getExtra().fileName);
		}

		@Override
		public void onNotify(ReceiveHandler handler, int percent)
		{
			NetworkDevice device = ApplicationHelper.getDeviceList().get(handler.getExtra().ip);
			mPublisher.notifyFileReceiving(handler.getExtra(), device, percent);
		}

		@Override
		public void onTransferCompleted(ReceiveHandler handler)
		{
			mTransaction.removeTransaction(handler.getExtra());

			if (multiCounter <= 1)
				mPublisher.notifyFileReceived(handler.getExtra(), handler.getFile(), ApplicationHelper.getDeviceList().get(handler.getExtra().ip));
			else
				mPublisher.notifyFileReceivedMulti(multiCounter);
		}

		@Override
		public void onInterrupted(ReceiveHandler handler)
		{
			File file = handler.getFile();

			if (file != null && file.isFile())
				file.delete();
		}

		@Override
		public void onSocketReady(final ReceiveHandler handler, final ServerSocket serverSocket)
		{
			CoolCommunication.Messenger.sendOnCurrentThread(handler.getExtra().ip, AppConfig.COMMUNATION_SERVER_PORT, null,
					new JsonResponseHandler()
					{
						@Override
						public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
						{
							try
							{
								json.put("request", "file_transfer_notify_server_ready");
								json.put("requestId", handler.getExtra().requestId);
								json.put("socketPort", serverSocket.getLocalPort());

								JSONObject response = new JSONObject(process.waitForResponse());

								if (!response.getBoolean("result"))
								{
									this.onError(new Exception("Request rejected"));
									serverSocket.close();
								}
							} catch (JSONException e)
							{
								this.onError(e);
							} catch (IOException e)
							{
								e.printStackTrace();
							}
						}

						@Override
						public void onError(Exception exception)
						{
							exception.printStackTrace();
							Log.d(TAG, "Error while receiver is waiting response of sender");
						}
					}
			);
		}

		@Override
		public boolean onCheckStatus(ReceiveHandler handler)
		{
			// TODO: 4/25/17 here is also for checking if the break requested
			return super.onCheckStatus(handler);
		}

		@Override
		public boolean onStart(ReceiveHandler handler)
		{
			Log.d(TAG, "onStart(): " + handler.getFile().getName());

			NetworkDevice device = ApplicationHelper.getDeviceList().get(handler.getExtra().ip);
			mPublisher.notifyFileReceiving(handler.getExtra(), device, 0);

			return true;
		}
	}
}
