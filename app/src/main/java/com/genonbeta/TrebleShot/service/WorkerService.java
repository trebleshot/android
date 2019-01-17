package com.genonbeta.TrebleShot.service;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;

import androidx.annotation.Nullable;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Service;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.InterruptAwareJob;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.android.framework.util.Interrupter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * created by: Veli
 * date: 26.01.2018 14:21
 */

public class WorkerService extends Service
{
    public static final String ACTION_KILL_SIGNAL = "com.genonbeta.intent.action.KILL_SIGNAL";
    public static final String ACTION_KILL_ALL_SIGNAL = "com.genonbeta.intent.action.KILL_ALL_SIGNAL";
    public static final String EXTRA_TASK_ID = "extraTaskId";

    public static final int ID_NOTIFICATION_FOREGROUND = 1103;

    private final List<RunningTask> mTaskList = new ArrayList<>();
    private ExecutorService mExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private LocalBinder mBinder = new LocalBinder();
    private NotificationUtils mNotificationUtils;
    private DynamicNotification mNotification;

    public static boolean run(final Context context, final RunningTask runningTask)
    {
        ServiceConnection serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                AppUtils.startForegroundService(context, new Intent(context, WorkerService.class));

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

    @Override
    public void onCreate()
    {
        super.onCreate();
        mNotificationUtils = new NotificationUtils(this, getDatabase(), getDefaultPreferences());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (intent != null)
            if (ACTION_KILL_SIGNAL.equals(intent.getAction()) && intent.hasExtra(EXTRA_TASK_ID)) {
                int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);

                RunningTask runningTask = findTaskById(taskId);

                if (runningTask == null
                        || runningTask.getInterrupter().interrupted())
                    getNotificationUtils().cancel(taskId);

                if (runningTask != null) {
                    runningTask.getInterrupter().interrupt();
                    runningTask.onInterrupted(taskId);
                }
            } else if (ACTION_KILL_ALL_SIGNAL.equals(intent.getAction())) {
                synchronized (getTaskList()) {
                    for (RunningTask runningTask : getTaskList()) {
                        runningTask.getInterrupter().interrupt();
                        runningTask.onInterrupted(runningTask.getTaskId());
                    }
                }
            }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();

        synchronized (getTaskList()) {
            for (RunningTask runningTask : getTaskList())
                runningTask.getInterrupter().interrupt(false);
        }
    }

    public synchronized RunningTask findTaskById(int taskId)
    {
        synchronized (getTaskList()) {
            for (RunningTask runningTask : getTaskList())
                if (runningTask.getTaskId() == taskId)
                    return runningTask;
        }

        return null;
    }

    public List<RunningTask> findTasksFor(String tag)
    {
        List<RunningTask> taskList = new ArrayList<>();

        synchronized (getTaskList()) {
            for (RunningTask runningTask : getTaskList())
                if (tag.equals(runningTask.getClientTag()))
                    taskList.add(runningTask);
        }

        return taskList;
    }

    public List<RunningTask> getTaskList()
    {
        return mTaskList;
    }

    public void publishForegroundNotification()
    {
        if (mNotification == null) {
            PendingIntent cancelIntent = PendingIntent.getService(
                    this,
                    0,
                    new Intent(this, WorkerService.class).setAction(ACTION_KILL_ALL_SIGNAL),
                    0);

            mNotification = mNotificationUtils.buildDynamicNotification(ID_NOTIFICATION_FOREGROUND, NotificationUtils.NOTIFICATION_CHANNEL_LOW);
            mNotification.setSmallIcon(R.drawable.ic_autorenew_white_24dp_static)
                    .setContentTitle(getString(R.string.text_taskOngoing))
                    .addAction(R.drawable.ic_close_white_24dp_static, getString(R.string.butn_cancel), cancelIntent);
        }

        synchronized (getTaskList()) {
            if (getTaskList().size() <= 0)
                stopForeground(true);
            else {
                StringBuilder stringBuilder = new StringBuilder();

                for (RunningTask task : getTaskList()) {
                    String statusText = task.getStatusText();

                    if (statusText == null || statusText.length() <= 0)
                        statusText = task.getClientTag();

                    statusText = statusText.replaceAll("\n", " ");

                    stringBuilder.append(statusText)
                            .append("\n");
                }

                mNotification.setContentText(stringBuilder.toString());
                startForeground(ID_NOTIFICATION_FOREGROUND, mNotification.build());
            }
        }
    }

    protected synchronized void registerWork(RunningTask runningTask)
    {
        synchronized (getTaskList()) {
            getTaskList().add(runningTask);
        }

        publishForegroundNotification();
    }

    public void run(final RunningTask runningTask)
    {
        mExecutor.submit(new Runnable()
        {
            @Override
            public void run()
            {
                runningTask.setService(WorkerService.this);

                registerWork(runningTask);
                runningTask.run();
                unregisterWork(runningTask);

                runningTask.setService(null);
            }
        });
    }

    protected synchronized void unregisterWork(RunningTask runningTask)
    {
        synchronized (getTaskList()) {
            getTaskList().remove(runningTask);
        }

        publishForegroundNotification();
    }

    public abstract static class RunningTask extends InterruptAwareJob
    {
        private WorkerService mService;
        private Interrupter mInterrupter;
        private String mTag;
        private int mJobId;
        private int mTaskId = AppUtils.getUniqueNumber();
        private String mStatusText;
        private long mLastNotified = 0;

        public RunningTask(String clientTag, int jobId)
        {
            mTag = clientTag;
            mJobId = jobId;
        }

        abstract protected void onRun();
        //abstract protected String jobTitle(Context context);

        public void onInterrupted(int taskId)
        {

        }

        public String getClientTag()
        {
            return mTag;
        }

        public Interrupter getInterrupter()
        {
            if (mInterrupter == null)
                mInterrupter = new Interrupter();

            return mInterrupter;
        }

        public RunningTask setInterrupter(Interrupter interrupter)
        {
            mInterrupter = interrupter;
            return this;
        }

        public int getJobId()
        {
            return mJobId;
        }

        @Nullable
        public WorkerService getService()
        {
            return mService;
        }

        public void setService(@Nullable WorkerService service)
        {
            mService = service;
        }

        public String getStatusText()
        {
            return mStatusText;
        }

        public int getTaskId()
        {
            return mTaskId;
        }

        public void publishStatusText(String text)
        {
            mStatusText = text;

            if (System.currentTimeMillis() - mLastNotified > 2000) {
                mService.publishForegroundNotification();
                mLastNotified = System.currentTimeMillis();
            }
        }

        public void run()
        {
            run(getInterrupter());
        }
    }

    public class LocalBinder extends Binder
    {
        public WorkerService getService()
        {
            return WorkerService.this;
        }
    }
}
