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
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.NetworkSuggestion;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
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

    public void makeAcquaintance(final Activity activity, final UITask task, final Object object, final int accessPin,
                                 final NetworkDeviceLoader.OnDeviceRegisteredListener registerListener)
    {
        WorkerService.RunningTask runningTask = new WorkerService.RunningTask()
        {
            private boolean mConnected = false;
            private InetAddress mAddress;

            @Override
            public void onRun()
            {
                final DialogInterface.OnClickListener retryButtonListener = (dialog, which) -> makeAcquaintance(
                        activity, task, object, accessPin, registerListener);

                try {
                    if (object instanceof NetworkDeviceListAdapter.NetworkSpecifier) {
                        boolean canContinue = true;

                        if (object instanceof NetworkSuggestion) {
                            // We might have used WifiManager.ACTION_WIFI_NETWORK_SUGGESTION_POST_CONNECTION intent
                            // to proceed, but as we are already going to do concurrent task, it should become available
                            // during that period.
                            final int status = getConnectionUtils().suggestNetwork((NetworkSuggestion) object);

                            if (status != WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
                                    && status != WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE) {
                                canContinue = false;

                                AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                                        .setTitle(R.string.text_error)
                                        .setNegativeButton(R.string.butn_close, null);

                                switch (status) {
                                    case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP:
                                        dialogBuilder.setMessage(R.string.text_errorExceededMaximumSuggestions)
                                                .setPositiveButton(R.string.butn_openSettings,
                                                        (dialog, which) -> activity.startActivity(new Intent(
                                                                Settings.ACTION_WIFI_SETTINGS)));
                                        break;
                                    case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                                        dialogBuilder.setMessage(R.string.text_errorNetworkSuggestionsDisallowed)
                                                .setPositiveButton(R.string.butn_openSettings,
                                                        (dialog, which) -> AppUtils.startApplicationDetails(activity));

                                    case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL:
                                        dialogBuilder.setMessage(R.string.text_errorNetworkSuggestionInternal)
                                                .setPositiveButton(R.string.butn_feedbackContact,
                                                        (dialog, which) -> AppUtils.startFeedbackActivity(activity));
                                }

                                postDialog(activity, dialogBuilder.create());
                            }
                        }

                        if (canContinue) {
                            mAddress = getConnectionUtils().establishHotspotConnection(getInterrupter(),
                                    (NetworkDeviceListAdapter.NetworkSpecifier) object,
                                    (delimiter, timePassed) -> timePassed >= 30000);
                        }
                    } else if (object instanceof InetAddress)
                        mAddress = (InetAddress) object;
                    else if (object instanceof DeviceConnection)
                        mAddress = ((DeviceConnection) object).toInet4Address();

                    if (mAddress != null) {
                        mConnected = setupConnection(activity, mAddress, accessPin, (database, device, connection) -> {
                            // we may be working with direct IP scan
                            new Handler(Looper.getMainLooper()).post(() -> {
                                if (registerListener != null)
                                    registerListener.onDeviceRegistered(database, device, connection);
                            });
                        }, retryButtonListener) != null;
                    }

                    if (!mConnected && !getInterrupter().interruptedByUser()) {
                        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity)
                                .setMessage(R.string.mesg_connectionFailure)
                                .setNegativeButton(R.string.butn_close, null)
                                .setPositiveButton(R.string.butn_retry, retryButtonListener);

                        if (object instanceof NetworkDevice)
                            dialogBuilder.setTitle(((NetworkDevice) object).nickname);

                        postDialog(activity, dialogBuilder.create());
                    }
                } catch (Exception ignored) {

                } finally {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (task != null && !activity.isFinishing())
                            task.updateTaskStopped();
                    });
                }
            }
        }.setTitle(activity.getString(R.string.mesg_completing))
                .setIconRes(R.drawable.ic_compare_arrows_white_24dp_static);

        runningTask.run(activity);

        if (task != null)
            task.updateTaskStarted(runningTask.getInterrupter());
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
            if (!activity.isFinishing()) {
                dialog.show();
            }
        });
    }

    @WorkerThread
    public NetworkDevice setupConnection(final Activity activity, final InetAddress inetAddress, int accessPin,
                                         final NetworkDeviceLoader.OnDeviceRegisteredListener listener,
                                         final DialogInterface.OnClickListener retryButtonListener)
    {
        return CommunicationBridge.connect(AppUtils.getKuick(activity), NetworkDevice.class, client -> {
            try {
                client.setPin(accessPin);

                CoolSocket.ActiveConnection activeConnection = client.communicate(inetAddress, false);

                activeConnection.reply(new JSONObject()
                        .put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE)
                        .toString());

                NetworkDevice device = client.getDevice();
                JSONObject receivedReply = new JSONObject(activeConnection.receive().response);

                if (receivedReply.has(Keyword.RESULT) && receivedReply.getBoolean(Keyword.RESULT)
                        && device.id != null) {
                    final DeviceConnection connection = NetworkDeviceLoader.processConnection(
                            AppUtils.getKuick(activity), device, inetAddress.getHostAddress());

                    if (listener != null)
                        listener.onDeviceRegistered(AppUtils.getKuick(activity), device, connection);
                } else
                    showConnectionRejectionInformation(activity, device, receivedReply, retryButtonListener);

                client.setReturn(device);
            } catch (Exception e) {
                e.printStackTrace();
            }

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
                    ? R.string.mesg_stoppingSelfHotspot
                    : R.string.mesg_startingSelfHotspot)
                    .show();

        AppUtils.startForegroundService(getConnectionUtils().getContext(),
                new Intent(getConnectionUtils().getContext(), CommunicationService.class)
                        .setAction(CommunicationService.ACTION_TOGGLE_HOTSPOT));

        watcher.onResultReturned(true, false);

        return true;
    }


    public boolean turnOnWiFi(final Activity activity, final int requestId, final RequestWatcher watcher)
    {
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
