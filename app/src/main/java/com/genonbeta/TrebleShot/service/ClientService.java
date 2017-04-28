package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationUtils;
import com.genonbeta.android.database.CursorItem;

import java.io.File;
import java.net.Socket;

public class ClientService extends Service
{
	public static final String TAG = "ClientService";

	public final static String ACTION_SEND = "com.genonbeta.TrebleShot.client.ACTION_SEND";
	public final static String ACTION_CANCEL_SENDING = "com.genonbeta.TrebleShot.client.CANCEL_SENDING";

	private NotificationUtils mNotification;
	private WifiManager.WifiLock mWifiLock;
	private Transaction mTransaction;
	private Send mSend = new Send();

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE)).createWifiLock(TAG);
		mNotification = new NotificationUtils(this);
		mTransaction = new Transaction(this);

		mSend.setNotifyDelay(2000);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
		{
			if (ACTION_SEND.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_REQUEST_ID))
			{
				try
				{
					int requestId = intent.getIntExtra(CommunicationService.EXTRA_REQUEST_ID, -1);
					CursorItem transaction = mTransaction.getTransaction(requestId);

					if (transaction != null)
					{
						AwaitedFileSender awaitedSender = new AwaitedFileSender(transaction);
						mSend.send(awaitedSender.ip, awaitedSender.port, awaitedSender.file, AppConfig.DEFAULT_BUFFER_SIZE, awaitedSender);
					}
				} catch (Exception e)
				{
					mNotification.showToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
				}
			}
			else if (ACTION_CANCEL_SENDING.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_REQUEST_ID))
			{
				int requestId = intent.getIntExtra(CommunicationService.EXTRA_REQUEST_ID, -1);

				Log.d(TAG, "Sender stop request is received for id = " + requestId);

				CursorItem transactionItem = mTransaction.getTransaction(requestId);

				if (transactionItem != null)
				{
					AwaitedFileSender sender = new AwaitedFileSender(transactionItem);
					mTransaction.removeTransactionGroup(sender.acceptId);
					mSend.cancelGroup(sender.acceptId);
				}
				else
					mNotification.cancel(intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1));
			}
		}

		return START_STICKY;
	}

	private class Send extends CoolTransfer.Send<AwaitedFileSender>
	{
		private int mCancelledGroupId = -1;

		public void cancelGroup(int groupId)
		{
			mCancelledGroupId = groupId;
		}

		public boolean isCancelled(AwaitedFileSender sender)
		{
			return sender.acceptId == mCancelledGroupId;
		}

		@Override
		public void onError(SendHandler handler, Exception error)
		{
			handler.getExtra().flag = Transaction.Flag.ERROR;

			mTransaction.updateTransaction(handler.getExtra());
			mNotification.showToast(getString(R.string.file_sending_error_msg, "<?>"));
		}

		@Override
		public void onNotify(SendHandler handler, int percent)
		{
			handler.getExtra().notification.updateProgress(100, percent, false);
		}

		@Override
		public void onTransferCompleted(SendHandler handler)
		{
			mNotification.showToast(getString(R.string.file_sent_msg, handler.getExtra().fileName));
			mTransaction.removeTransaction(handler.getExtra());
		}

		@Override
		public void onInterrupted(SendHandler handler)
		{
			mNotification.showToast(getString(R.string.file_send_cancelled_msg, handler.getExtra().fileName));
		}

		@Override
		public void onSocketReady(SendHandler handler)
		{

		}

		@Override
		public boolean onStart(SendHandler handler)
		{
			Looper.prepare();
			mWifiLock.acquire();

			mCancelledGroupId = -1;

			NetworkDevice device = ApplicationHelper.getDeviceList().get(handler.getExtra().ip);
			handler.getExtra().notification = mNotification.notifyFileSending(handler.getExtra(), device, 0);
			handler.getExtra().flag = Transaction.Flag.RUNNING;

			mTransaction.updateTransaction(handler.getExtra());

			return true;
		}

		@Override
		public void onStop(SendHandler handler)
		{
			super.onStop(handler);

			handler.getExtra().notification.cancel();

			mWifiLock.release();
		}

		@Override
		public boolean onCheckStatus(SendHandler handler)
		{
			return isCancelled(handler.getExtra());
		}
	}
}
