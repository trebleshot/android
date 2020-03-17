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
import android.os.PowerManager;
import android.util.Log;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.WelcomeActivity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableBgTask;
import com.genonbeta.TrebleShot.util.AppUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

public abstract class Activity extends AppCompatActivity
{
    private static final String TAG = Activity.class.getSimpleName();

    public static final String ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED";

    public static final int REQUEST_PICK_PROFILE_PHOTO = 1000;

    private final List<BaseAttachableBgTask> mAttachedTasks = new ArrayList<>();
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
            for (BaseAttachableBgTask task : mAttachedTasks)
                task.detachAnchor();

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

    protected void onPreviousRunningTask(@Nullable BaseAttachableBgTask task)
    {

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (AppUtils.checkRunningConditions(this))
            AppUtils.startService(this, new Intent(this, BackgroundService.class));
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
                            .into(new CustomTarget<Drawable>()
                            {
                                @Override
                                public void onResourceReady(@NonNull Drawable resource,
                                                            @Nullable Transition<? super Drawable> transition)
                                {
                                    try {
                                        Bitmap bitmap = Bitmap.createBitmap(AppConfig.PHOTO_SCALE_FACTOR,
                                                AppConfig.PHOTO_SCALE_FACTOR, Bitmap.Config.ARGB_8888);
                                        Canvas canvas = new Canvas(bitmap);
                                        FileOutputStream outputStream = openFileOutput("profilePicture",
                                                MODE_PRIVATE);

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
                            });
                }
            }
    }

    public void onUserProfileUpdated()
    {

    }

    public void attachRunningTask(BaseAttachableBgTask task)
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

    public void checkForTasks()
    {
        try {
            BackgroundService service = AppUtils.getBgService(this);
            BackgroundTask task = service.findTaskBy(BackgroundService.hashIntent(getIntent()));

            if (task == null)
                onPreviousRunningTask(null);
            else if (task instanceof BaseAttachableBgTask) {
                BaseAttachableBgTask attachable = (BaseAttachableBgTask) task;
                onPreviousRunningTask(attachable);

                synchronized (mAttachedTasks) {
                    attachRunningTask(attachable);
                }
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    /**
     * Exits app closing all the active services and connections.
     * This will also prevent this activity from notifying {@link BackgroundService}
     * as the user leaves to the state of {@link Activity#onPause()}
     */
    public void exitApp()
    {
        stopService(new Intent(this, DeviceScannerService.class));
        stopService(new Intent(this, BackgroundService.class));

        finish();
    }

    public Kuick getDatabase()
    {
        return AppUtils.getKuick(this);
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
