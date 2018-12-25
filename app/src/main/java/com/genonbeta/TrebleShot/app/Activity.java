package com.genonbeta.TrebleShot.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;

public abstract class Activity extends AppCompatActivity
{
    private AlertDialog mOngoingRequest;
    private boolean mDarkThemeRequested = false;
    private boolean mThemeLoadingFailed = false;
    private boolean mCustomFontsEnabled = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        mDarkThemeRequested = isDarkThemeRequested();
        mCustomFontsEnabled = isUsingCustomFonts();

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

                if (!mThemeLoadingFailed)
                    setTheme(appliedRes);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        // Apply Ubuntu Font Family as a patch if enabled
        if (mCustomFontsEnabled) {
            Log.d(Activity.class.getSimpleName(), "Custom fonts have been applied");
            getTheme().applyStyle(R.style.TextAppearance_Ubuntu, true);
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if ((mDarkThemeRequested != isDarkThemeRequested() && !mThemeLoadingFailed)
            || mCustomFontsEnabled != isUsingCustomFonts())
            recreate();

        if (!AppUtils.checkRunningConditions(this))
            requestRequiredPermissions();

        AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
                .setAction(CommunicationService.ACTION_SERVICE_STATUS)
                .putExtra(CommunicationService.EXTRA_STATUS_STARTED, true));
    }

    @Override
    protected void onPause()
    {
        super.onPause();

        AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
                .setAction(CommunicationService.ACTION_SERVICE_STATUS)
                .putExtra(CommunicationService.EXTRA_STATUS_STARTED, false));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (AppUtils.checkRunningConditions(this))
            AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class));
        else
            requestRequiredPermissions();
    }

    public AccessDatabase getDatabase()
    {
        return AppUtils.getDatabase(this);
    }

    protected SharedPreferences getDefaultPreferences()
    {
        return AppUtils.getDefaultPreferences(this);
    }

    public boolean isDarkThemeRequested()
    {
        return getDefaultPreferences().getBoolean("dark_theme", false);
    }

    public boolean isUsingCustomFonts()
    {
        return getDefaultPreferences().getBoolean("custom_fonts", false)
                && getDefaultPreferences().getBoolean("developer_mode", false);
    }

    public boolean requestRequiredPermissions()
    {
        if (mOngoingRequest != null && mOngoingRequest.isShowing())
            return false;

        for (RationalePermissionRequest.PermissionRequest request : AppUtils.getRequiredPermissions(this))
            if ((mOngoingRequest = RationalePermissionRequest.requestIfNecessary(this, request)) != null)
                return false;

        return true;
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
