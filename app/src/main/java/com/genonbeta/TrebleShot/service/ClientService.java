package com.genonbeta.TrebleShot.service;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;

import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.android.database.CursorItem;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class ClientService extends AbstractTransactionService<AwaitedFileSender>
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
	public ArrayList<CoolTransfer.TransferHandler<AwaitedFileSender>> getProcessList()
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
					CursorItem transaction = getTransactionInstance().getTransaction(requestId);

					if (transaction != null) {
						AwaitedFileSender awaitedSender = new AwaitedFileSender(transaction);
						StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), awaitedSender.fileUri);

						mSend.send(awaitedSender.ip, awaitedSender.port, streamInfo.inputStream, streamInfo.size, AppConfig.DEFAULT_BUFFER_SIZE, awaitedSender, false);
					}
				} catch (Exception e) {

				}
			}

		return START_STICKY;
	}

	public class Send extends CoolTransfer.Send<AwaitedFileSender>
	{
		@Override
		public Flag onError(TransferHandler<AwaitedFileSender> handler, Exception error)
		{
			handler.getExtra().flag = Transaction.Flag.INTERRUPTED;

			getTransactionInstance()
					.edit()
					.updateTransaction(handler.getExtra())
					.done();

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onNotify(TransferHandler<AwaitedFileSender> handler, int percent)
		{
			handler.getExtra().notification.updateProgress(100, percent, false);
		}

		@Override
		public void onTransferCompleted(TransferHandler<AwaitedFileSender> handler)
		{
			getTransactionInstance()
					.edit()
					.removeTransaction(handler.getExtra())
					.done();
		}

		@Override
		public void onInterrupted(TransferHandler<AwaitedFileSender> handler)
		{

		}

		@Override
		public Flag onSocketReady(TransferHandler<AwaitedFileSender> handler)
		{
			return Flag.CONTINUE;
		}

		@Override
		public Flag onStart(TransferHandler<AwaitedFileSender> handler)
		{
			getWifiLock().acquire();

			handler.getExtra().notification = getNotificationUtils().notifyFileTransaction(handler.getExtra());
			handler.getExtra().flag = Transaction.Flag.RUNNING;

			getTransactionInstance()
					.edit()
					.updateTransaction(handler.getExtra())
					.done();

			return Flag.CONTINUE;
		}

		@Override
		public void onStop(TransferHandler<AwaitedFileSender> handler)
		{
			super.onStop(handler);

			handler.getExtra().notification.cancel();
			getWifiLock().release();
		}

		@Override
		public void onOrientatingStreams(Handler handler, InputStream inputStream, OutputStream outputStream)
		{
			super.onOrientatingStreams(handler, inputStream, outputStream);

			if (handler.getExtra().fileSize > 0)
				try {
					inputStream.skip(handler.getExtra().fileSize);
				} catch (IOException e) {
					handler.interrupt();
					e.printStackTrace();
				}
		}

		@Override
		public void onProcessListChanged(ArrayList<TransferHandler<AwaitedFileSender>> processList, TransferHandler<AwaitedFileSender> handler, boolean isAdded)
		{
			super.onProcessListChanged(processList, handler, isAdded);

			if (isAdded)
				getWifiLock().acquire();
			else if (processList.size() < 1)
				getWifiLock().release();
		}
	}
}
