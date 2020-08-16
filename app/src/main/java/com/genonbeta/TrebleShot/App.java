/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.multidex.MultiDexApplication;
import androidx.preference.PreferenceManager;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.Identity;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.android.updatewithgithub.GitHubUpdater;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */

public class App extends MultiDexApplication implements Thread.UncaughtExceptionHandler
{
    public static final String TAG = App.class.getSimpleName();

    public static final String ACTION_OREO_HOTSPOT_STARTED = "org.monora.trebleshot.intent.action.HOTSPOT_STARTED";

    public static final String ACTION_TASK_CHANGE = "com.genonbeta.TrebleShot.transaction.action.TASK_STATUS_CHANGE";

    public static final String EXTRA_HOTSPOT_CONFIG = "hotspotConfig";


    private final ExecutorService mExecutor = Executors.newFixedThreadPool(10);
    private final List<AsyncTask> mTaskList = new ArrayList<>();
    private int mForegroundActivitiesCount = 0;
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    private File mCrashLogFile;
    private NsdDaemon mNsdDaemon;
    private HotspotManager mHotspotManager;
    private MediaScannerConnection mMediaScanner;
    private NotificationHelper mNotificationHelper;
    private DynamicNotification mTasksNotification;
    private long mTaskNotificationTime;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mCrashLogFile = getApplicationContext().getFileStreamPath(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG);

        Thread.setDefaultUncaughtExceptionHandler(this);
        initializeSettings();

        mNsdDaemon = new NsdDaemon(getApplicationContext(), AppUtils.getKuick(this),
                AppUtils.getDefaultPreferences(getApplicationContext()));
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        mHotspotManager = HotspotManager.newInstance(this);
        mMediaScanner = new MediaScannerConnection(this, null);
        mNotificationHelper = new NotificationHelper(new NotificationUtils(getApplicationContext(),
                AppUtils.getKuick(getApplicationContext()), AppUtils.getDefaultPreferences(getApplicationContext())));

        mMediaScanner.connect();

        if (Build.VERSION.SDK_INT >= 26)
            mHotspotManager.setSecondaryCallback(new SecondaryHotspotCallback());

