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

package com.genonbeta.TrebleShot.app;

import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.request.Request;
import com.bumptech.glide.request.target.SizeReadyCallback;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.WelcomeActivity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Activity extends AppCompatActivity
{
    public static final String ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED";

    public static final int REQUEST_PICK_PROFILE_PHOTO = 1000;

    private final List<WorkerService.RunningTask> mAttachedTasks = new ArrayList<>();
    private AlertDialog mOngoingRequest;
    private IntentFilter mFilter = new IntentFilter();
    private boolean mDarkThemeRequested = false;
    private boolean mAmoledDarkThemeRequested = false;
    private boolean mThemeLoadingFailed = false;
    private boolean mCustomFontsEnabled = false;
    private boolean mSkipPermissionRequest = false;
    private boolean mWelcomePageDisallowed = false;

    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED.equals(intent.getAction()))
                checkForThemeChange();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        mDarkThemeRequested = isDarkThemeRequested();
        mAmoledDarkThemeRequested = isAmoledDarkThemeRequested();
        mCustomFontsEnabled = isUsingCustomFonts();

        mFilter.addAction(ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED);

        if (mDarkThemeRequested) {
            try {
                @StyleRes
                int currentThemeRes = getPackageManager().getActivityInfo(getComponentName(), 0).theme;

                Log.d(Activity.class.getSimpleName(), "Activity theme id: " + currentThemeRes);

                if (currentThemeRes == 0)
                    currentThemeRes = getApplicationInfo().theme;

                Log.d(Activity.class.getSimpleName(), "After change theme: " + currentThemeRes);

                @StyleRes
                int appliedRes = 0;

                switch (currentThemeRes) {
                    case R.style.Theme_TrebleShot:
                        appliedRes = R.style.Theme_TrebleShot_Dark;
                        break;
                    case R.style.Theme_TrebleShot_NoActionBar:
                        appliedRes = R.style.Theme_TrebleShot_Dark_NoActionBar;
                        break;
                    case R.style.Theme_TrebleShot_NoActionBar_StaticStatusBar:
                        appliedRes = R.style.Theme_TrebleShot_Dark_NoActionBar_StaticStatusBar;
                        break;
                    default:
                        Log.e(Activity.class.getSimpleName(), "There is an unknown theme applied. "
                                + "Resources could fail. "
                                + "Dark theme won't be effective");
                }

                mThemeLoadingFailed = appliedRes == 0;

                if (!mThemeLoadingFailed) {
                    setTheme(appliedRes);

                    if (mAmoledDarkThemeRequested)
                        getTheme().applyStyle(R.style.BlackPatch, true);
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Apply the Preferred Font Family as a patch if enabled
        if (mCustomFontsEnabled) {
            Log.d(Activity.class.getSimpleName(), "Custom fonts have been applied");
            getTheme().applyStyle(R.style.TextAppearance_Cantarell, true);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        checkForThemeChange();

        if (Build.VERSION.SDK_INT >= 23)
            registerReceiver(mReceiver, mFilter);

        if (!hasIntroductionShown() && !mWelcomePageDisallowed) {
            startActivity(new Intent(this, WelcomeActivity.class));
            finish();
        } else if (!AppUtils.checkRunningConditions(this)) {
            if (!mSkipPermissionRequest)
                requestRequiredPermissions(true);
        }
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        if (Build.VERSION.SDK_INT >= 23)
            unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart()
    {
        super.onStart();

        if (AppUtils.checkRunningConditions(this))
            App.notifyActivityInForeground(this, true);
    }

    @Override
    protected void onStop()
    {
        super.onStop();

        synchronized (mAttachedTasks) {
            for (WorkerService.RunningTask task : mAttachedTasks) {
                task.detachAnchor();
            }

            mAttachedTasks.clear();
        }

        if (AppUtils.checkRunningConditions(this))
            App.notifyActivityInForeground(this, false);
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
    }

    protected void onPreviousRunningTask(@Nullable WorkerService.RunningTask task)
    {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (AppUtils.checkRunningConditions(this))
            AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class));
        else
            requestRequiredPermissions(!mSkipPermissionRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_PICK_PROFILE_PHOTO)
            if (resultCode == RESULT_OK && data != null) {
                Uri chosenImageUri = data.getData();

                if (chosenImageUri != null) {
                    GlideApp.with(this)
                            .load(chosenImageUri)
                            .centerCrop()
                            .override(200, 200)
                            .into(new Target<Drawable>()
                            {
                                @Override
                                public void onLoadStarted(@Nullable Drawable placeholder)
                                {

                                }

                                @Override
                                public void onLoadFailed(@Nullable Drawable errorDrawable)
                                {

                                }

                                @Override
                                public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition)
                                {
                                    try {
                                        Bitmap bitmap = Bitmap.createBitmap(AppConfig.PHOTO_SCALE_FACTOR, AppConfig.PHOTO_SCALE_FACTOR, Bitmap.Config.ARGB_8888);
                                        Canvas canvas = new Canvas(bitmap);
                                        FileOutputStream outputStream = openFileOutput("profilePicture", MODE_PRIVATE);

                                        resource.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                                        resource.draw(canvas);
                                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                                        outputStream.close();

                                        notifyUserProfileChanged();
                                    } catch (Exception error) {
                                        error.printStackTrace();
                                    }
                                }

                                @Override
                                public void onLoadCleared(@Nullable Drawable placeholder)
                                {

                                }

                                @Override
                                public void getSize(@NonNull SizeReadyCallback cb)
                                {

                                }

                                @Override
                                public void removeCallback(@NonNull SizeReadyCallback cb)
                                {

                                }

                                @Nullable
                                @Override
                                public Request getRequest()
                                {
                                    return null;
                                }

                                @Override
                                public void setRequest(@Nullable Request request)
                                {

                                }

                                @Override
                                public void onStart()
                                {

                                }

                                @Override
                                public void onStop()
                                {

                                }

                                @Override
                                public void onDestroy()
                                {

                                }
                            });
                }
            }
    }

    public void onUserProfileUpdated()
    {

    }

    public void attachRunningTask(WorkerService.RunningTask task)
    {
        synchronized (mAttachedTasks) {
            mAttachedTasks.add(task);
        }
    }

    public boolean isPowerSaveMode()
    {
        if (android.os.Build.VERSION.SDK_INT < 23)
            return false;

        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        return powerManager != null && powerManager.isPowerSaveMode();
    }

    public void checkForThemeChange()
    {
        if (((mDarkThemeRequested != isDarkThemeRequested() || (isDarkThemeRequested()
                && mAmoledDarkThemeRequested != isAmoledDarkThemeRequested())) && !mThemeLoadingFailed)
                || mCustomFontsEnabled != isUsingCustomFonts())
            recreate();
    }

    public boolean checkForTasks()
    {
        ServiceConnection serviceConnection = new ServiceConnection()
        {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service)
            {
                WorkerService workerService = ((WorkerService.LocalBinder) service).getService();

                WorkerService.RunningTask task = workerService
                        .findTaskByHash(WorkerService.intentHash(getIntent()));

                onPreviousRunningTask(task);

                if (task != null)
                    synchronized (mAttachedTasks) {
                        attachRunningTask(task);
                    }

                unbindService(this);
            }

            @Override
            public void onServiceDisconnected(ComponentName name)
            {

            }
        };

        return bindService(new Intent(Activity.this, WorkerService.class),
                serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Exits app closing all the active services and connections.
     * This will also prevent this activity from notifying {@link CommunicationService}
     * as the user leaves to the state of {@link Activity#onPause()}
     */
    public void exitApp()
    {
        stopService(new Intent(this, CommunicationService.class));
        stopService(new Intent(this, DeviceScannerService.class));
        stopService(new Intent(this, WorkerService.class));

        finish();
    }

    public AccessDatabase getDatabase()
    {
        return AppUtils.getDatabase(this);
    }

    protected SharedPreferences getDefaultPreferences()
    {
        return AppUtils.getDefaultPreferences(this);
    }

    public boolean hasIntroductionShown()
    {
        return getDefaultPreferences().getBoolean("introduction_shown", false);
    }

    public boolean isAmoledDarkThemeRequested()
    {
        return getDefaultPreferences().getBoolean("amoled_theme", false);
    }

    public boolean isDarkThemeRequested()
    {
        String value = getDefaultPreferences().getString("theme", "light");
        int systemWideTheme = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        return "dark".equals(value) || ("system".equals(value) && systemWideTheme == Configuration.UI_MODE_NIGHT_YES)
                || ("battery".equals(value) && isPowerSaveMode());
    }

    public boolean isUsingCustomFonts()
    {
        return getDefaultPreferences().getBoolean("custom_fonts", false) && Build.VERSION.SDK_INT >= 16;
    }

    public void loadProfilePictureInto(String deviceName, ImageView imageView)
    {
        try {
            FileInputStream inputStream = openFileInput("profilePicture");
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            GlideApp.with(this)
                    .load(bitmap)
                    .circleCrop()
                    .into(imageView);
        } catch (FileNotFoundException e) {
            imageView.setImageDrawable(AppUtils.getDefaultIconBuilder(this).buildRound(deviceName));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void notifyUserProfileChanged()
    {
        if (!isFinishing())
            runOnUiThread(this::onUserProfileUpdated);
    }

    public void setSkipPermissionRequest(boolean skip)
    {
        mSkipPermissionRequest = skip;
    }

    public void requestProfilePictureChange()
    {
        startActivityForResult(new Intent(Intent.ACTION_PICK).setType("image/*"), REQUEST_PICK_PROFILE_PHOTO);
    }

    public boolean requestRequiredPermissions(boolean killActivityOtherwise)
    {
        if (mOngoingRequest != null && mOngoingRequest.isShowing())
            return false;

        for (RationalePermissionRequest.PermissionRequest request : AppUtils.getRequiredPermissions(this))
            if ((mOngoingRequest = RationalePermissionRequest.requestIfNecessary(this, request,
                    killActivityOtherwise)) != null)
                return false;

        return true;
    }

    public void setWelcomePageDisallowed(boolean disallow)
    {
        mWelcomePageDisallowed = disallow;
    }

    public void startProfileEditor()
    {
        new ProfileEditorDialog(this).show();
    }

    public interface OnBackPressedListener
    {
        boolean onBackPressed();
    }

    public interface OnPreloadArgumentWatcher
    {
        Bundle passPreLoadingArguments();
    }
}
