package com.genonbeta.TrebleShot.service;

import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;

import com.genonbeta.CoolSocket.CoolTransferSend;
import com.genonbeta.CoolSocket.CoolTransferSendHandler;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.android.database.CursorItem;

import java.net.ServerSocket;
import java.util.ArrayList;

public class ClientService extends AbstractTransactionService<AwaitedFileSender, CoolTransferSendHandler<AwaitedFileSender>>
{
	public static final String TAG = "ClientService";

	public final static String ACTION_SEND = "com.genonbeta.TrebleShot.client.ACTION_SEND";

	private Send mSend = new Send();

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public ArrayList<CoolTransferSendHandler<AwaitedFileSender>> onProcessList()
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
			if (ACTION_SEND.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_REQUEST_ID))
			{
				try
				{
					int requestId = intent.getIntExtra(CommunicationService.EXTRA_REQUEST_ID, -1);
					CursorItem transaction = getTransactionInstance().getTransaction(requestId);

					if (transaction != null)
					{
						AwaitedFileSender awaitedSender = new AwaitedFileSender(transaction);
						mSend.send(awaitedSender.ip, awaitedSender.port, awaitedSender.file, AppConfig.DEFAULT_BUFFER_SIZE, awaitedSender);
					}
				} catch (Exception e)
				{
					getNotificationUtils().showToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
				}
			}


		return START_STICKY;
	}

	public class Send extends CoolTransferSend<AwaitedFileSender>
	{
		@Override
		public void onError(CoolTransferSendHandler<AwaitedFileSender> handler, Exception error)
		{
			handler.getExtra().flag = Transaction.Flag.ERROR;

			getTransactionInstance().updateTransaction(handler.getExtra());
			getNotificationUtils().showToast(getString(R.string.file_sending_error_msg, "<?>"));
		}

		@Override
		public void onNotify(CoolTransferSendHandler<AwaitedFileSender> handler, int percent)
		{
			handler.getExtra().notification.updateProgress(100, percent, false);
		}

		@Override
		public void onTransferCompleted(CoolTransferSendHandler<AwaitedFileSender> handler)
		{
			getNotificationUtils().showToast(getString(R.string.file_sent_msg, handler.getExtra().fileName));
			getTransactionInstance().removeTransaction(handler.getExtra());
		}

		@Override
		public void onInterrupted(CoolTransferSendHandler<AwaitedFileSender> handler)
		{
			getNotificationUtils().showToast(getString(R.string.file_send_cancelled_msg, handler.getExtra().fileName));
		}

		@Override
		public void onSocketReady(CoolTransferSendHandler<AwaitedFileSender> handler)
		{

		}

		@Override
		public void onSocketReady(CoolTransferSendHandler<AwaitedFileSender> handler, ServerSocket serverSocket)
		{

		}

		@Override
		public boolean onStart(CoolTransferSendHandler<AwaitedFileSender> handler)
		{
			Looper.prepare();
			getWifiLock().acquire();

			NetworkDevice device = ApplicationHelper.getDeviceList().get(handler.getExtra().ip);
			handler.getExtra().notification = getNotificationUtils().notifyFileSending(handler.getExtra(), device);
			handler.getExtra().flag = Transaction.Flag.RUNNING;

			getTransactionInstance().updateTransaction(handler.getExtra());

			return true;
		}

		@Override
		public void onStop(CoolTransferSendHandler<AwaitedFileSender> handler)
		{
			super.onStop(handler);

			handler.getExtra().notification.cancel();
			getWifiLock().release();
		}
	}
}
