package com.genonbeta.TrebleShot.service;

import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.NotificationUtils;

import java.util.ArrayList;

/**
 * created by: Veli
 * date: 26.01.2018 14:21
 */

public class WorkerService extends Service
{
	public static final String ACTION_KILL_SIGNAL = "com.genonbeta.intent.action.KILL_SIGNAL";

	public static final String EXTRA_TASK_ID = "extraTaskId";

	private final ArrayList<RunningTask> mTaskList = new ArrayList<>();
	private LocalBinder mBinder = new LocalBinder();
	private NotificationUtils mNotificationUtils;

	@Nullable
	@Override
	public IBinder onBind(Intent intent)
	{
		return mBinder;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		mNotificationUtils = new NotificationUtils(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		if (intent != null)
			if (ACTION_KILL_SIGNAL.equals(intent.getAction()) && intent.hasExtra(EXTRA_TASK_ID)) {
				int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);

				getNotificationUtils().cancel(taskId);

				RunningTask runningTask = findTaskById(taskId);

				if (runningTask != null)
					runningTask.getInterrupter().interrupt();
			}

		return START_NOT_STICKY;
	}

	public synchronized RunningTask findTaskById(int taskId)
	{
		for (RunningTask runningTask : getTaskList())
			if (runningTask.getTaskId() == taskId)
				return runningTask;

		return null;
	}

	public NotificationUtils getNotificationUtils()
	{
		return mNotificationUtils;
	}

	public ArrayList<RunningTask> getTaskList()
	{
		synchronized (mTaskList) {
			return mTaskList;
		}
	}

	protected synchronized void registerWork(RunningTask runningTask)
	{
		getTaskList().add(runningTask);
	}

	public void run(final RunningTask runningTask)
	{
		new Thread()
		{
			@Override
			public void run()
			{
				super.run();

				NotifiableRunningTask notifiableRunningWork = runningTask instanceof NotifiableRunningTask
						? (NotifiableRunningTask) runningTask
						: null;

				if (notifiableRunningWork != null) {
					DynamicNotification dynamicNotification = getNotificationUtils().buildDynamicNotification(runningTask.getTaskId(), NotificationUtils.NOTIFICATION_CHANNEL_LOW);

					notifiableRunningWork.onUpdateNotification(dynamicNotification, NotifiableRunningTask.UpdateType.Started);

					Intent cancelIntent = new Intent(WorkerService.this, WorkerService.class);

					cancelIntent.setAction(ACTION_KILL_SIGNAL);
					cancelIntent.putExtra(EXTRA_TASK_ID, runningTask.getTaskId());

					dynamicNotification
							.setOngoing(true)
							.setContentTitle(getString(R.string.text_taskOngoing))
							.setProgress(0, 0, true)
							.addAction(R.drawable.ic_clear_white_24dp, getString(R.string.butn_cancel),
									PendingIntent.getService(WorkerService.this, AppUtils.getUniqueNumber(), cancelIntent, 0));

					dynamicNotification.show();
				}

				registerWork(runningTask);
				runningTask.onRun();
				unregisterWork(runningTask);

				if (notifiableRunningWork != null && !runningTask.getInterrupter().interrupted()) {
					DynamicNotification dynamicNotification = getNotificationUtils().buildDynamicNotification(runningTask.getTaskId(), NotificationUtils.NOTIFICATION_CHANNEL_LOW);

					notifiableRunningWork.onUpdateNotification(dynamicNotification, NotifiableRunningTask.UpdateType.Done);
					dynamicNotification.setContentTitle(getString(R.string.text_taskCompleted));
					dynamicNotification.show();
				}
			}
		}.start();
	}

	protected synchronized void unregisterWork(RunningTask runningTask)
	{
		getTaskList().remove(runningTask);
	}

	public static boolean run(final Context context, final RunningTask runningTask)
	{
		ServiceConnection serviceConnection = new ServiceConnection()
		{
			@Override
			public void onServiceConnected(ComponentName name, IBinder service)
			{
				WorkerService workerService = ((WorkerService.LocalBinder) service).getService();

				workerService.run(runningTask);

				context.unbindService(this);
			}

			@Override
			public void onServiceDisconnected(ComponentName name)
			{

			}
		};

		return context.bindService(new Intent(context, WorkerService.class), serviceConnection, Context.BIND_AUTO_CREATE);
	}

	public class LocalBinder extends Binder
	{
		public WorkerService getService()
		{
			return WorkerService.this;
		}
	}

	public abstract static class RunningTask
	{
		private Interrupter mInterrupter;
		private int mTaskId = AppUtils.getUniqueNumber();

		abstract public long getJobId();

		abstract public void onRun();

		public Interrupter getInterrupter()
		{
			if (mInterrupter == null)
				mInterrupter = new Interrupter();

			return mInterrupter;
		}

		public int getTaskId()
		{
			return mTaskId;
		}

		public RunningTask setInterrupter(Interrupter interrupter)
		{
			mInterrupter = interrupter;
			return this;
		}
	}

	public abstract static class NotifiableRunningTask extends RunningTask
	{
		public abstract void onUpdateNotification(DynamicNotification dynamicNotification, UpdateType updateType);

		public enum UpdateType
		{
			Started,
			Ongoing,
			Done
		}
	}
}
