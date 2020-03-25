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

package com.genonbeta.TrebleShot.ui;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.backgroundservice.BackgroundTask;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.InetAddress;

/**
 * created by: veli
 * date: 15/04/18 18:44
 */
public class UIConnectionUtils
{
    public static final String TAG = "UIConnectionUtils";

    private SnackbarPlacementProvider mSnackbarSupport;
    private boolean mWirelessEnableRequested = false;
    private ConnectionUtils mConnectionUtils;

    public UIConnectionUtils(ConnectionUtils connectionUtils, SnackbarPlacementProvider snackbarSupport)
    {
        mConnectionUtils = connectionUtils;
        mSnackbarSupport = snackbarSupport;
    }

    public static void showConnectionRejectionInformation(final Activity activity, final NetworkDevice device,
                                                          final JSONObject clientResponse,
                                                          final DialogInterface.OnClickListener retryButtonListener)
    {
        try {
            if (clientResponse.has(Keyword.ERROR)) {
                if (clientResponse.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_ALLOWED))
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (!activity.isFinishing())
                            new AlertDialog.Builder(activity)
                                    .setTitle(R.string.mesg_notAllowed)
                                    .setMessage(activity.getString(R.string.text_notAllowedHelp, device.nickname,
                                            AppUtils.getLocalDeviceName(activity)))
                                    .setNegativeButton(R.string.butn_close, null)
                                    .setPositiveButton(R.string.butn_retry, retryButtonListener)
                                    .show();
                    });
            } else
                showUnknownError(activity, retryButtonListener);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public static void showUnknownError(final Activity activity,
                                        final DialogInterface.OnClickListener retryButtonListener)
    {
        postDialog(activity, new AlertDialog.Builder(activity)
                .setMessage(R.string.mesg_somethingWentWrong)
                .setNegativeButton(R.string.butn_close, null)
                .setPositiveButton(R.string.butn_retry, retryButtonListener)
                .create());
    }

    public ConnectionUtils getConnectionUtils()
    {
        return mConnectionUtils;
    }

    public SnackbarPlacementProvider getSnackbarSupport()
    {
        return mSnackbarSupport;
    }

    public boolean notifyWirelessRequestHandled()
    {
        boolean returnedState = mWirelessEnableRequested;
        mWirelessEnableRequested = false;
        return returnedState;
    }

    public static void postDialog(Activity activity, AlertDialog dialog)
    {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (!activity.isFinishing())
                dialog.show();
        });
    }

    public void showConnectionOptions(final Activity activity, final int locationPermRequestId, final RequestWatcher watcher)
    {
        if (!getConnectionUtils().getWifiManager().isWifiEnabled())
            getSnackbarSupport().createSnackbar(R.string.mesg_suggestSelfHotspot)
                    .setAction(R.string.butn_enable, view -> {
                        mWirelessEnableRequested = true;
                        turnOnWiFi(activity, locationPermRequestId, watcher);
                    })
                    .show();
        else if (validateLocationPermission(activity, locationPermRequestId, watcher)) {
            watcher.onResultReturned(true, false);

            getSnackbarSupport().createSnackbar(R.string.mesg_scanningSelfHotspot)
                    .setAction(R.string.butn_wifiSettings, view -> activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                    .show();
        }

        watcher.onResultReturned(true, false);
    }

    public boolean toggleHotspot(boolean conditional, final FragmentActivity activity, final int locationPermRequestId,
                                 final RequestWatcher watcher)
    {
        if (!HotspotUtils.isSupported())
            return false;

        DialogInterface.OnClickListener defaultNegativeListener = (dialog, which) -> watcher.onResultReturned(
                false, false);

        if (conditional) {
            if (Build.VERSION.SDK_INT >= 26 && !validateLocationPermission(activity, locationPermRequestId, watcher))
                return false;

            if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(getConnectionUtils().getContext())) {
                new AlertDialog.Builder(getConnectionUtils().getContext())
                        .setMessage(R.string.mesg_errorHotspotPermission)
                        .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                        .setPositiveButton(R.string.butn_settings, (dialog, which) -> {
                            activity.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    .setData(Uri.parse("package:" + getConnectionUtils().getContext().getPackageName()))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));

                            watcher.onResultReturned(false, true);
                        })
                        .show();

                return false;
            } else if (Build.VERSION.SDK_INT < 26 && !getConnectionUtils().getHotspotUtils().isEnabled()
                    && getConnectionUtils().isMobileDataActive()) {
                new AlertDialog.Builder(getConnectionUtils().getContext())
                        .setMessage(R.string.mesg_warningHotspotMobileActive)
                        .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                        .setPositiveButton(R.string.butn_skip, (dialog, which) -> {
                            // no need to call watcher due to recycle
                            toggleHotspot(false, activity, locationPermRequestId, watcher);
                        })
                        .show();

                return false;
            }
        }

        WifiConfiguration wifiConfiguration = getConnectionUtils().getHotspotUtils().getConfiguration();

        if (!getConnectionUtils().getHotspotUtils().isEnabled() || (wifiConfiguration != null
                && AppUtils.getHotspotName(getConnectionUtils().getContext()).equals(wifiConfiguration.SSID)))
            getSnackbarSupport().createSnackbar(getConnectionUtils().getHotspotUtils().isEnabled()
                    ? R.string.mesg_stoppingSelfHotspot : R.string.mesg_startingSelfHotspot)
                    .show();

        toggleHotspot(activity);
        watcher.onResultReturned(true, false);

        return true;
    }

    private void toggleHotspot(Activity activity)
    {
        try {
            AppUtils.getBgService(activity).toggleHotspot();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public boolean turnOnWiFi(final Activity activity, final int requestId, final RequestWatcher watcher)
    {
        // FIXME: 18.03.2020 Wi-Fi state is not alterable on API 29
        if (getConnectionUtils().getWifiManager().setWifiEnabled(true)) {
            getSnackbarSupport().createSnackbar(R.string.mesg_turningWiFiOn).show();
            watcher.onResultReturned(true, false);
            return true;
        } else
            new AlertDialog.Builder(getConnectionUtils().getContext())
                    .setMessage(R.string.mesg_wifiEnableFailed)
                    .setNegativeButton(R.string.butn_close, (dialog, which) -> watcher.onResultReturned(
                            false, false))
                    .setPositiveButton(R.string.butn_settings, (dialog, which) -> {
                        activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        watcher.onResultReturned(false, true);
                    })
                    .show();

        return false;
    }

    public boolean validateLocationPermission(final Activity activity, final int requestId,
                                              final RequestWatcher watcher)
    {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        final DialogInterface.OnClickListener defaultNegativeListener = (dialog, which) -> watcher.onResultReturned(
                false, false);

        if (!getConnectionUtils().hasLocationPermission(getConnectionUtils().getContext())) {
            new AlertDialog.Builder(getConnectionUtils().getContext())
                    .setMessage(R.string.mesg_locationPermissionRequiredSelfHotspot)
                    .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                    .setPositiveButton(R.string.butn_ask, (dialog, which) -> {
                        watcher.onResultReturned(false, true);
                        // No, I am not going to add an if statement since when it is not needed
                        // the main method returns true.
                        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, requestId);
                    })
                    .show();

            return false;
        }

        if (!getConnectionUtils().isLocationServiceEnabled()) {
            new AlertDialog.Builder(getConnectionUtils().getContext())
                    .setMessage(R.string.mesg_locationDisabledSelfHotspot)
                    .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                    .setPositiveButton(R.string.butn_locationSettings, (dialog, which) -> {
                        watcher.onResultReturned(false, true);
                        activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    })
                    .show();

            return false;
        }

        watcher.onResultReturned(true, false);

        return true;
    }

    public interface RequestWatcher
    {
        void onResultReturned(boolean result, boolean shouldWait);
    }
}
