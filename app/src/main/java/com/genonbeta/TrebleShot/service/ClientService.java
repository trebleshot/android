package com.genonbeta.TrebleShot.service;

import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;

import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.app.TransactionService;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.TransactionObject;
import com.genonbeta.android.database.SQLQuery;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ClientService extends TransactionService<TransactionObject>
{
	public static final String TAG = "ClientService";

	public static final String ACTION_SEND = "com.genonbeta.TrebleShot.client.ACTION_SEND";

	private Send mSend = new Send();

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public ArrayList<CoolTransfer.TransferHandler<TransactionObject>> getProcessList()
	{
		return mSend.getProcessList();
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		mSend.setNotifyDelay(2000);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			if (ACTION_SEND.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_REQUEST_ID)) {
				try {
					int requestId = intent.getIntExtra(CommunicationService.EXTRA_REQUEST_ID, -1);

					TransactionObject transactionObject = new TransactionObject(requestId);
					getDatabase().reconstruct(transactionObject);

					TransactionObject.Group group = new TransactionObject.Group(transactionObject.groupId);
					getDatabase().reconstruct(group);

					NetworkDevice.Connection connection = new NetworkDevice.Connection(group.deviceId, group.connectionAdapter);
					getDatabase().reconstruct(connection);

					StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), Uri.parse(transactionObject.file), true);

					mSend.send(connection.ipAddress, transactionObject.accessPort, streamInfo.inputStream, streamInfo.size, AppConfig.DEFAULT_BUFFER_SIZE, transactionObject, false);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		return START_STICKY;
	}

	public class Send extends CoolTransfer.Send<TransactionObject>
	{
		@Override
		public Flag onError(TransferHandler<TransactionObject> handler, Exception error)
		{
			error.printStackTrace();

			handler.getExtra().flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().publish(handler.getExtra());

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
			getDatabase().remove(handler.getExtra());

			if (getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(handler.getExtra().groupId))) == null)
				getDatabase().remove(new TransactionObject.Group(handler.getExtra().groupId));
		}

		@Override
		public void onInterrupted(TransferHandler<TransactionObject> handler)
		{
		}

		@Override
		public Flag onSocketReady(TransferHandler<TransactionObject> handler)
		{
			return Flag.CONTINUE;
		}

		@Override
		public Flag onStart(TransferHandler<TransactionObject> handler)
		{
			try {
				handler.getExtra().notification = getNotificationUtils().notifyFileTransaction(handler.getExtra());
				handler.getExtra().flag = TransactionObject.Flag.RUNNING;

				getWifiLock().acquire();
				getDatabase().publish(handler.getExtra());

				return Flag.CONTINUE;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onStop(TransferHandler<TransactionObject> handler)
		{
			super.onStop(handler);

			handler.getExtra().notification.cancel();
			getWifiLock().release();
		}

		@Override
		public void onOrientatingStreams(Handler handler, InputStream inputStream, OutputStream outputStream)
		{
			super.onOrientatingStreams(handler, inputStream, outputStream);

			if (handler.getExtra().skippedBytes > 0)
				try {
					handler.skipBytes(handler.getExtra().skippedBytes);
				} catch (IOException e) {
					handler.interrupt();
					e.printStackTrace();
				}
		}

		@Override
		public void onProcessListChanged(ArrayList<TransferHandler<TransactionObject>> processList, TransferHandler<TransactionObject> handler, boolean isAdded)
		{
			super.onProcessListChanged(processList, handler, isAdded);

			if (isAdded)
				getWifiLock().acquire();
			else if (processList.size() < 1)
				getWifiLock().release();
		}
	}
}
