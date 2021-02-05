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
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter.DbVirtualDevice;
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter.DescriptionVirtualDevice;
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter.VirtualDevice;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.dialog.FindConnectionDialog;
import com.genonbeta.TrebleShot.dataobject.Device;
import com.genonbeta.TrebleShot.dataobject.DeviceAddress;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;
import com.genonbeta.TrebleShot.ui.callback.IconProvider;
import com.genonbeta.TrebleShot.util.Connections;
import com.genonbeta.TrebleShot.util.DeviceLoader;
import com.genonbeta.TrebleShot.util.NsdDaemon;
import com.genonbeta.TrebleShot.util.P2pDaemon;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.List;

import static com.genonbeta.TrebleShot.adapter.DeviceListAdapter.NetworkDescription;

public class DeviceListFragment extends EditableListFragment<VirtualDevice, RecyclerViewAdapter.ViewHolder,
        DeviceListAdapter> implements IconProvider, DeviceLoader.OnDeviceResolvedListener
{
    public static final int REQUEST_LOCATION_PERMISSION = 643;

    public static final String ARG_USE_HORIZONTAL_VIEW = "useHorizontalView";
    public static final String ARG_HIDDEN_DEVICES_LIST = "hiddenDeviceList";

    private final IntentFilter mIntentFilter = new IntentFilter();
    private final StatusReceiver mStatusReceiver = new StatusReceiver();
    private Connections mConnections;
    private Device.Type[] mHiddenDeviceTypes;
    private P2pDaemon mP2pDaemon;

    public static void openInfo(Activity activity, Connections utils, VirtualDevice virtualDevice)
    {
        if (virtualDevice instanceof DescriptionVirtualDevice) {
            NetworkDescription description = ((DescriptionVirtualDevice) virtualDevice).description;
            AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                    .setTitle(virtualDevice.name())
                    .setMessage(R.string.text_trebleshotHotspotDescription)
                    .setNegativeButton(R.string.butn_close, null);

            if (Build.VERSION.SDK_INT < 29)
                builder.setPositiveButton(utils.isConnectedToNetwork(description) ? R.string.butn_disconnect
                        : R.string.butn_connect, (dialog, which) -> App.from(activity).run(
                        new DeviceIntroductionTask(description, 0)));

            builder.show();
        } else if (virtualDevice instanceof DbVirtualDevice)
            new DeviceInfoDialog(activity, ((DbVirtualDevice) virtualDevice).device).show();
    }

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setFilteringSupported(true);
        setSortingSupported(false);
        setItemOffsetDecorationEnabled(true);
        setItemOffsetForEdgesEnabled(true);
        setDefaultItemOffsetPadding(getResources().getDimension(R.dimen.padding_list_content_parent_layout));

        mIntentFilter.addAction(Kuick.ACTION_DATABASE_CHANGE);
        mIntentFilter.addAction(NsdDaemon.ACTION_DEVICE_STATUS);
        mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

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

        // TODO: 2/1/21 Wifi Direct daemon? Might not be supported by Android TV.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon = new P2pDaemon(getConnections());
    }

    @Override
    public void onViewCreated(@NonNull View view, final Bundle savedInstanceState)
    {
        super.onViewCreated(view, savedInstanceState);

        setListAdapter(new DeviceListAdapter(this, getConnections(),
                App.from(requireActivity()).getNsdDaemon(), mHiddenDeviceTypes));
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
        // TODO: 2/1/21 Fix regression issue of Wifi Direct.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon.start(requireContext());
    }

    @Override
    public void onPause()
    {
        super.onPause();
        requireActivity().unregisterReceiver(mStatusReceiver);
        // TODO: 2/1/21 Enable stop implementation of the Wifi Direct daemon after fixing the regression issue.
        //if (Build.VERSION.SDK_INT >= 16)
        //    mP2pDaemon.stop(requireContext());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (REQUEST_LOCATION_PERMISSION == requestCode)
            getConnections().showConnectionOptions(getActivity(), this, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onDeviceResolved(Device device, DeviceAddress address)
    {
        AddDeviceActivity.returnResult(requireActivity(), device, address);
    }

    public Connections getConnections()
    {
        if (mConnections == null)
            mConnections = new Connections(requireContext());
        return mConnections;
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
    public boolean performDefaultLayoutClick(RecyclerViewAdapter.ViewHolder holder, VirtualDevice object)
    {
        if (requireActivity() instanceof AddDeviceActivity) {
            if (object instanceof DescriptionVirtualDevice)
                App.run(requireActivity(), new DeviceIntroductionTask(((DescriptionVirtualDevice) object).description,
                        0));
            else if (object instanceof DbVirtualDevice) {
                Device device = ((DbVirtualDevice) object).device;
                if (BuildConfig.PROTOCOL_VERSION_MIN > device.protocolVersionMin)
                    createSnackbar(R.string.mesg_versionNotSupported).show();
                else
                    FindConnectionDialog.show(getActivity(), device, this);
            } else
                return false;
        } else
            openInfo(getActivity(), getConnections(), object);

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
