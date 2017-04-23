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
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationPublisher;

import java.io.File;
import java.net.Socket;

public class ClientService extends Service
{
	public static final String TAG = "ClientService";

	public final static String ACTION_SEND = "com.genonbeta.TrebleShot.client.ACTION_SEND";
	public final static String ACTION_CANCEL_SENDING = "com.genonbeta.TrebleShot.client.CANCEL_SENDING";

	private NotificationPublisher mPublisher;
	private WifiManager.WifiLock mWifiLock;
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
		mPublisher = new NotificationPublisher(this);

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

					if (ApplicationHelper.getSenders().containsKey(requestId))
					{
						AwaitedFileSender awaitedSender = ApplicationHelper.getSenders().get(requestId);

						mSend.send(awaitedSender.ip, awaitedSender.port, awaitedSender.file, AppConfig.DEFAULT_BUFFER_SIZE, awaitedSender);

						Log.d(TAG, "Send intent is ok");
					}

					Log.d(TAG, "Send intent is received");
				} catch (Exception e)
				{
					mPublisher.makeToast(getString(R.string.file_sending_error_msg, getString(R.string.communication_problem)));
				}
			}
			else if (ACTION_CANCEL_SENDING.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_REQUEST_ID))
			{
				int requestId = intent.getIntExtra(CommunicationService.EXTRA_REQUEST_ID, -1);

				Log.d(TAG, "Sender stop request is received for id = " + requestId);

				if (ApplicationHelper.getSenders().containsKey(requestId))
				{
					AwaitedFileSender sender = ApplicationHelper.getSenders().get(requestId);
					sender.isCancelled = true;
				}
				else
					mPublisher.cancelNotification(intent.getIntExtra(NotificationPublisher.EXTRA_NOTIFICATION_ID, -1));
			}
		}

		return START_STICKY;
	}

	private class Send extends CoolTransfer.Send<AwaitedFileSender>
	{
		@Override
		public boolean onStart(String serverIp, int port, File file, AwaitedFileSender extra)
		{
			Looper.prepare();
			mWifiLock.acquire();

			NetworkDevice device = ApplicationHelper.getDeviceList().get(extra.ip);
			mPublisher.notifyFileSending(extra, device, 0);

			return true;
		}

		@Override
		public void onStop(String serverIp, int port, File file, AwaitedFileSender extra)
		{
			super.onStop(serverIp, port, file, extra);

			mWifiLock.release();
			mPublisher.cancelNotification(extra.requestId);

			ApplicationHelper.removeSender(extra);
		}

		@Override
		public void onError(String serverIp, int port, File file, Exception error, AwaitedFileSender extra)
		{
			mPublisher.makeToast(getString(R.string.file_sending_error_msg, "<?>"));
		}

		@Override
		public void onNotify(Socket socket, String serverIp, int port, File file, int percent, AwaitedFileSender extra)
		{
			NetworkDevice device = ApplicationHelper.getDeviceList().get(extra.ip);
			mPublisher.notifyFileSending(extra, device, percent);
		}

		@Override
		public void onTransferCompleted(String serverIp, int port, File file, AwaitedFileSender extra)
		{
			mPublisher.makeToast(getString(((!extra.isCancelled) ? R.string.file_sent_msg : R.string.file_send_cancelled_msg), extra.fileName));
		}

		@Override
		public boolean onBreakRequest(String serverIp, int port, File file, AwaitedFileSender extra)
		{
			return extra.isCancelled;
		}

		@Override
		public void onSocketReady(Socket socket, String serverIp, int port, File file, AwaitedFileSender extra)
		{

		}
	}
}
