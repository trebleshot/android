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

	abstract public ArrayList<CoolTransfer.TransferHandler<E>> getProcessList();

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
			if (ACTION_CANCEL_JOB.equals(intent.getAction())
					|| ACTION_CANCEL_KILL.equals(intent.getAction()))
			{
				int groupId = intent.getIntExtra(CommunicationService.EXTRA_GROUP_ID, -1);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				CoolTransfer.TransferHandler<E> handler = findProcessById(groupId);

				if (handler != null)
				{
					if (ACTION_CANCEL_KILL.equals(intent.getAction()))
					{
						try
						{
							if (handler instanceof CoolTransfer.Receive.Handler)
							{
								CoolTransfer.Receive.Handler receiveHandler = ((CoolTransfer.Receive.Handler) handler);

								if (receiveHandler.getServerSocket() != null)
									receiveHandler.getServerSocket().close();
							}

							if (handler.getSocket() != null)
								handler.getSocket().close();
						} catch (IOException e)
						{
							e.printStackTrace();
						}
					}
					else
					{
						handler.getExtra().notification = getNotificationUtils().notifyStuckThread(handler.getExtra());
						handler.interrupt();
					}
				}
				else
					mNotification.cancel(notificationId);
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

	public CoolTransfer.TransferHandler<E> findProcessById(int groupId)
	{
		for (CoolTransfer.TransferHandler<E> handler : getProcessList())
			if (handler.getExtra().groupId == groupId)
				return handler;

		return null;
	}

	public E findExtraById(int groupId)
	{
		CoolTransfer.TransferHandler<E> handler = findProcessById(groupId);
		return handler == null ? null : handler.getExtra();
	}

	public boolean isProcessRunning(int groupId)
	{
		return findProcessById(groupId) != null;
	}
}
