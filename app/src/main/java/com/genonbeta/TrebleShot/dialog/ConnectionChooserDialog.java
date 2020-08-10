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

package com.genonbeta.TrebleShot.dialog;

import android.app.Activity;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import androidx.annotation.ColorInt;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ManageDevicesActivity;
import com.genonbeta.TrebleShot.callback.OnConnectionSelectionListener;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.android.database.SQLQuery;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by: veli
 * Date: 5/19/17 12:18 AM
 */

public class ConnectionChooserDialog extends AlertDialog.Builder
{
    private final List<DeviceAddress> mConnections = new ArrayList<>();
    private final List<NetworkInterface> mNetworkInterfaces = new ArrayList<>();

    private Device mDevice;

    @ColorInt
    private int mActiveColor;

    @ColorInt
    private int mPassiveColor;

    public ConnectionChooserDialog(final Activity activity, Device device, OnConnectionSelectionListener listener)
    {
        super(activity);

        mDevice = device;
        mActiveColor = ContextCompat.getColor(activity, AppUtils.getReference(activity, R.attr.colorAccent));
        mPassiveColor = ContextCompat.getColor(activity, AppUtils.getReference(activity, R.attr.colorControlNormal));

        ConnectionListAdapter adapter = new ConnectionListAdapter();

        if (mConnections.size() > 0)
            setAdapter(adapter, (dialog, which) -> listener.onConnectionSelection(mConnections.get(which)));
        else
            setMessage(R.string.text_noNetworkAvailable);

        setTitle(getContext().getString(R.string.text_availableNetworks, device.username));
        setNegativeButton(R.string.butn_cancel, null);
        setNeutralButton(R.string.text_manageDevices, (dialog, which) -> activity.startActivity(new Intent(activity,
                ManageDevicesActivity.class)));
    }

    private class ConnectionListAdapter extends BaseAdapter
    {
        public ConnectionListAdapter()
        {
            mConnections.addAll(AppUtils.getKuick(getContext()).castQuery(
                    new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                            .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?", mDevice.uid)
                            .setOrderBy(Kuick.FIELD_DEVICECONNECTION_LASTCHECKEDDATE + " DESC"),
                    DeviceAddress.class));

            mNetworkInterfaces.addAll(NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES));
        }

        @Override
        public int getCount()
        {
            return mConnections.size();
        }

        @Override
        public Object getItem(int position)
        {
            return mConnections.get(position);
        }

        @Override
        public long getItemId(int position)
        {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            if (convertView == null)
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_available_interface, parent,
                        false);

            DeviceAddress address = (DeviceAddress) getItem(position);

            TextView textView1 = convertView.findViewById(R.id.pending_available_interface_text1);
            TextView textView2 = convertView.findViewById(R.id.pending_available_interface_text2);
            TextView textView3 = convertView.findViewById(R.id.pending_available_interface_text3);

            boolean accessible = false;

            for (NetworkInterface networkInterface : mNetworkInterfaces)
                if (address.adapterName.equals(networkInterface.getDisplayName())) {
                    accessible = true;
                    break;
                }

            textView1.setTextColor(accessible ? mActiveColor : mPassiveColor);
            textView1.setText(TextUtils.getAdapterName(getContext(), address));
            textView2.setText(address.ipAddress);
            textView3.setText(TimeUtils.getTimeAgo(getContext(), address.lastCheckedDate));

            return convertView;
        }
    }
}
