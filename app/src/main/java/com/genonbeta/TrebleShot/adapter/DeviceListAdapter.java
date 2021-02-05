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

package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.net.MacAddress;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiNetworkSuggestion;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.util.ObjectsCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.IEditableListFragment;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.dataobject.Device;
import com.genonbeta.TrebleShot.dataobject.Editable;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.Connections;
import com.genonbeta.TrebleShot.util.DeviceLoader;
import com.genonbeta.TrebleShot.util.NsdDaemon;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.genonbeta.TrebleShot.fragment.DeviceListFragment.openInfo;

public class DeviceListAdapter extends EditableListAdapter<DeviceListAdapter.VirtualDevice, RecyclerViewAdapter.ViewHolder>
{
    private final Connections mConnections;
    private final TextDrawable.IShapeBuilder mIconBuilder;
    private final List<Device.Type> mHiddenDeviceTypes;
    private final NsdDaemon mNsdDaemon;

    public DeviceListAdapter(IEditableListFragment<VirtualDevice, ViewHolder> fragment, Connections utils,
                             NsdDaemon nsdDaemon, Device.Type[] hiddenDeviceTypes)
    {
        super(fragment);
        mConnections = utils;
        mIconBuilder = AppUtils.getDefaultIconBuilder(getContext());
        mNsdDaemon = nsdDaemon;
        mHiddenDeviceTypes = hiddenDeviceTypes != null ? Arrays.asList(hiddenDeviceTypes) : new ArrayList<>();
    }

    @Override
    public List<VirtualDevice> onLoad()
    {
        boolean devMode = AppUtils.getDefaultPreferences(getContext()).getBoolean("developer_mode", false);
        List<VirtualDevice> list = new ArrayList<>();

        if (mConnections.canReadScanResults()) {
            for (ScanResult result : mConnections.getWifiManager().getScanResults()) {
                if ((result.capabilities == null || result.capabilities.equals("[ESS]"))
                        && Connections.isClientNetwork(result.SSID))
                    list.add(new DescriptionVirtualDevice(new NetworkDescription(result)));
            }
        }

        for (Device device : AppUtils.getKuick(getContext()).castQuery(new SQLQuery.Select(Kuick.TABLE_DEVICES)
                .setOrderBy(Kuick.FIELD_DEVICES_LASTUSAGETIME + " DESC"), Device.class)) {
            if (mNsdDaemon.isDeviceOnline(device))
                device.type = Device.Type.NORMAL_ONLINE;
            else if (Device.Type.NORMAL_ONLINE.equals(device.type))
                device.type = Device.Type.NORMAL;

            if ((!mHiddenDeviceTypes.contains(device.type)) && (!device.isLocal || devMode))
                list.add(new DbVirtualDevice(device));
        }

        List<VirtualDevice> filteredList = new ArrayList<>();
        for (VirtualDevice virtualDevice : list)
            if (filterItem(virtualDevice))
                filteredList.add(virtualDevice);

        return filteredList;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        ViewHolder holder = new RecyclerViewAdapter.ViewHolder(getInflater().inflate(
                isHorizontalOrientation() || isGridLayoutRequested() ? R.layout.list_network_device_grid
                        : R.layout.list_network_device, parent, false));

        getFragment().registerLayoutViewClicks(holder);
        holder.itemView.findViewById(R.id.menu)
                .setOnClickListener(v -> openInfo(getFragment().getActivity(), mConnections,
                        getList().get(holder.getAdapterPosition())));

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position)
    {
        VirtualDevice virtualDevice = getItem(position);
        View parentView = holder.itemView;

        TextView text1 = parentView.findViewById(R.id.text1);
        TextView text2 = parentView.findViewById(R.id.text2);
        ImageView image = parentView.findViewById(R.id.image);
        ImageView statusImage = parentView.findViewById(R.id.imageStatus);
        View layoutOnline = parentView.findViewById(R.id.layout_online);

        text1.setText(virtualDevice.name());
        text2.setText(virtualDevice.description(getContext()));
        layoutOnline.setVisibility(virtualDevice.isOnline() ? View.VISIBLE : View.GONE);
        boolean isRestricted = false;
        boolean isTrusted = false;

        if (virtualDevice instanceof DbVirtualDevice) {
            Device device = ((DbVirtualDevice) virtualDevice).device;
            isRestricted = device.isBlocked;
            isTrusted = device.isTrusted;

            DeviceLoader.showPictureIntoView(device, image, mIconBuilder);
        } else
            image.setImageDrawable(mIconBuilder.buildRound(virtualDevice.name()));

        if (isRestricted) {
            statusImage.setVisibility(View.VISIBLE);
            statusImage.setImageResource(R.drawable.ic_block_white_24dp);
        } else if (isTrusted) {
            statusImage.setVisibility(View.VISIBLE);
            statusImage.setImageResource(R.drawable.ic_vpn_key_white_24dp);
        } else {
            statusImage.setVisibility(View.GONE);
        }
    }

