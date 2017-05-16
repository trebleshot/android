package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.Intent;
import android.net.wifi.WifiManager;

import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedTransaction;
import com.genonbeta.TrebleShot.helper.NotificationUtils;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 4/29/17 2:59 PM
 */

abstract public class AbstractTransactionService<E extends AwaitedTransaction> extends Service
{
	public static final String TAG = AbstractTransactionService.class.getSimpleName();

	public final static String ACTION_CANCEL_JOB = "com.genonbeta.TrebleShot.transaction.action.CANCEL_JOB";
	public final static String ACTION_CANCEL_KILL = "com.genonbeta.TrebleShot.transaction.action.CANCEL_KILL";

	private NotificationUtils mNotification;
	private WifiManager.WifiLock mWifiLock;
	private Transaction mTransaction;

	abstract public ArrayList<CoolTransfer.TransferHandler<E>> onProcessList();

	@Override
	public void onCreate()
	{
		super.onCreate();

		mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE)).createWifiLock(TAG);
		mNotification = new NotificationUtils(this);
		mTransaction = new Transaction(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent != null)
			if (ACTION_CANCEL_JOB.equals(intent.getAction()) && intent.hasExtra(CommunicationService.EXTRA_REQUEST_ID))
			{
				int acceptId = intent.getIntExtra(CommunicationService.EXTRA_ACCEPT_ID, -1);

				for (CoolTransfer.TransferHandler<E> handler : onProcessList())
				{
					if (handler.getExtra().acceptId == acceptId)
					{
						handler.getExtra().notification = getNotificationUtils().notifyStuckThread(handler.getExtra());
						handler.interrupt();

						break;
					}
				}

				return START_STICKY;
			}
			else if (ACTION_CANCEL_KILL.equals(intent.getAction()))
			{
				int acceptId = intent.getIntExtra(CommunicationService.EXTRA_ACCEPT_ID, -1);

				for (CoolTransfer.TransferHandler<E> handler : onProcessList())
					if (handler.getExtra().acceptId == acceptId && handler.getSocket() != null)
					{
						try
						{
							handler.getSocket().close();
						} catch (IOException e)
						{
							e.printStackTrace();
						}

						break;
					}

				return START_STICKY;
			}

		return START_NOT_STICKY;
	}

	public NotificationUtils getNotificationUtils()
	{
		return mNotification;
	}

	public Transaction getTransactionInstance()
	{
		return mTransaction;
	}

	public WifiManager.WifiLock getWifiLock()
	{
		return mWifiLock;
	}
}
