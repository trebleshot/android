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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.BuildConfig;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.AddDeviceActivity;
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter;
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter.InfoHolder;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.dialog.FindConnectionDialog;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.DeviceLoader;
import com.genonbeta.TrebleShot.util.NsdDaemon;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.List;

import static com.genonbeta.TrebleShot.adapter.DeviceListAdapter.NetworkDescription;

public class DeviceListFragment extends EditableListFragment<InfoHolder, RecyclerViewAdapter.ViewHolder,
        DeviceListAdapter> implements IconProvider, DeviceLoader.OnDeviceResolvedListener
{
    public static final int REQUEST_LOCATION_PERMISSION = 643;

    public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";
    public static final String ARG_HIDDEN_DEVICES_LIST = "hiddenDeviceList";

    private NsdDaemon mNsdDaemon;
    private final IntentFilter mIntentFilter = new IntentFilter();
    private final StatusReceiver mStatusReceiver = new StatusReceiver();
    private ConnectionUtils mConnectionUtils;
    private Device.Type[] mHiddenDeviceTypes;

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
            new DeviceInfoDialog(activity, (Device) specifier).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setSortingSupported(false);
        setUseDefaultPaddingDecoration(true);
        setUseDefaultPaddingDecorationSpaceForEdges(true);
        setDefaultPaddingDecorationSize(getResources().getDimension(R.dimen.padding_list_content_parent_layout));

        mIntentFilter.addAction(Kuick.ACTION_DATABASE_CHANGE);
        mIntentFilter.addAction(NsdDaemon.ACTION_DEVICE_STATUS);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

        mNsdDaemon = new NsdDaemon(getContext(), AppUtils.getKuick(getContext()),
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
                    }
                }
            }
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new DeviceListAdapter(this, getConnectionUtils(), mNsdDaemon, mHiddenDeviceTypes));
        setEmptyListImage(R.drawable.ic_devices_white_24dp);
        setEmptyListText(getString(R.string.text_findDevicesHint));
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        requireActivity().registerReceiver(mStatusReceiver, mIntentFilter);
        mNsdDaemon.startDiscovering();
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireActivity().unregisterReceiver(mStatusReceiver);
        mNsdDaemon.stopDiscovering();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQUEST_LOCATION_PERMISSION == requestCode)
            getConnectionUtils().showConnectionOptions(getActivity(), this, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onDeviceResolved(Device device, DeviceAddress address)
    {
        AddDeviceActivity.handleResult(requireActivity(), device, address);
    }

    public ConnectionUtils getConnectionUtils()
    {
        if (mConnectionUtils == null)
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
        return context.getString(R.string.text_allDevices);
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

    @Override
    public boolean performDefaultLayoutClick(RecyclerViewAdapter.ViewHolder holder, InfoHolder object)
    {
        Object specifier = object.object();
        if (requireActivity() instanceof AddDeviceActivity) {
            if (specifier instanceof NetworkDescription)
                App.run(requireActivity(), new DeviceIntroductionTask((NetworkDescription) specifier, -1));
            else if (specifier instanceof Device) {
                Device device = (Device) specifier;
                if (BuildConfig.PROTOCOL_VERSION_MIN > device.protocolVersionMin)
                    createSnackbar(R.string.mesg_versionNotSupported).show();
                else
                    FindConnectionDialog.show(getActivity(), device, this);
            } else
                return false;
        } else
            openInfo(getActivity(), getConnectionUtils(), object);

        return true;
    }

    private class StatusReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())
                    || NsdDaemon.ACTION_DEVICE_STATUS.equals(intent.getAction()))
                refreshList();
            else if (Kuick.ACTION_DATABASE_CHANGE.equals(intent.getAction())) {
                Kuick.BroadcastData data = Kuick.toData(intent);
                if (Kuick.TABLE_DEVICES.equals(data.tableName))
                    refreshList();
            }
        }
    }
}
