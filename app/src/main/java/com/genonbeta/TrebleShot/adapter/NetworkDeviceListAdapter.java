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
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkSuggestion;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragmentBase;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.view.HolderConsumer;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.genonbeta.TrebleShot.fragment.DeviceListFragment.openInfo;

public class NetworkDeviceListAdapter extends EditableListAdapter<NetworkDeviceListAdapter.InfoHolder, RecyclerViewAdapter.ViewHolder>
{
    private ConnectionUtils mConnectionUtils;
    private TextDrawable.IShapeBuilder mIconBuilder;
    private List<Device.Type> mHiddenDeviceTypes;

    public NetworkDeviceListAdapter(EditableListFragmentBase<InfoHolder> fragment,
                                    HolderConsumer<ViewHolder> consumer, ConnectionUtils connectionUtils,
                                    Device.Type[] hiddenDeviceTypes)
    {
        super(fragment, consumer);
        mConnectionUtils = connectionUtils;
        mIconBuilder = AppUtils.getDefaultIconBuilder(getContext());
        mHiddenDeviceTypes = hiddenDeviceTypes != null ? Arrays.asList(hiddenDeviceTypes) : new ArrayList<>();
    }

    @Override
    public List<InfoHolder> onLoad()
    {
        boolean devMode = AppUtils.getDefaultPreferences(getContext()).getBoolean("developer_mode", false);
        List<InfoHolder> list = new ArrayList<>();

        if (mConnectionUtils.canReadScanResults()) {
            for (ScanResult result : mConnectionUtils.getWifiManager().getScanResults()) {
                if ((result.capabilities == null || result.capabilities.contains("OPEN"))
                        && ConnectionUtils.isClientNetwork(result.SSID))
                    list.add(new InfoHolder(new NetworkDescription(result)));
            }
        }

        for (Device device : AppUtils.getKuick(getContext()).castQuery(new SQLQuery.Select(Kuick.TABLE_DEVICES)
                .setOrderBy(Kuick.FIELD_DEVICES_LASTUSAGETIME + " DESC"), Device.class))
            if (!mHiddenDeviceTypes.contains(device.type) && (!device.isLocal || devMode))
                list.add(new InfoHolder(device));

        List<InfoHolder> filteredList = new ArrayList<>();
        for (InfoHolder infoHolder : list)
            if (filterItem(infoHolder))
                filteredList.add(infoHolder);

        return filteredList;
    }

    @NonNull
    @Override
    public RecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        ViewHolder holder = new RecyclerViewAdapter.ViewHolder(getInflater().inflate(
                isHorizontalOrientation() || isGridLayoutRequested() ? R.layout.list_network_device_grid
                        : R.layout.list_network_device, parent, false));

        getConsumer().registerLayoutViewClicks(holder);
        holder.itemView.findViewById(R.id.menu)
                .setOnClickListener(v -> openInfo(getFragment().getActivity(), mConnectionUtils,
                        getList().get(holder.getAdapterPosition())));

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerViewAdapter.ViewHolder holder, int position)
    {
        try {
            InfoHolder infoHolder = getItem(position);
            Object specifier = infoHolder.object();
            View parentView = holder.itemView;

            TextView text1 = parentView.findViewById(R.id.text1);
            TextView text2 = parentView.findViewById(R.id.text2);
            ImageView image = parentView.findViewById(R.id.image);
            ImageView statusImage = parentView.findViewById(R.id.imageStatus);

            text1.setText(infoHolder.name());
            text2.setText(infoHolder.description(getContext()));
            boolean isRestricted = false;
            boolean isTrusted = false;

            if (specifier instanceof Device) {
                Device device = (Device) specifier;
                isRestricted = device.isRestricted;
                isTrusted = device.isTrusted;

                NetworkDeviceLoader.showPictureIntoView(device, image, mIconBuilder);
            } else
                image.setImageDrawable(mIconBuilder.buildRound(infoHolder.name()));

            if (isRestricted) {
                statusImage.setVisibility(View.VISIBLE);
                statusImage.setImageResource(R.drawable.ic_block_white_24dp);
            } else if (isTrusted) {
                statusImage.setVisibility(View.VISIBLE);
                statusImage.setImageResource(R.drawable.ic_vpn_key_white_24dp);
            } else {
                statusImage.setVisibility(View.GONE);
            }
        } catch (NotReadyException e) {
            e.printStackTrace();
        }
    }

    public static final class InfoHolder implements Editable
    {
        private Object mObject;
        private boolean mIsSelected = false;

        public InfoHolder(Device device)
        {
            mObject = device;
        }

        public InfoHolder(NetworkDescription description)
        {
            mObject = description;
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            for (String keyword : filteringKeywords)
                if (keyword.equals(name()))
                    return true;
            return false;
        }

        public String description(Context context)
        {
            if (mObject instanceof Device)
                return ((Device) mObject).model;
            else if (mObject instanceof NetworkDescription)
                return context.getString(R.string.text_trebleshotHotspot);

            return context.getString(R.string.text_unknown);
        }

        @Override
        public boolean comparisonSupported()
        {
            return mObject instanceof Device;
        }

        @Override
        public String getComparableName()
        {
            return name();
        }

        @Override
        public long getComparableDate()
        {
            if (mObject instanceof Device)
                return ((Device) mObject).lastUsageTime;
            return 0;
        }

        @Override
        public long getComparableSize()
        {
            return 0;
        }

        @Override
        public long getId()
        {
            if (mObject instanceof Device)
                return ((Device) mObject).id.hashCode();

            return 0;
        }

        @Override
        public String getSelectableTitle()
        {
            return name();
        }

        @Override
        public boolean isSelectableSelected()
        {
            return mIsSelected;
        }

        public String name()
        {
            if (mObject instanceof Device)
                return ((Device) mObject).nickname;
            else if (mObject instanceof WifiConfiguration)
                return AppUtils.getFriendlySSID(((WifiConfiguration) mObject).SSID);
            else if (mObject instanceof NetworkDescription)
                return AppUtils.getFriendlySSID(((NetworkDescription) mObject).ssid);

            return mObject.toString();
        }

        public Object object()
        {
            return mObject;
        }

        @Override
        public void setId(long id)
        {
            throw new IllegalStateException("This object does not support ID attributing.");
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            if (mObject instanceof Device) {
                mIsSelected = selected;
                return true;
            }
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

        @RequiresApi(29)
        public WifiNetworkSuggestion toNetworkSuggestion()
        {
            // TODO: 1/1/20 Ensure using both wpa2 and wpa3 does not cause an issue
            WifiNetworkSuggestion.Builder builder = new WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setIsAppInteractionRequired(true);

            if (password != null)
                builder.setWpa2Passphrase(password)
                        .setWpa3Passphrase(password);

            if (bssid != null)
                builder.setBssid(MacAddress.fromString(bssid));

            return builder.build();
        }
    }
}
