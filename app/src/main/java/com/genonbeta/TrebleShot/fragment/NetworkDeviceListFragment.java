package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ConnectionManagerActivity;
import com.genonbeta.TrebleShot.dialog.EstablishConnectionDialog;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.callback.OnDeviceSelectedListener;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.callback.DetachListener;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.NsdDiscovery;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

import java.util.List;

public class NetworkDeviceListFragment
        extends EditableListFragment<NetworkDeviceListAdapter.EditableNetworkDevice, EditableListAdapter.EditableViewHolder, NetworkDeviceListAdapter>
        implements TitleSupport, DetachListener, IconSupport, ConnectionManagerActivity.DeviceSelectionSupport
{
    public static final int REQUEST_LOCATION_PERMISSION = 643;

    public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";

    private NsdDiscovery mNsdDiscovery;
    private NetworkDeviceSelectedListener mDeviceSelectedListener;
    private IntentFilter mIntentFilter = new IntentFilter();
    private StatusReceiver mStatusReceiver = new StatusReceiver();
    @Nullable
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private UIConnectionUtils mConnectionUtils;
    private boolean mWaitForWiFi = false;

    private UIConnectionUtils.RequestWatcher mWiFiWatcher = new UIConnectionUtils.RequestWatcher()
    {
        @Override
        public void onResultReturned(boolean result, boolean shouldWait)
        {
            mWaitForWiFi = shouldWait;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
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

        mNsdDiscovery = new NsdDiscovery(getContext(), AppUtils.getDatabase(getContext()), AppUtils.getDefaultPreferences(getContext()));
    }

    @Override
    protected RecyclerView onListView(ViewGroup container)
    {
        if (isHorizontalOrientation())
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

        useEmptyActionButton(getString(R.string.butn_scan), new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                requestRefresh();
            }
        });

        if (mSwipeRefreshLayout != null)
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
            {
                @Override
                public void onRefresh()
                {
                    requestRefresh();
                }
            });
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
        final AppUtils.QuickActions<EditableListAdapter.EditableViewHolder> quickActions = new AppUtils.QuickActions<EditableListAdapter.EditableViewHolder>()
        {
            @Override
            public void onQuickActions(final EditableListAdapter.EditableViewHolder clazz)
            {
                registerLayoutViewClicks(clazz);

                clazz.getView().findViewById(R.id.menu).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        openInfo(getAdapter().getList().get(clazz.getAdapterPosition()));

                        /*
                                                final PopupMenu popupMenu = new PopupMenu(getContext(), v);
                        final Menu menu = popupMenu.getMenu();
                        final NetworkDevice device = getAdapter().getList().get(clazz.getAdapterPosition());
                        final boolean isHotspotInstance = device instanceof NetworkDeviceListAdapter.HotspotNetwork;
                        final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = isHotspotInstance
                                ? (NetworkDeviceListAdapter.HotspotNetwork) device : null;
                        popupMenu.getMenuInflater().inflate(R.menu.action_mode_network_device, menu);

                        menu.findItem(R.id.action_mode_network_device_toggle_connection).setVisible(isHotspotInstance);
                        menu.findItem(R.id.action_mode_network_device_toggle_access).setVisible(!isHotspotInstance);
                        menu.findItem(R.id.action_mode_network_device_remove).setVisible(!isHotspotInstance);

                        if (isHotspotInstance) {
                            menu.findItem(R.id.action_mode_network_device_toggle_connection)
                                    .setTitle(getConnectionUtils().isConnectedToNetwork(hotspotNetwork) ? R.string.butn_disconnect : R.string.butn_connect);
                        } else {
                            menu.findItem(R.id.action_mode_network_device_toggle_access)
                                    .setTitle(device.isRestricted ? R.string.butn_allow : R.string.butn_block);
                        }

                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
                        {
                            @Override
                            public boolean onMenuItemClick(MenuItem item)
                            {
                                int id = item.getItemId();

                                if (id == R.id.action_mode_network_device_toggle_access) {
                                    device.isRestricted = !device.isRestricted;
                                    AppUtils.getDatabase(getContext()).update(device);
                                } else if (id == R.id.action_mode_network_device_remove) {
                                    new RemoveDeviceDialog(getActivity(), device)
                                            .show();
                                } else if (id == R.id.action_mode_network_device_toggle_connection
                                        && isHotspotInstance) {
                                    getConnectionUtils().toggleConnection(hotspotNetwork);
                                } else
                                    return false;

                                return true;
                            }
                        });

                        popupMenu.show();
                        */
                    }
                });
            }
        };

        return new NetworkDeviceListAdapter(getContext(), getConnectionUtils())
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
            if (device.versionNumber != -1 && device.versionNumber < AppConfig.SUPPORTED_MIN_VERSION)
                createSnackbar(R.string.mesg_versionNotSupported).show();
            else if (device instanceof NetworkDeviceListAdapter.HotspotNetwork)
                mConnectionUtils.makeAcquaintance(getActivity(),
                        null,
                        device,
                        -1,
                        new NetworkDeviceLoader.OnDeviceRegisteredErrorListener()
                        {
                            @Override
                            public void onError(Exception error)
                            {

                            }

                            @Override
                            public void onDeviceRegistered(AccessDatabase database, NetworkDevice device, NetworkDevice.Connection connection)
                            {
                                mDeviceSelectedListener.onNetworkDeviceSelected(device, connection);
                            }
                        }
                );
            else
                new EstablishConnectionDialog(getActivity(), device, new OnDeviceSelectedListener()
                {
                    @Override
                    public void onDeviceSelected(NetworkDevice.Connection connection, List<NetworkDevice.Connection> availableInterfaces)
                    {
                        mDeviceSelectedListener.onNetworkDeviceSelected(device, connection);
                    }
                }).show();

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
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        super.onCreateOptionsMenu(menu, inflater);

        if (!isHorizontalOrientation())
            inflater.inflate(R.menu.actions_network_device, menu);
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
            builder.setPositiveButton(getConnectionUtils().isConnectedToNetwork(hotspotNetwork) ? R.string.butn_disconnect : R.string.butn_connect, new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which)
                {
                    getConnectionUtils().toggleConnection(hotspotNetwork);
                }
            });

            builder.show();
        } else
            new DeviceInfoDialog(getActivity(), AppUtils.getDatabase(getContext()), device).show();
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
            } else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())
                    || (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
                    && AccessDatabase.TABLE_DEVICES.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME))
            ))
                refreshList();
            else if (getUIConnectionUtils().notifyWirelessRequestHandled()
                    && WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
                    && WifiManager.WIFI_STATE_ENABLED == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)) {
                requestRefresh();
            }
        }
    }
}
