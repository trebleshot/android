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

package com.genonbeta.TrebleShot.fragment;

import android.app.Activity;
import android.content.*;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDeviceActivity;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.InfoHolder;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.Service;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.dialog.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NsdDiscovery;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.List;

import static com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.*;

public class DeviceListFragment extends EditableListFragment<InfoHolder,
        RecyclerViewAdapter.ViewHolder, NetworkDeviceListAdapter> implements IconProvider
{
    public static final int REQUEST_LOCATION_PERMISSION = 643;

    public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";
    public static final String ARG_HIDDEN_DEVICES_LIST = "hiddenDeviceList";

    private NsdDiscovery mNsdDiscovery;
    private IntentFilter mIntentFilter = new IntentFilter();
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ConnectionUtils mConnectionUtils;
    private Device.Type[] mHiddenDeviceTypes;
    private boolean mWaitForWiFi = false;
    private boolean mSwipeRefreshEnabled = true;
    private boolean mDeviceScanAllowed = true;
    private DeviceScannerService mService;

    private ServiceConnection mScannerConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            mService = ((DeviceScannerService.Binder) service).getService();
            checkRefreshing();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            mService = null;
        }
    };

    public static void openInfo(Activity activity, ConnectionUtils utils, InfoHolder infoHolder)
    {
        Object specifier = infoHolder.object();
        if (specifier instanceof WifiConfiguration) {
            WifiConfiguration config = (WifiConfiguration) specifier;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle(infoHolder.name())
                    .setMessage(R.string.text_trebleshotHotspotDescription)
                    .setNegativeButton(R.string.butn_close, null);

            if (Build.VERSION.SDK_INT < 29)
                builder.setPositiveButton(utils.isConnectedToNetwork(config) ? R.string.butn_disconnect
                        : R.string.butn_connect, (dialog, which) -> utils.toggleConnection(config));

            builder.show();
        } else if (specifier instanceof Device)
            new DeviceInfoDialog(activity, AppUtils.getKuick(activity), (Device) specifier).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        if (!isHorizontalOrientation() && mSwipeRefreshEnabled)
            setLayoutResId(R.layout.layout_network_device);

        setFilteringSupported(true);
        setSortingSupported(false);
        setUseDefaultPaddingDecoration(true);
        setUseDefaultPaddingDecorationSpaceForEdges(true);
        setDefaultPaddingDecorationSize(getResources().getDimension(R.dimen.padding_list_content_parent_layout));

        mIntentFilter.addAction(DeviceScannerService.ACTION_SCAN_STARTED);
        mIntentFilter.addAction(DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED);
        mIntentFilter.addAction(Kuick.ACTION_DATABASE_CHANGE);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        mNsdDiscovery = new NsdDiscovery(getContext(), AppUtils.getKuick(getContext()),
                AppUtils.getDefaultPreferences(getContext()));

        if (getArguments() != null) {
            Bundle args = getArguments();

            if (args.containsKey(ARG_HIDDEN_DEVICES_LIST)) {
                List<String> hiddenTypes = args.getStringArrayList(ARG_HIDDEN_DEVICES_LIST);

                if (hiddenTypes != null && hiddenTypes.size() > 0) {
                    mHiddenDeviceTypes = new Device.Type[hiddenTypes.size()];

                    for (int i = 0; i < hiddenTypes.size(); i++) {
                        Device.Type type = Device.Type.valueOf(hiddenTypes.get(i));
                        mHiddenDeviceTypes[i] = type;

                        if (mDeviceScanAllowed && Device.Type.NORMAL.equals(type))
                            mDeviceScanAllowed = false;
                    }
                }
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new NetworkDeviceListAdapter(this, this, getConnectionUtils(),
                mHiddenDeviceTypes));
        setEmptyListImage(R.drawable.ic_devices_white_24dp);
        setEmptyListText(getString(R.string.text_findDevicesHint));
        useEmptyListActionButton(getString(R.string.butn_scan), v -> requestRefresh());

        mSwipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);

        if (mSwipeRefreshLayout != null) {
            Context context = requireContext();

            mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(context,
                    AppUtils.getReference(context, R.attr.colorAccent)));
            mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(ContextCompat.getColor(context,
                    AppUtils.getReference(context, R.attr.colorSurface)));
            mSwipeRefreshLayout.setOnRefreshListener(this::requestRefresh);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);

        if (AppUtils.getDefaultPreferences(getContext()).getBoolean("scan_devices_auto", false))
            requestRefresh();
    }

    @Override
    public boolean onDefaultClickAction(RecyclerViewAdapter.ViewHolder holder)
    {
        try {
            InfoHolder infoHolder = getAdapter().getItem(holder.getAdapterPosition());
            Object specifier = infoHolder.object();
            if (requireActivity() instanceof AddDeviceActivity) {
                if (specifier instanceof NetworkDescription)
                    BackgroundService.run(requireActivity(), new DeviceIntroductionTask(
                            (NetworkDescription) specifier, -1));
                else if (specifier instanceof Device) {
                    Device device = (Device) specifier;
                    if (device.versionCode < AppConfig.SUPPORTED_MIN_VERSION)
                        createSnackbar(R.string.mesg_versionNotSupported).show();
                    else
                        new EstablishConnectionDialog(getActivity(), device, (connection, availableInterfaces) ->
                                AddDeviceActivity.returnResult(requireActivity(), device, connection)).show();
                }
            } else
                openInfo(getActivity(), getConnectionUtils(), infoHolder);
        } catch (NotReadyException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        requireActivity().registerReceiver(mStatusReceiver, mIntentFilter);
        requireActivity().bindService(new Intent(getContext(), DeviceScannerService.class), mScannerConnection,
                Service.BIND_AUTO_CREATE);

        mNsdDiscovery.startDiscovering();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireActivity().unregisterReceiver(mStatusReceiver);
        requireActivity().unbindService(mScannerConnection);

        mNsdDiscovery.stopDiscovering();

    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);

        if (!isHorizontalOrientation()) {
            inflater.inflate(R.menu.actions_network_device, menu);
            menu.findItem(R.id.network_devices_scan).setVisible(mDeviceScanAllowed);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        if (R.id.network_devices_scan == item.getItemId())
            requestRefresh();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQUEST_LOCATION_PERMISSION == requestCode)
            getConnectionUtils().showConnectionOptions(getActivity(), this, REQUEST_LOCATION_PERMISSION);
    }

    public void checkRefreshing()
    {
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(mService != null && mService.getDeviceScanner().isBusy());
    }

    public ConnectionUtils getConnectionUtils()
    {
        if  (mConnectionUtils == null)
            mConnectionUtils = new ConnectionUtils(requireContext());
        return mConnectionUtils;
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_devices_white_24dp;
    }

    @Override
    public CharSequence getDistinctiveTitle(Context context)
    {
        return context.getString(R.string.text_useKnownDevice);
    }

    @Override
    public boolean isHorizontalOrientation()
    {
        return (getArguments() != null && getArguments().getBoolean(ARG_USE_HORIZONTAL_VIEW))
                || super.isHorizontalOrientation();
    }

    public void setHiddenDeviceTypes(Device.Type[] types)
    {
        mHiddenDeviceTypes = types;
    }

    public void setSwipeRefreshEnabled(boolean enabled)
    {
        mSwipeRefreshEnabled = enabled;
    }

    public void setDeviceScanAllowed(boolean allow)
    {
        mDeviceScanAllowed = allow;
    }

    public void requestRefresh()
    {
        if (Build.VERSION.SDK_INT < 29)
            getConnectionUtils().getWifiManager().startScan();

        if (!AppUtils.toggleDeviceScanning(mService))
            Toast.makeText(getContext(), R.string.mesg_stopping, Toast.LENGTH_SHORT).show();
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            checkRefreshing();

            if (DeviceScannerService.ACTION_SCAN_STARTED.equals(intent.getAction())
                    && intent.hasExtra(DeviceScannerService.EXTRA_SCAN_STATUS)) {
                String scanStatus = intent.getStringExtra(DeviceScannerService.EXTRA_SCAN_STATUS);

                if (DeviceScannerService.STATUS_OK.equals(scanStatus)
                        || DeviceScannerService.SCANNER_NOT_AVAILABLE.equals(scanStatus)) {
                    boolean selfNetwork = getConnectionUtils().isConnectionToHotspotNetwork();

                    if (!selfNetwork)
                        createSnackbar(DeviceScannerService.STATUS_OK.equals(scanStatus) ? R.string.mesg_scanningDevices
                                : R.string.mesg_stillScanning).show();
                    else
                        createSnackbar(R.string.mesg_scanningDevicesSelfHotspot)
                                .setAction(R.string.butn_disconnect, v -> getConnectionUtils().getWifiManager().disconnect())
                                .show();
                } else if (DeviceScannerService.STATUS_NO_NETWORK_INTERFACE.equals(scanStatus))
                    getConnectionUtils().showConnectionOptions(getActivity(), DeviceListFragment.this,
                            REQUEST_LOCATION_PERMISSION);
            } else if (DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED.equals(intent.getAction())) {
                createSnackbar(R.string.mesg_scanCompleted)
                        .show();
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction()))
                refreshList();
            if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_DEVICES.equals(data.tableName))
                    refreshList();
            } else if (getConnectionUtils().notifyWirelessRequestHandled()
                    && WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    && WifiManager.WIFI_STATE_ENABLED == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE,
                    -1)) {
                mService.getDeviceScanner().run();
                requestRefresh();
            }
        }
    }
}
