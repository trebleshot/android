package com.genonbeta.TrebleShot.service;

import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.FileUtils;
import com.genonbeta.TrebleShot.helper.JsonResponseHandler;
import com.genonbeta.TrebleShot.receiver.FileChangesReceiver;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class ServerService extends AbstractTransactionService<AwaitedFileReceiver>
{
	public static final String TAG = "ServerService";

	public final static String ACTION_START_RECEIVING = "com.genonbeta.TrebleShot.action.START_RECEIVING";

	private Receive mReceive = new Receive();

	@Override
	public IBinder onBind(Intent p1)
	{
		return null;
	}

	@Override
	public ArrayList<CoolTransfer.TransferHandler<AwaitedFileReceiver>> onProcessList()
	{
		return mReceive.getProcessList();
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		mReceive.setNotifyDelay(2000);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
		{
			if (ACTION_START_RECEIVING.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_GROUP_ID))
			{
				int groupId = intent.getIntExtra(CommunicationService.EXTRA_GROUP_ID, -1);
				AwaitedFileReceiver runningReceiver = null;

				for (CoolTransfer.TransferHandler<AwaitedFileReceiver> handler : mReceive.getProcessList())
				{
					if (handler.getExtra().groupId == groupId)
					{
						runningReceiver = handler.getExtra();
						break;
					}
				}

				if (runningReceiver == null)
					doJob(groupId);
				else
					Toast.makeText(this, getString(R.string.ongoing_list_warning, runningReceiver.fileName), Toast.LENGTH_SHORT).show();
			}
		}

		return START_STICKY;
	}

	public boolean doJob(int groupId)
	{
		SQLQuery.Select selectQuery = new SQLQuery.Select(MainDatabase.TABLE_TRANSFER)
				.setWhere(MainDatabase.FIELD_TRANSFER_TYPE + "=? AND " + MainDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + MainDatabase.FIELD_TRANSFER_FLAG + " != ?",
						String.valueOf(MainDatabase.TYPE_TRANSFER_TYPE_INCOMING),
						String.valueOf(groupId),
						Transaction.Flag.INTERRUPTED.toString());

		CursorItem receiverInstance = getTransactionInstance().getFirstFromTable(selectQuery);

		if (receiverInstance == null)
			return false;

		AwaitedFileReceiver receiver = new AwaitedFileReceiver(receiverInstance);
		File file = new File(FileUtils.getSaveLocationForFile(getApplicationContext(), receiver.fileName));

		if (Transaction.Flag.PENDING.equals(receiver.flag))
		{
			if (file.isFile())
			{
				file = FileUtils.getUniqueFile(file);
				receiver.fileName = file.getName();
			}

			try
			{
				file.createNewFile();
			} catch (IOException e)
			{
				e.printStackTrace();
				return false;
			}
		}

		mReceive.receive(0, file, receiver.fileSize, AppConfig.DEFAULT_BUFFER_SIZE, 10000, receiver);

		return true;
	}

	public class Receive extends CoolTransfer.Receive<AwaitedFileReceiver>
	{
		public int multiCounter = 0;

		@Override
		public Flag onError(TransferHandler<AwaitedFileReceiver> handler, Exception error)
		{
			handler.getExtra().flag = Transaction.Flag.INTERRUPTED;

			getTransactionInstance().updateTransaction(handler.getExtra());
			getNotificationUtils().notifyReceiveError(handler.getExtra());

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onNotify(TransferHandler<AwaitedFileReceiver> handler, int percent)
		{
			handler.getExtra().notification.updateProgress(100, percent, false);
		}

		@Override
		public void onTransferCompleted(TransferHandler<AwaitedFileReceiver> handler)
		{
			multiCounter++;

			getTransactionInstance().removeTransaction(handler.getExtra());

			if (multiCounter <= 1)
				getNotificationUtils().notifyFileReceived(handler.getExtra(), handler.getFile());
			else
				getNotificationUtils().notifyFileReceived(handler.getExtra(), multiCounter);
		}

		@Override
		public void onInterrupted(TransferHandler<AwaitedFileReceiver> handler)
		{
			handler.getExtra().notification.cancel();
			handler.getExtra().flag = Transaction.Flag.INTERRUPTED;

			getTransactionInstance().updateTransaction(handler.getExtra());
		}

		@Override
		public Flag onSocketReady(TransferHandler<AwaitedFileReceiver> handler)
		{
			return Flag.CONTINUE;
		}

		@Override
		public Flag onSocketReady(final ServerService.Receive.Handler handler, final ServerSocket serverSocket)
		{
			try
			{
				JSONObject jsonObject = new JSONObject();

				jsonObject.put(Keyword.REQUEST, Keyword.REQUEST_SERVER_READY);
				jsonObject.put(Keyword.REQUEST_ID, handler.getExtra().requestId);
				jsonObject.put(Keyword.GROUP_ID, handler.getExtra().groupId);
				jsonObject.put(Keyword.SOCKET_PORT, serverSocket.getLocalPort());

				if (handler.getFile().length() > 0)
					jsonObject.put(Keyword.FILE_SIZE, handler.getFile().length());

				JSONObject response = new JSONObject(CoolCommunication.Messenger.sendOnCurrentThread(handler.getExtra().ip, AppConfig.COMMUNATION_SERVER_PORT, jsonObject.toString(), null));

				if (response.getBoolean(Keyword.RESULT))
					return Flag.CONTINUE;

				if (response.has(Keyword.FLAG) && Keyword.FLAG_GROUP_EXISTS.equals(response.getString(Keyword.FLAG)))
					return Flag.CANCEL_CURRENT;
			} catch (JSONException e)
			{
				// It shouldn't have happened.
				e.printStackTrace();
			}

			return Flag.CANCEL_ALL;
		}

		@Override
		public Flag onStart(TransferHandler<AwaitedFileReceiver> handler)
		{
			Log.d(TAG, "onStart(): " + handler.getFile().getName());

			handler.getExtra().notification = getNotificationUtils().notifyFileTransaction(handler.getExtra());
			handler.getExtra().flag = Transaction.Flag.RUNNING;

			getTransactionInstance().updateTransaction(handler.getExtra());

			return Flag.CONTINUE;
		}

		@Override
		public void onPrepareNext(TransferHandler<AwaitedFileReceiver> handler)
		{
			doJob(handler.getExtra().groupId);
		}

		@Override
		public void onProcessListChanged(ArrayList<TransferHandler<AwaitedFileReceiver>> processList, TransferHandler<AwaitedFileReceiver> handler, boolean isAdded)
		{
			super.onProcessListChanged(processList, handler, isAdded);

			Intent updateReceivedList = new Intent(FileChangesReceiver.ACTION_FILE_LIST_CHANGED);

			if (processList.size() > 0)
			{
				getWifiLock().acquire();
				updateReceivedList.putExtra(FileChangesReceiver.NOT_COMPLETE_JOB, true);
			}
			else
				getWifiLock().release();

			sendBroadcast(updateReceivedList);
		}
	}
}
