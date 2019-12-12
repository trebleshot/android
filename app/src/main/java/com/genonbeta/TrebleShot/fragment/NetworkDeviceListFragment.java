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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDeviceActivity;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.dialog.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.callback.DetachListener;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader.OnDeviceRegisteredErrorListener;
import com.genonbeta.TrebleShot.util.NsdDiscovery;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

import java.util.List;

public class NetworkDeviceListFragment
        extends EditableListFragment<NetworkDeviceListAdapter.EditableNetworkDevice, EditableListAdapter.EditableViewHolder, NetworkDeviceListAdapter>
        implements TitleSupport, DetachListener, IconSupport, AddDeviceActivity.DeviceSelectionSupport
{
    public static final int REQUEST_LOCATION_PERMISSION = 643;

    public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";
    public static final String ARG_HIDDEN_DEVICES_LIST = "hiddenDeviceList";

    private NsdDiscovery mNsdDiscovery;
    private NetworkDeviceSelectedListener mDeviceSelectedListener;
    private IntentFilter mIntentFilter = new IntentFilter();
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    @Nullable
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private UIConnectionUtils mConnectionUtils;
    private NetworkDevice.Type[] mHiddenDeviceTypes;
    private boolean mWaitForWiFi = false;
    private boolean mSwipeRefreshEnabled = true;
    private boolean mDeviceScanAllowed = true;

    OnDeviceRegisteredErrorListener mDeviceErrorListener = new OnDeviceRegisteredErrorListener()
    {
        @Override
        public void onError(Exception error)
        {
        }

        @Override
        public void onDeviceRegistered(AccessDatabase database, NetworkDevice device, DeviceConnection connection)
        {
            mDeviceSelectedListener.onNetworkDeviceSelected(device, connection);
        }
    };

    private UIConnectionUtils.RequestWatcher mWiFiWatcher = (result, shouldWait) -> mWaitForWiFi = shouldWait;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        if (getArguments() != null) {
            Bundle args = getArguments();

            if (args.containsKey(ARG_HIDDEN_DEVICES_LIST)) {
                List<String> hiddenTypes = args.getStringArrayList(ARG_HIDDEN_DEVICES_LIST);

                if (hiddenTypes != null && hiddenTypes.size() > 0) {
                    mHiddenDeviceTypes = new NetworkDevice.Type[hiddenTypes.size()];

                    for (int i = 0; i < hiddenTypes.size(); i++) {
                        NetworkDevice.Type type = NetworkDevice.Type.valueOf(hiddenTypes.get(i));
                        mHiddenDeviceTypes[i] = type;

                        if (mDeviceScanAllowed && NetworkDevice.Type.NORMAL.equals(type))
                            mDeviceScanAllowed = false;
                    }
                }
            }
        }

        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setSortingSupported(false);
        setUseDefaultPaddingDecoration(true);
        setUseDefaultPaddingDecorationSpaceForEdges(true);
        setDefaultPaddingDecorationSize(getResources().getDimension(R.dimen.padding_list_content_parent_layout));

        mIntentFilter.addAction(DeviceScannerService.ACTION_SCAN_STARTED);
        mIntentFilter.addAction(DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED);
        mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        mNsdDiscovery = new NsdDiscovery(getContext(), AppUtils.getDatabase(getContext()),
                AppUtils.getDefaultPreferences(getContext()));
    }

    @Override
    protected RecyclerView onListView(ViewGroup container)
    {
        if (isHorizontalOrientation() || !mSwipeRefreshEnabled)
            return super.onListView(container);

        Context context = container.getContext();

        mSwipeRefreshLayout = new SwipeRefreshLayout(getActivity());

        mSwipeRefreshLayout.setColorSchemeColors(ContextCompat
                .getColor(context, AppUtils.getReference(getActivity(), R.attr.colorAccent)));

        mSwipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        mSwipeRefreshLayout.setProgressBackgroundColorSchemeColor(
                ContextCompat.getColor(context, AppUtils.getReference(context, R.attr.colorSurface))
        );

        container.addView(mSwipeRefreshLayout);

        return super.onListView(mSwipeRefreshLayout);
    }

    @Override
    public void onViewCreated(@NonNull View view, final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setEmptyImage(R.drawable.ic_devices_white_24dp);
        setEmptyText(getString(R.string.text_findDevicesHint));

        useEmptyActionButton(getString(R.string.butn_scan), v -> requestRefresh());

        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setOnRefreshListener(this::requestRefresh);
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
    public NetworkDeviceListAdapter onAdapter()
    {
        final AppUtils.QuickActions<EditableListAdapter.EditableViewHolder> quickActions = clazz -> {
            registerLayoutViewClicks(clazz);

            clazz.getView().findViewById(R.id.menu).setOnClickListener(v -> {
                openInfo(getAdapter().getList().get(clazz.getAdapterPosition()));
            });
        };

        return new NetworkDeviceListAdapter(getContext(), getConnectionUtils(), mHiddenDeviceTypes)
        {
            @NonNull
            @Override
            public EditableListAdapter.EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
            {
                return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
            }
        };
    }

    @Override
    public boolean onDefaultClickAction(EditableListAdapter.EditableViewHolder holder)
    {
        final NetworkDevice device = getAdapter().getList().get(holder.getAdapterPosition());

        if (mDeviceSelectedListener != null && mDeviceSelectedListener.isListenerEffective()) {
            if (device.versionCode != -1 && device.versionCode < AppConfig.SUPPORTED_MIN_VERSION)
                createSnackbar(R.string.mesg_versionNotSupported).show();
            else if (device instanceof NetworkDeviceListAdapter.HotspotNetwork)
                mConnectionUtils.makeAcquaintance(getActivity(), null, device, -1, mDeviceErrorListener);
            else
                new EstablishConnectionDialog(getActivity(), device,
                        (connection, availableInterfaces) -> mDeviceSelectedListener.onNetworkDeviceSelected(device,
                                connection)).show();

        } else {
            openInfo(device);
        }

        return true;
    }

    @Override
    public void onResume()
    {
        super.onResume();
        getActivity().registerReceiver(mStatusReceiver, mIntentFilter);

        refreshList();
        checkRefreshing();

        mNsdDiscovery.startDiscovering();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        getActivity().unregisterReceiver(mStatusReceiver);

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
            getUIConnectionUtils().showConnectionOptions(getActivity(), REQUEST_LOCATION_PERMISSION, mWiFiWatcher);
    }

    @Override
    public void onPrepareDetach()
    {
        showCustomView(false);
    }

    public void checkRefreshing()
    {
        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setRefreshing(!DeviceScannerService.getDeviceScanner()
                    .isScannerAvailable());
    }

    public ConnectionUtils getConnectionUtils()
    {
        return getUIConnectionUtils().getConnectionUtils();
    }

    @Override
    public int getIconRes()
    {
        return R.drawable.ic_devices_white_24dp;
    }

    public UIConnectionUtils getUIConnectionUtils()
    {
        if (mConnectionUtils == null)
            mConnectionUtils = new UIConnectionUtils(ConnectionUtils.getInstance(getContext()), this);

        return mConnectionUtils;
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_useKnownDevice);
    }

    @Override
    public boolean isHorizontalOrientation()
    {
        return (getArguments() != null && getArguments().getBoolean(ARG_USE_HORIZONTAL_VIEW))
                || super.isHorizontalOrientation();
    }

    private void openInfo(NetworkDevice device)
    {
        if (device instanceof NetworkDeviceListAdapter.HotspotNetwork) {
            final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = (NetworkDeviceListAdapter.HotspotNetwork) device;

            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

            builder.setTitle(hotspotNetwork.nickname);
            builder.setMessage(R.string.text_trebleshotHotspotDescription);
            builder.setNegativeButton(R.string.butn_close, null);
            builder.setPositiveButton(getConnectionUtils().isConnectedToNetwork(hotspotNetwork) ? R.string.butn_disconnect : R.string.butn_connect, (dialog, which) -> getConnectionUtils().toggleConnection(hotspotNetwork));

            builder.show();
        } else
            new DeviceInfoDialog(getActivity(), AppUtils.getDatabase(getContext()), device).show();
    }

    public void setHiddenDeviceTypes(NetworkDevice.Type[] types)
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
        getConnectionUtils().getWifiManager().startScan();

        if (!AppUtils.toggleDeviceScanning(getContext()))
            Toast.makeText(getContext(), R.string.mesg_stopping, Toast.LENGTH_SHORT).show();
    }

    public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
    {
        mDeviceSelectedListener = listener;
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            checkRefreshing();

            if (DeviceScannerService.ACTION_SCAN_STARTED.equals(intent.getAction()) && intent.hasExtra(DeviceScannerService.EXTRA_SCAN_STATUS)) {
                String scanStatus = intent.getStringExtra(DeviceScannerService.EXTRA_SCAN_STATUS);

                if (DeviceScannerService.STATUS_OK.equals(scanStatus) || DeviceScannerService.SCANNER_NOT_AVAILABLE.equals(scanStatus)) {
                    boolean selfNetwork = getConnectionUtils().isConnectionSelfNetwork();

                    if (!selfNetwork)
                        createSnackbar(DeviceScannerService.STATUS_OK.equals(scanStatus) ?
                                R.string.mesg_scanningDevices
                                : R.string.mesg_stillScanning)
                                .show();
                    else
                        createSnackbar(R.string.mesg_scanningDevicesSelfHotspot)
                                .setAction(R.string.butn_disconnect, new View.OnClickListener()
                                {
                                    @Override
                                    public void onClick(View v)
                                    {
                                        getConnectionUtils().getWifiManager().disconnect();
                                    }
                                })
                                .show();
                } else if (DeviceScannerService.STATUS_NO_NETWORK_INTERFACE.equals(scanStatus))
                    getUIConnectionUtils().showConnectionOptions(getActivity(), REQUEST_LOCATION_PERMISSION, mWiFiWatcher);
            } else if (DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED.equals(intent.getAction())) {
                createSnackbar(R.string.mesg_scanCompleted)
                        .show();
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction()))
                refreshList();
            if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                AccessDatabase.BroadcastData data = AccessDatabase.toData(intent);
                if (AccessDatabase.TABLE_DEVICES.equals(data.tableName))
                    refreshList();
            } else if (getUIConnectionUtils().notifyWirelessRequestHandled()
                    && WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    && WifiManager.WIFI_STATE_ENABLED == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)) {
                requestRefresh();
            }
        }
    }
}
