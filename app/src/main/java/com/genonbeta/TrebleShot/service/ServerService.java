package com.genonbeta.TrebleShot.service;

import android.app.*;
import android.content.*;
import android.net.wifi.*;
import android.os.*;
import android.support.v4.app.*;
import android.util.*;
import com.genonbeta.CoolSocket.*;
import com.genonbeta.TrebleShot.config.*;
import com.genonbeta.TrebleShot.helper.*;
import java.io.*;
import java.net.*;
import org.json.*;

public class ServerService extends Service
{
	public static final String TAG = "ServerService";

	public final static String ACTION_CHECK_AVAILABLES = "com.genonbeta.TrebleShot.server.OPEN_NEW_SERVER_SOCKET";
	public final static String ACTION_CANCEL_RECEIVING = "com.genonbeta.TrebleShot.server.CANCEL_RECEIVING";
	public final static String ACTION_FILE_LIST_CHANGED = "com.genonbeta.TrebleShot.action.FILE_LIST_CHANGED";

	private ServerRunnable mRunnable = new ServerRunnable();
	private boolean mIsBreakRequested = false;

	private NotificationPublisher mPublisher;
	private WifiManager.WifiLock mWifiLock;
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

		mWifiLock = ((WifiManager) getSystemService(Service.WIFI_SERVICE)).createWifiLock(TAG);
		mPublisher = new NotificationPublisher(this);

		mReceive.setNotifyDelay(2000);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
		{
			if (ACTION_CHECK_AVAILABLES.equals(intent.getAction()))
			{
				Log.d(TAG, "Thread started for request; status = " + (startEngine() ? "now started" : "already started"));
			}
			else if (ACTION_CANCEL_RECEIVING.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_ACCEPT_ID))
			{
				final int acceptId = intent.getIntExtra(CommunicationService.EXTRA_ACCEPT_ID, -1);

				ApplicationHelper.removeReceivers(acceptId);

				mIsBreakRequested = true;
			}
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

	public File getUniqueFile(File file) throws IOException
	{
		if (file.isFile())
		{
			file = FileUtils.getUniqueFile(file);
			file.createNewFile();
		}

		return file;
	}

	private class ServerRunnable implements Runnable
	{
		@Override
		public void run()
		{
			mWifiLock.acquire();

			Log.d(TAG, "Receiver count " + ApplicationHelper.getReceivers().size());

			int preNotified = 0;

			for (AwaitedFileReceiver receiver : ApplicationHelper.getReceivers())
			{
				preNotified++;

				Log.d(TAG, "ReceiverThread running for receiver = " + receiver.fileName);

				try
				{
					File file = getUniqueFile(new File(FileUtils.getSaveLocationForFile(getApplicationContext(), receiver.fileName)));

					if (!mIsBreakRequested)
					{
						mReceive.receiveOnCurrentThread(0, file, receiver.fileSize, AppConfig.DEFAULT_BUFFER_SIZE, 10000, receiver);

						if (!receiver.processCancelled && !mIsBreakRequested)
						{
							if (preNotified <= 1)
								mPublisher.notifyFileReceived(receiver, file, ApplicationHelper.getDeviceList().get(receiver.ip));
							else
								mPublisher.notifyFileReceivedMulti(preNotified);
						}
					}
				}
				catch (Exception e)
				{
					Log.d(TAG, "ongoing receiving error = " + e.getMessage());
				}
				finally
				{
					mPublisher.cancelNotification(NotificationPublisher.NOTIFICATION_ID_RECEIVING);
					ApplicationHelper.removeReceiver(receiver);

					sendBroadcast(new Intent(ACTION_FILE_LIST_CHANGED));
				}
			}

			mIsBreakRequested = false;

			Log.d(TAG, "Thread done");
			mWifiLock.release();

			if (ApplicationHelper.getReceivers().size() > 0)
				run();
		}

		public boolean requestBreak()
		{
			if (mIsBreakRequested == true)
				return false;

			mIsBreakRequested = true;

			return true;
		}
	}

	private class Receive extends CoolTransfer.Receive<AwaitedFileReceiver>
	{
		@Override
		public void onError(int port, File file, Exception error, AwaitedFileReceiver extra)
		{
			if (!mIsBreakRequested)
			{
				mPublisher.notifyReceiveError(extra.fileName);
				extra.processCancelled = true;
			}

			if (file != null && file.isFile())
				file.delete();
		}

		@Override
		public void onNotify(Socket socket, int port, File file, int percent, AwaitedFileReceiver extra)
		{
			NetworkDevice device = ApplicationHelper.getDeviceList().get(extra.ip);
			mPublisher.notifyFileReceiving(extra, device, percent);
		}

		@Override
		public void onTransferCompleted(int port, File file, AwaitedFileReceiver extra)
		{

		}

		@Override
		public void onSocketReady(final ServerSocket serverSocket, int port, File file, final AwaitedFileReceiver extra)
		{
			CoolCommunication.Messenger.sendOnCurrentThread(extra.ip, AppConfig.COMMUNATION_SERVER_PORT, null,
				new JsonResponseHandler()
				{
					@Override
					public void onJsonMessage(Socket socket, CoolCommunication.Messenger.Process process, JSONObject json)
					{
						try
						{
							json.put("request", "file_transfer_notify_server_ready");
							json.put("requestId", extra.requestId);
							json.put("socketPort", serverSocket.getLocalPort());

							JSONObject response = new JSONObject(process.waitForResponse());

							if (!response.getBoolean("result"))
								this.onError(new Exception("Request rejected"));
						}
						catch (JSONException e)
						{
							this.onError(e);
						}
					}

					@Override
					public void onError(Exception exception)
					{
						Log.d(TAG, "Error while receiver is waiting response of sender");
						extra.processCancelled = true;
					}
				}
			);
		}

		@Override
		public boolean onBreakRequest(int port, File file, AwaitedFileReceiver extra)
		{
			return super.onBreakRequest(port, file, extra) || mIsBreakRequested || extra.processCancelled;
		}

		@Override
		public boolean onStart(int port, File file, AwaitedFileReceiver extra)
		{
			if (extra.processCancelled)
				return false;

			NetworkDevice device = ApplicationHelper.getDeviceList().get(extra.ip);
			mPublisher.notifyFileReceiving(extra, device, 0);

			return true;
		}
	}
}