        if (!Keyword.Flavor.googlePlay.equals(AppUtils.getBuildFlavor())
                && !UpdateUtils.hasNewVersion(getApplicationContext())
                && (System.currentTimeMillis() - UpdateUtils.getLastTimeCheckedForUpdates(
                getApplicationContext())) >= AppConfig.DELAY_CHECK_FOR_UPDATES) {
            GitHubUpdater updater = UpdateUtils.getDefaultUpdater(getApplicationContext());
            UpdateUtils.checkForUpdates(getApplicationContext(), updater, false, null);
        }
    }

    public void attach(AsyncTask task)
    {
        runInternal(task);
    }

    @Nullable
    public AsyncTask findTaskBy(Identity identity)
    {
        List<AsyncTask> taskList = findTasksBy(identity);
        return taskList.size() > 0 ? taskList.get(0) : null;
    }

    @NonNull
    public synchronized List<AsyncTask> findTasksBy(Identity identity)
    {
        synchronized (mTaskList) {
            return findTasksBy(mTaskList, identity);
        }
    }

    public static <T extends AsyncTask> List<T> findTasksBy(List<T> taskList, Identity identity)
    {
        List<T> foundList = new ArrayList<>();
        for (T task : taskList)
            if (task.getIdentity().equals(identity))
                foundList.add(task);
        return foundList;
    }

    public static App from(android.app.Activity activity) throws IllegalStateException
    {
        if (activity.getApplication() instanceof App)
            return (App) activity.getApplication();

        throw new IllegalStateException("The app does not have an App instance.");
    }

    public HotspotManager getHotspotManager()
    {
        return mHotspotManager;
    }

    public WifiConfiguration getHotspotConfig()
    {
        return getHotspotManager().getConfiguration();
    }

    public MediaScannerConnection getMediaScanner()
    {
        return mMediaScanner;
    }

    public NotificationHelper getNotificationHelper()
    {
        return mNotificationHelper;
    }

    public NsdDaemon getNsdDaemon()
    {
        return mNsdDaemon;
    }

    private ExecutorService getSelfExecutor()
    {
        return mExecutor;
    }

    protected List<AsyncTask> getTaskList()
    {
        return mTaskList;
    }

    public <T extends AsyncTask> List<T> getTaskListOf(Class<T> clazz)
    {
        synchronized (mTaskList) {
            return getTaskListOf(mTaskList, clazz);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AsyncTask> List<T> getTaskListOf(List<? extends AsyncTask> taskList, Class<T> clazz)
    {
        List<T> foundList = new ArrayList<>();
        for (AsyncTask task : taskList)
            if (clazz.isInstance(task))
                foundList.add((T) task);
        return foundList;
    }

    public boolean hasTaskOf(Class<? extends AsyncTask> clazz)
    {
        synchronized (mTaskList) {
            return hasTaskOf(mTaskList, clazz);
        }
    }

    public static boolean hasTaskOf(List<? extends AsyncTask> taskList, Class<? extends AsyncTask> clazz)
    {
        for (AsyncTask task : taskList)
            if (clazz.isInstance(task))
                return true;
        return false;
    }

    public boolean hasTasks()
    {
        return mTaskList.size() > 0;
    }

    public static boolean hasTaskWith(List<? extends AsyncTask> taskList, Identity identity)
    {
        for (AsyncTask task : taskList)
            if (task.getIdentity().equals(identity))
                return true;
        return false;
    }

    private void initializeSettings()
    {
        //SharedPreferences defaultPreferences = AppUtils.getDefaultLocalPreferences(this);
        SharedPreferences defaultPreferences = AppUtils.getDefaultPreferences(this);
        Device localDevice = AppUtils.getLocalDevice(getApplicationContext());
        boolean nsdDefined = defaultPreferences.contains("nsd_enabled");
        boolean refVersion = defaultPreferences.contains("referral_version");

        PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false);

        if (!refVersion)
            defaultPreferences.edit()
                    .putInt("referral_version", localDevice.versionCode)
                    .apply();

        // Some pre-kitkat devices were soft rebooting when this feature was turned on by default.
        // So we will disable it for them and it will still remain as an option for the user.
        if (!nsdDefined)
            defaultPreferences.edit()
                    .putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
                    .apply();

        if (defaultPreferences.contains("migrated_version")) {
            int migratedVersion = defaultPreferences.getInt("migrated_version", localDevice.versionCode);

            if (migratedVersion < localDevice.versionCode) {
                // migrating to a new version

                if (migratedVersion <= 67)
                    AppUtils.getViewingPreferences(getApplicationContext()).edit()
                            .clear()
                            .apply();

                defaultPreferences.edit()
                        .putInt("migrated_version", localDevice.versionCode)
                        .putInt("previously_migrated_version", migratedVersion)
                        .apply();
            }
        } else
            defaultPreferences.edit()
                    .putInt("migrated_version", localDevice.versionCode)
                    .apply();
    }

    public void interruptTasksBy(Identity identity, boolean userAction)
    {
        synchronized (mTaskList) {
            for (AsyncTask task : findTasksBy(identity))
                task.interrupt(userAction);
        }
    }

    public void interruptAllTasks()
    {
        synchronized (mTaskList) {
            for (AsyncTask task : mTaskList) {
                task.interrupt(false);
                Log.d(TAG, "interruptAllTasks(): Ongoing task stopped: " + task.getName());
            }
        }
    }

    public static void interruptTasksBy(android.app.Activity activity, Identity identity, boolean userAction)
    {
        try {
            App.from(activity).interruptTasksBy(identity, userAction);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public static void notifyActivityInForeground(Activity activity, boolean inForeground)
    {
        ((App) activity.getApplication()).notifyActivityInForeground(inForeground);
    }

    public synchronized void notifyActivityInForeground(boolean inForeground)
    {
        if (mForegroundActivitiesCount == 0 && !inForeground)
            return;

        mForegroundActivitiesCount += inForeground ? 1 : -1;
        boolean inBg = mForegroundActivitiesCount == 0;
        boolean newlyInFg = mForegroundActivitiesCount == 1;
        Intent intent = new Intent(this, BackgroundService.class);

        if (newlyInFg)
            ContextCompat.startForegroundService(getApplicationContext(), intent);
        else if (inBg) {
            intent.setAction(BackgroundService.ACTION_END_SESSION)
                    .putExtra(BackgroundService.EXTRA_CHECK_FOR_TASKS, true);
            ContextCompat.startForegroundService(getApplicationContext(), intent);
        }

        Log.d(TAG, "notifyActivityInForeground: Count: " + mForegroundActivitiesCount);
    }

    public boolean publishTaskNotifications(boolean force)
    {
        if (System.nanoTime() <= mTaskNotificationTime && !force)
            return false;

        if (!hasTasks()) {
            if (mTasksNotification != null)
                mTasksNotification.cancel();
            return false;
        }

        List<AsyncTask> taskList;
        synchronized (mTaskList) {
            taskList = new ArrayList<>(mTaskList);
        }

        mTaskNotificationTime = System.nanoTime();
        mTasksNotification = mNotificationHelper.notifyTasksNotification(this, taskList, mTasksNotification);
        return true;
    }

    protected synchronized <T extends AsyncTask> void registerWork(T task)
    {
        synchronized (mTaskList) {
            mTaskList.add(task);
        }

        Log.d(TAG, "registerWork: " + task.getClass().getSimpleName());
        sendBroadcast(new Intent(ACTION_TASK_CHANGE));
    }

    public void run(final AsyncTask runningTask)
    {
        getSelfExecutor().submit(() -> attach(runningTask));
    }

    public static <T extends AsyncTask> void run(android.app.Activity activity, T task)
    {
        try {
            from(activity).run(task);
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    private void runInternal(AsyncTask runningTask)
    {
        registerWork(runningTask);

        try {
            runningTask.run(this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        unregisterWork(runningTask);
        publishTaskNotifications(true);
    }

    public void toggleHotspot()
    {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this))
            return;

        if (getHotspotManager().isEnabled())
            getHotspotManager().disable();
        else
            Log.d(TAG, "toggleHotspot: Enabling=" + getHotspotManager().enableConfigured(AppUtils.getHotspotName(
                    this), null));
    }

    @Override
    public void uncaughtException(@NonNull Thread t, @NonNull Throwable e)
    {
        try {
            if ((!mCrashLogFile.exists() || mCrashLogFile.delete()) && mCrashLogFile.createNewFile()
                    && mCrashLogFile.canWrite()) {
                StringBuilder stringBuilder = new StringBuilder();
                StackTraceElement[] stackTraceElements = e.getStackTrace();

                stringBuilder.append("--TREBLESHOT-CRASH-LOG--\n")
                        .append("\nException: ")
                        .append(e.getClass().getSimpleName())
                        .append("\nMessage: ")
                        .append(e.getMessage())
                        .append("\nCause: ")
                        .append(e.getCause()).append("\nDate: ")
                        .append(DateFormat.getLongDateFormat(this).format(new Date(
                                System.currentTimeMillis())))
                        .append("\n\n")
                        .append("--STACKTRACE--\n\n");

                if (stackTraceElements.length > 0)
                    for (StackTraceElement element : stackTraceElements) {
                        stringBuilder.append(element.getClassName())
                                .append(".")
                                .append(element.getMethodName())
                                .append(":")
                                .append(element.getLineNumber())
                                .append("\n");
                    }

                FileOutputStream outputStream = new FileOutputStream(mCrashLogFile);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(stringBuilder.toString().getBytes());

                int len;
                byte[] buffer = new byte[8196];

                while ((len = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, len);
                    outputStream.flush();
                }

                outputStream.close();
                inputStream.close();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        mDefaultExceptionHandler.uncaughtException(t, e);
    }

    protected synchronized void unregisterWork(AsyncTask task)
    {
        synchronized (mTaskList) {
            mTaskList.remove(task);
        }

        Log.d(TAG, "unregisterWork: " + task.getClass().getSimpleName());
        sendBroadcast(new Intent(ACTION_TASK_CHANGE));
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private class SecondaryHotspotCallback extends WifiManager.LocalOnlyHotspotCallback
    {
        @Override
        public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation)
        {
            super.onStarted(reservation);
            sendBroadcast(new Intent(ACTION_OREO_HOTSPOT_STARTED)
                    .putExtra(EXTRA_HOTSPOT_CONFIG, reservation.getWifiConfiguration()));
        }
    }
}
