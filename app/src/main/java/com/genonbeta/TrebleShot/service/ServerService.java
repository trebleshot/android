package com.genonbeta.TrebleShot.service;

import android.content.Intent;
import android.media.MediaScannerConnection;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.TransactionService;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
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
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;

public class ServerService extends TransactionService<TransactionObject>
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
					Toast.makeText(this, getString(R.string.mesg_groupOngoingNotice, runningReceiver.friendlyName), Toast.LENGTH_SHORT).show();
			}
		}

		return START_STICKY;
	}

	public boolean doJob(int groupId)
	{
		TransactionObject.Group group = new TransactionObject.Group(groupId);

		try {
			getDatabase().reconstruct(group);

			CursorItem receiverInstance = getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND " + AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_FLAG + " !=?",
							TransactionObject.Type.INCOMING.toString(),
							String.valueOf(groupId),
							TransactionObject.Flag.INTERRUPTED.toString()));

			if (receiverInstance == null
					&& getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(groupId))) == null) {
				getDatabase().remove(group);
				return false;
			}

			Log.d(TAG, "We exist");

			TransactionObject transactionObject = new TransactionObject(receiverInstance);
			File file = FileUtils.getIncomingTransactionFile(getApplicationContext(), transactionObject, group);

			mReceive.receive(0, file, transactionObject.fileSize, AppConfig.DEFAULT_BUFFER_SIZE, AppConfig.DEFAULT_SOCKET_TIMEOUT, transactionObject, false);
		} catch (Exception e) {
			e.printStackTrace();
			return false;
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
			handler.setFile(finalFileLocation);
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
			Flag flag = CoolSocket.connect(new CoolSocket.Client.ConnectionHandler()
			{
				@Override
				public void onConnect(CoolSocket.Client client)
				{
					client.setReturn(Flag.CANCEL_ALL);

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

						CoolSocket.ActiveConnection activeConnection = client.connect(new InetSocketAddress(connection.ipAddress, AppConfig.COMMUNICATION_SERVER_PORT), CoolSocket.NO_TIMEOUT);

						activeConnection.reply(jsonObject.toString());

						JSONObject response = new JSONObject(activeConnection.receive().response);

						if (response.getBoolean(Keyword.RESULT))
							client.setReturn(Flag.CONTINUE);
						else if (response.has(Keyword.FLAG) && Keyword.FLAG_GROUP_EXISTS.equals(response.getString(Keyword.FLAG)))
							client.setReturn(Flag.CANCEL_CURRENT);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}, Flag.class);

			if (!Flag.CONTINUE.equals(flag)) {
				handler.getExtra().flag = TransactionObject.Flag.INTERRUPTED;
				getDatabase().publish(handler.getExtra());
			}

			return flag;
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
			try {
				TransactionObject.Group group = new TransactionObject.Group(handler.getExtra().groupId);
				getDatabase().reconstruct(group);

				NetworkDevice device = new NetworkDevice(group.deviceId);
				getDatabase().reconstruct(device);

				if (!doJob(handler.getExtra().groupId))
					if (multiCounter <= 1)
						getNotificationUtils().notifyFileReceived(handler.getExtra(), device, handler.getFile());
					else
						getNotificationUtils().notifyFileReceived(handler.getExtra(), handler.getFile().getParent(), multiCounter);
			} catch (Exception e) {
				handler.getExtra().notification.cancel();
			}
		}

		@Override
		public void onProcessListChanged
				(ArrayList<TransferHandler<TransactionObject>> processList, TransferHandler<TransactionObject> handler,
				 boolean isAdded)
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
