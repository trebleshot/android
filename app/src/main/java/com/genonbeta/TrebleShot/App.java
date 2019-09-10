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

import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;

import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.updatewithgithub.GitHubUpdater;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */

public class App extends Application implements Thread.UncaughtExceptionHandler
{
    public static final String TAG = App.class.getSimpleName();

    private int mForegroundActivitiesCount = 0;
    private Thread.UncaughtExceptionHandler mDefaultExceptionHandler;
    private File mCrashLogFile;

    @Override
    public void onCreate()
    {
        super.onCreate();

        mCrashLogFile = getApplicationContext().getFileStreamPath(
                Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG);
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(this);

        initializeSettings();

        if (!Keyword.Flavor.googlePlay.equals(AppUtils.getBuildFlavor())
                && !UpdateUtils.hasNewVersion(getApplicationContext())
                && (System.currentTimeMillis() - UpdateUtils.getLastTimeCheckedForUpdates(getApplicationContext())) >= AppConfig.DELAY_CHECK_FOR_UPDATES) {
            GitHubUpdater updater = UpdateUtils.getDefaultUpdater(getApplicationContext());
            UpdateUtils.checkForUpdates(getApplicationContext(), updater, false, null);
        }
    }

    @Override
    public void uncaughtException(Thread t, Throwable e)
    {
        try {
            if ((!mCrashLogFile.exists() || mCrashLogFile.delete())
                    && mCrashLogFile.createNewFile() && mCrashLogFile.canWrite()) {
                StringBuilder stringBuilder = new StringBuilder();
                StackTraceElement[] stackTraceElements = e.getStackTrace();

                stringBuilder.append("--TREBLESHOT-CRASH-LOG--\n");

                stringBuilder.append("\nException: " + e.getClass().getSimpleName());
                stringBuilder.append("\nMessage: " + e.getMessage());
                stringBuilder.append("\nCause: " + e.getCause());
                stringBuilder.append("\nDate: " + DateFormat.getLongDateFormat(this).format(
                        new Date(System.currentTimeMillis())));
                stringBuilder.append("\n\n");
                stringBuilder.append("--STACKTRACE--\n\n");

                if (stackTraceElements.length > 0)
                    for (StackTraceElement element : stackTraceElements) {
                        stringBuilder.append(element.getClassName());
                        stringBuilder.append(".");
                        stringBuilder.append(element.getMethodName());
                        stringBuilder.append(":");
                        stringBuilder.append(element.getLineNumber());
                        stringBuilder.append("\n");
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

    public static void notifyActivityInForeground(Activity activity, boolean inForeground)
    {
        ((App) activity.getApplication()).notifyActivityInForeground(inForeground);
    }

    public synchronized void notifyActivityInForeground(boolean inForeground)
    {
        if (mForegroundActivitiesCount < 0 && !inForeground)
            return;

        boolean hadNone = mForegroundActivitiesCount == 0;
        mForegroundActivitiesCount += inForeground ? 1 : -1;

        if ((hadNone && inForeground) || mForegroundActivitiesCount == 0)
            AppUtils.startForegroundService(this,
                    new Intent(this, CommunicationService.class)
                            .setAction(CommunicationService.ACTION_SERVICE_STATUS)
                            .putExtra(CommunicationService.EXTRA_STATUS_STARTED, inForeground));

        Log.d(TAG, "notifyActivityInForeground: Count: " + mForegroundActivitiesCount);
    }

    private void initializeSettings()
    {
        //SharedPreferences defaultPreferences = AppUtils.getDefaultLocalPreferences(this);
        SharedPreferences defaultPreferences = AppUtils.getDefaultPreferences(this);
        NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());
        boolean nsdDefined = defaultPreferences.contains("nsd_enabled");
        boolean refVersion = defaultPreferences.contains("referral_version");

        PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false);

        if (!refVersion)
            defaultPreferences.edit()
                    .putInt("referral_version", localDevice.versionNumber)
                    .apply();

        // Some pre-kitkat devices were soft rebooting when this feature was turned on by default.
        // So we will disable it for them and they will still remain an option for the user.
        if (!nsdDefined)
            defaultPreferences.edit()
                    .putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
                    .apply();

        if (defaultPreferences.contains("migrated_version")) {
            int migratedVersion = defaultPreferences.getInt("migrated_version", localDevice.versionNumber);

            if (migratedVersion < localDevice.versionNumber) {
                // migrating to a new version

                if (migratedVersion <= 67)
                    AppUtils.getViewingPreferences(getApplicationContext()).edit()
                            .clear()
                            .apply();

                defaultPreferences.edit()
                        .putInt("migrated_version", localDevice.versionNumber)
                        .putInt("previously_migrated_version", migratedVersion)
                        .apply();
            }
        } else
            defaultPreferences.edit()
                    .putInt("migrated_version", localDevice.versionNumber)
                    .apply();
    }
}