    public abstract static class VirtualDevice implements Editable
    {
        protected boolean mIsSelected = false;

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            for (String keyword : filteringKeywords)
                if (keyword.equals(name()))
                    return true;
            return false;
        }

        public abstract String description(Context context);

        @Override
        public String getComparableName()
        {
            return name();
        }

        @Override
        public String getSelectableTitle()
        {
            return name();
        }

        public abstract boolean isOnline();

        @Override
        public boolean isSelectableSelected()
        {
            return mIsSelected;
        }

        public abstract String name();

        @Override
        public void setId(long id)
        {
            throw new IllegalStateException("This object does not support ID attributing.");
        }
    }

    public static class DbVirtualDevice extends VirtualDevice
    {
        public final Device device;

        public DbVirtualDevice(Device device)
        {
            this.device = device;
        }

        @Override
        public boolean comparisonSupported()
        {
            return true;
        }

        @Override
        public String description(Context context)
        {
            return device.model;
        }

        @Override
        public long getComparableDate()
        {
            return device.lastUsageTime;
        }

        @Override
        public long getComparableSize()
        {
            return 0;
        }

        @Override
        public long getId()
        {
            return device.hashCode();
        }

        @Override
        public boolean isOnline()
        {
            return Device.Type.NORMAL_ONLINE.equals(device.type);
        }

        @Override
        public String name()
        {
            return device.username;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            mIsSelected = selected;
            return true;
        }
    }

    public static class DescriptionVirtualDevice extends VirtualDevice
    {
        public final NetworkDescription description;

        public DescriptionVirtualDevice(NetworkDescription description)
        {
            this.description = description;
        }

        @Override
        public boolean comparisonSupported()
        {
            return true;
        }

        @Override
        public String description(Context context)
        {
            return context.getString(R.string.text_trebleshotHotspot);
        }

        @Override
        public long getComparableDate()
        {
            return System.currentTimeMillis();
        }

        @Override
        public long getComparableSize()
        {
            return 0;
        }

        @Override
        public long getId()
        {
            return hashCode();
        }

        @Override
        public boolean isOnline()
        {
            return true;
        }

        @Override
        public String name()
        {
            return description.ssid;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            return false;
        }
    }

    public static class NetworkDescription
    {
        public String ssid;
        public String bssid;
        public String password;

        public NetworkDescription(String ssid, @Nullable String bssid, @Nullable String password)
        {
            this.ssid = ssid;
            this.bssid = bssid;
            this.password = password;
        }

        public NetworkDescription(ScanResult result)
        {
            this(result.SSID, result.BSSID, null);
        }

        @Override
        public int hashCode()
        {
            return ObjectsCompat.hash(ssid, bssid, password);
        }

        @RequiresApi(29)
        public WifiNetworkSuggestion toNetworkSuggestion()
        {
            WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setIsAppInteractionRequired(true);

            if (password != null)
                builder.setWpa2Passphrase(password);

            if (bssid != null)
                builder.setBssid(MacAddress.fromString(bssid));

            return builder.build();
        }
    }
}
