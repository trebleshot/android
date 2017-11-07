package com.genonbeta.TrebleShot.service;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.TransactionObject;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONObject;

import java.io.File;
import java.net.ServerSocket;
import java.util.ArrayList;

public class ServerService extends AbstractTransactionService<TransactionObject>
{
	public static final String TAG = "ServerService";

	public final static String ACTION_START_RECEIVING = "com.genonbeta.TrebleShot.action.START_RECEIVING";

	private Receive mReceive = new Receive();
	private MediaScannerConnection mMediaScanner;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public ArrayList<CoolTransfer.TransferHandler<TransactionObject>> getProcessList()
	{
		return mReceive.getProcessList();
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		mMediaScanner = new MediaScannerConnection(this, null);

		mMediaScanner.connect();
		mReceive.setNotifyDelay(2000);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		Log.d(TAG, "onStart()");

		if (intent != null) {
			if (ACTION_START_RECEIVING.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_GROUP_ID)) {
				int groupId = intent.getIntExtra(CommunicationService.EXTRA_GROUP_ID, -1);
				TransactionObject runningReceiver = findExtraById(groupId);

				if (runningReceiver == null)
					doJob(groupId);
				else
					Toast.makeText(this, getString(R.string.mesg_groupOngoingNotice, runningReceiver.file), Toast.LENGTH_SHORT).show();
			}
		}

		return START_STICKY;
	}

	public boolean doJob(int groupId)
	{
		SQLQuery.Select selectQuery = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
				.setWhere(AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND " + AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_FLAG + " !=?",
						TransactionObject.Type.INCOMING.toString(),
						String.valueOf(groupId),
						TransactionObject.Flag.INTERRUPTED.toString());

		CursorItem receiverInstance = getDatabase().getFirstFromTable(selectQuery);

		if (receiverInstance == null)
			return false;

		TransactionObject transactionObject = new TransactionObject(receiverInstance);
		TransactionObject.Group group = new TransactionObject.Group(transactionObject.groupId);

		try {
			getDatabase().reconstruct(group);

			File file = new File(group.savePath != null
					? FileUtils.getSaveLocationForFile(getApplicationContext(), transactionObject.file)
					: group.savePath + File.separator + transactionObject.file);

			if (!file.createNewFile())
				return false;

			mReceive.receive(0, file, transactionObject.fileSize, AppConfig.DEFAULT_BUFFER_SIZE, 10000, transactionObject, false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return true;
	}

	public class Receive extends CoolTransfer.Receive<TransactionObject>
	{
		public int multiCounter = 0;

		@Override
		public Flag onError(TransferHandler<TransactionObject> handler, Exception error)
		{
			error.printStackTrace();

			handler.getExtra().flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().publish(handler.getExtra());
			getNotificationUtils().notifyReceiveError(handler.getExtra());

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onNotify(TransferHandler<TransactionObject> handler, int percent)
		{
			handler.getExtra().notification.updateProgress(100, percent, false);
		}

		@Override
		public void onTransferCompleted(TransferHandler<TransactionObject> handler)
		{
			multiCounter++;

			getDatabase().remove(handler.getExtra());

			File finalFileLocation = FileUtils.getUniqueFile(new File(handler.getFile().getParent() + File.separator + handler.getExtra().friendlyName), true);

			handler.getFile().renameTo(finalFileLocation);

			if (multiCounter <= 1)
				try {
					getNotificationUtils().notifyFileReceived(handler.getExtra(), finalFileLocation);
				} catch (Exception e) {
					e.printStackTrace();
				}
			else
				getNotificationUtils().notifyFileReceived(handler.getExtra(), finalFileLocation.getParent(), multiCounter);
		}

		@Override
		public void onInterrupted(TransferHandler<TransactionObject> handler)
		{
			handler.getExtra().notification.cancel();
			handler.getExtra().flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().publish(handler.getExtra());
		}

		@Override
		public Flag onSocketReady(TransferHandler<TransactionObject> handler)
		{
			return Flag.CONTINUE;
		}

		@Override
		public Flag onSocketReady(final ServerService.Receive.Handler handler, final ServerSocket serverSocket)
		{
			try {
				NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());
				JSONObject jsonObject = new JSONObject();

				jsonObject.put(Keyword.SERIAL, localDevice.deviceId);
				jsonObject.put(Keyword.REQUEST, Keyword.REQUEST_SERVER_READY);
				jsonObject.put(Keyword.REQUEST_ID, handler.getExtra().requestId);
				jsonObject.put(Keyword.GROUP_ID, handler.getExtra().groupId);
				jsonObject.put(Keyword.SOCKET_PORT, serverSocket.getLocalPort());

				TransactionObject.Group group = new TransactionObject.Group(handler.getExtra().groupId);
				getDatabase().reconstruct(group);

				NetworkDevice.Connection connection = new NetworkDevice.Connection(group.deviceId, group.connectionAdapter);
				getDatabase().reconstruct(connection);

				if (handler.getFile().length() > 0)
					jsonObject.put(Keyword.SKIPPED_BYTES, handler.getFile().length());

				JSONObject response = new JSONObject(CoolCommunication.Messenger.sendOnCurrentThread(connection.ipAddress, AppConfig.COMMUNATION_SERVER_PORT, jsonObject.toString(), null));

				if (response.getBoolean(Keyword.RESULT))
					return Flag.CONTINUE;

				if (response.has(Keyword.FLAG) && Keyword.FLAG_GROUP_EXISTS.equals(response.getString(Keyword.FLAG)))
					return Flag.CANCEL_CURRENT;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return Flag.CANCEL_ALL;
		}

		@Override
		public Flag onStart(TransferHandler<TransactionObject> handler)
		{
			try {
				handler.getExtra().notification = getNotificationUtils().notifyFileTransaction(handler.getExtra());
				handler.getExtra().flag = TransactionObject.Flag.RUNNING;

				getDatabase().publish(handler.getExtra());

				return Flag.CONTINUE;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onPrepareNext(TransferHandler<TransactionObject> handler)
		{
			doJob(handler.getExtra().groupId);
		}

		@Override
		public void onProcessListChanged(ArrayList<TransferHandler<TransactionObject>> processList, TransferHandler<TransactionObject> handler, boolean isAdded)
		{
			super.onProcessListChanged(processList, handler, isAdded);

			if (isAdded)
				getWifiLock().acquire();
			else {
				if (processList.size() < 1)
					getWifiLock().release();

				if (mMediaScanner.isConnected())
					mMediaScanner.scanFile(handler.getFile().getAbsolutePath(), handler.getExtra().fileMimeType);

				sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
						.putExtra(FileListFragment.EXTRA_PATH, handler.getFile().getParent()));
			}
		}
	}
}
