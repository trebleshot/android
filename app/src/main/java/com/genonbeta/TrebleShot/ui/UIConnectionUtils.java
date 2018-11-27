package com.genonbeta.TrebleShot.ui;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

/**
 * created by: veli
 * date: 15/04/18 18:44
 */
public class UIConnectionUtils
{
    public static final String TAG = "UIConnectionUtils";
    public static final int WORKER_TASK_CONNECT_TS_NETWORK = 1;

    private SnackbarSupport mSnackbarSupport;
    private boolean mShowHotspotInfo = false;
    private boolean mWirelessEnableRequested = false;
    private ConnectionUtils mConnectionUtils;

    public UIConnectionUtils(ConnectionUtils connectionUtils, SnackbarSupport snackbarSupport)
    {
        mConnectionUtils = connectionUtils;
        mSnackbarSupport = snackbarSupport;
    }

    public ConnectionUtils getConnectionUtils()
    {
        return mConnectionUtils;
    }

    public SnackbarSupport getSnackbarSupport()
    {
        return mSnackbarSupport;
    }

    public void makeAcquaintance(final Context context, final AccessDatabase database,
                                 final UITask task, final Object object, final int accessPin,
                                 final NetworkDeviceLoader.OnDeviceRegisteredListener registerListener)
    {
        WorkerService.RunningTask runningTask = new WorkerService.RunningTask(TAG, WORKER_TASK_CONNECT_TS_NETWORK)
        {
            private boolean mConnected = false;
            private String mRemoteAddress;

            @Override
            public void onRun()
            {
                try {
                    if (object instanceof NetworkDeviceListAdapter.HotspotNetwork) {
                        mRemoteAddress = getConnectionUtils().establishHotspotConnection(getInterrupter(), (NetworkDeviceListAdapter.HotspotNetwork) object, new ConnectionUtils.TimeoutListener()
                        {
                            @Override
                            public boolean onTimePassed(int delimiter, long timePassed)
                            {
                                return timePassed >= 20000;
                            }
                        });
                    } else if (object instanceof String)
                        mRemoteAddress = (String) object;

                    if (mRemoteAddress != null) {
                        mConnected = getConnectionUtils().setupConnection(database, mRemoteAddress, accessPin, new NetworkDeviceLoader.OnDeviceRegisteredListener()
                        {
                            @Override
                            public void onDeviceRegistered(final AccessDatabase database, final NetworkDevice device, final NetworkDevice.Connection connection)
                            {
                                // we may be working with direct IP scan
                                new Handler(Looper.getMainLooper()).post(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        if (registerListener != null)
                                            registerListener.onDeviceRegistered(database, device, connection);
                                    }
                                });
                            }
                        }) != null;
                    }

                    if (!mConnected && !getInterrupter().interruptedByUser())
                        getSnackbarSupport().createSnackbar(R.string.mesg_connectionFailure)
                                .show();
                } catch (Exception e) {

                } finally {
                    new Handler(Looper.getMainLooper()).post(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (task != null)
                                task.updateTaskStopped();
                        }
                    });
                }
                // We can't add dialog outside of the else statement as it may close other dialogs as well
            }
        };

        if (task != null)
            task.updateTaskStarted(runningTask.getInterrupter());

        WorkerService.run(context, runningTask);
    }

    public boolean notifyShowHotspotHandled()
    {
        boolean returnedState = mShowHotspotInfo;

        mShowHotspotInfo = false;

        return returnedState;
    }

    public boolean notifyWirelessRequestHandled()
    {
        boolean returnedState = mWirelessEnableRequested;

        mWirelessEnableRequested = false;

        return returnedState;
    }

    public void showConnectionOptions(final FragmentActivity activity, final int locationPermRequestId, final RequestWatcher watcher)
    {
        if (!getConnectionUtils().getWifiManager().isWifiEnabled())
            getSnackbarSupport().createSnackbar(R.string.mesg_suggestSelfHotspot)
                    .setAction(R.string.butn_enable, new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            mWirelessEnableRequested = true;
                            turnOnWiFi(activity, locationPermRequestId, watcher);
                        }
                    })
                    .show();
        else if (validateLocationPermission(activity, locationPermRequestId, watcher)) {
            watcher.onResultReturned(true, false);

            getSnackbarSupport().createSnackbar(R.string.mesg_scanningSelfHotspot)
                    .setAction(R.string.butn_wifiSettings, new View.OnClickListener()
                    {
                        @Override
                        public void onClick(View view)
                        {
                            activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                        }
                    })
                    .show();
        }

        watcher.onResultReturned(true, false);
    }

    public boolean toggleHotspot(boolean conditional,
                                 final FragmentActivity activity,
                                 final int locationPermRequestId,
                                 final RequestWatcher watcher)
    {
        if (!HotspotUtils.isSupported())
            return false;

        DialogInterface.OnClickListener defaultNegativeListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                watcher.onResultReturned(false, false);
            }
        };

        if (conditional) {
            if (Build.VERSION.SDK_INT >= 26 && !validateLocationPermission(activity, locationPermRequestId, watcher))
                return false;

            if (Build.VERSION.SDK_INT >= 23
                    && !Settings.System.canWrite(getConnectionUtils().getContext())) {
                new AlertDialog.Builder(getConnectionUtils().getContext())
                        .setMessage(R.string.mesg_errorHotspotPermission)
                        .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                        .setPositiveButton(R.string.butn_settings, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                activity.startActivityForResult(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                        .setData(Uri.parse("package:" + getConnectionUtils().getContext().getPackageName()))
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), locationPermRequestId);

                                watcher.onResultReturned(false, true);
                            }
                        })
                        .show();

                return false;
            } else if (Build.VERSION.SDK_INT < 26
                    && !getConnectionUtils().getHotspotUtils().isEnabled()
                    && getConnectionUtils().isMobileDataActive()) {
                new AlertDialog.Builder(getConnectionUtils().getContext())
                        .setMessage(R.string.mesg_warningHotspotMobileActive)
                        .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                        .setPositiveButton(R.string.butn_skip, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // no need to call watcher due to recycle
                                toggleHotspot(false, activity, locationPermRequestId, watcher);
                            }
                        })
                        .show();

                return false;
            }
        }

        WifiConfiguration wifiConfiguration = getConnectionUtils().getHotspotUtils().getConfiguration();

        if (!getConnectionUtils().getHotspotUtils().isEnabled()
                || (wifiConfiguration != null && AppUtils.getHotspotName(getConnectionUtils().getContext()).equals(wifiConfiguration.SSID)))
            getSnackbarSupport().createSnackbar(getConnectionUtils().getHotspotUtils().isEnabled()
                    ? R.string.mesg_stoppingSelfHotspot
                    : R.string.mesg_startingSelfHotspot)
                    .show();

        AppUtils.startForegroundService(getConnectionUtils().getContext(), new Intent(getConnectionUtils().getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_TOGGLE_HOTSPOT));

        watcher.onResultReturned(true, false);

        mShowHotspotInfo = true;

        return true;
    }

    public boolean turnOnWiFi(final FragmentActivity activity, final int requestId, final RequestWatcher watcher)
    {
        if (getConnectionUtils().getWifiManager().setWifiEnabled(true)) {
            getSnackbarSupport().createSnackbar(R.string.mesg_turningWiFiOn).show();
            watcher.onResultReturned(true, false);
            return true;
        } else
            new AlertDialog.Builder(getConnectionUtils().getContext())
                    .setMessage(R.string.mesg_wifiEnableFailed)
                    .setNegativeButton(R.string.butn_close, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            watcher.onResultReturned(false, false);
                        }
                    })
                    .setPositiveButton(R.string.butn_settings, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            activity.startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), requestId);
                            watcher.onResultReturned(false, true);
                        }
                    })
                    .show();

        return false;
    }

    public boolean validateLocationPermission(final FragmentActivity activity, final int requestId, final RequestWatcher watcher)
    {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        final DialogInterface.OnClickListener defaultNegativeListener = new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which)
            {
                watcher.onResultReturned(false, false);
            }
        };

        if (!getConnectionUtils().hasLocationPermission(getConnectionUtils().getContext())) {
            new AlertDialog.Builder(getConnectionUtils().getContext())
                    .setMessage(R.string.mesg_locationPermissionRequiredSelfHotspot)
                    .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                    .setPositiveButton(R.string.butn_ask, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            watcher.onResultReturned(false, true);
                            activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION}, requestId);
                        }
                    })
                    .show();

            return false;
        }

        if (!getConnectionUtils().isLocationServiceEnabled()) {
            new AlertDialog.Builder(getConnectionUtils().getContext())
                    .setMessage(R.string.mesg_locationDisabledSelfHotspot)
                    .setNegativeButton(R.string.butn_cancel, defaultNegativeListener)
                    .setPositiveButton(R.string.butn_locationSettings, new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialog, int which)
                        {
                            watcher.onResultReturned(false, true);
                            activity.startActivityForResult(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), requestId);
                        }
                    })
                    .show();

            return false;
        }

        watcher.onResultReturned(true, false);

        return true;
    }

    public interface RequestWatcher {
        void onResultReturned(boolean result, boolean shouldWait);
    }
}
