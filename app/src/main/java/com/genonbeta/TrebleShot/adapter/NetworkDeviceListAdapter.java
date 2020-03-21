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

import android.annotation.TargetApi;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiNetworkSuggestion;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.BuildConfig;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragmentBase;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.NetworkDevice;
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

import static com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment.openInfo;

public class NetworkDeviceListAdapter extends EditableListAdapter<NetworkDevice, RecyclerViewAdapter.ViewHolder>
{
    private ConnectionUtils mConnectionUtils;
    private TextDrawable.IShapeBuilder mIconBuilder;
    private List<NetworkDevice.Type> mHiddenDeviceTypes;

    public NetworkDeviceListAdapter(EditableListFragmentBase<NetworkDevice> fragment,
                                    HolderConsumer<ViewHolder> consumer, ConnectionUtils connectionUtils,
                                    NetworkDevice.Type[] hiddenDeviceTypes)
    {
        super(fragment, consumer);
        mConnectionUtils = connectionUtils;
        mIconBuilder = AppUtils.getDefaultIconBuilder(getContext());
        mHiddenDeviceTypes = hiddenDeviceTypes != null ? Arrays.asList(hiddenDeviceTypes) : new ArrayList<>();
    }

    @Override
    public List<NetworkDevice> onLoad()
    {
        List<NetworkDevice> list = new ArrayList<>();

        if (mConnectionUtils.canReadScanResults()) {
            for (ScanResult result : mConnectionUtils.getWifiManager().getScanResults()) {
                if (!AppUtils.isFamiliarHotspot(result.SSID))
                    continue;

                HotspotNetwork hotspotNetwork = new HotspotNetwork(ConnectionUtils.createWifiConfig(result,
                        null));
                hotspotNetwork.lastUsageTime = System.currentTimeMillis();

                list.add(hotspotNetwork);
            }
        }

        for (NetworkDevice device : AppUtils.getKuick(getContext()).castQuery(new SQLQuery.Select(
                        Kuick.TABLE_DEVICES).setOrderBy(Kuick.FIELD_DEVICES_LASTUSAGETIME + " DESC"),
                NetworkDevice.class))
            if (filterItem(device) && !mHiddenDeviceTypes.contains(device.type) && (!device.isLocal
                    || AppUtils.getDefaultPreferences(getContext()).getBoolean("developer_mode", false)))
                list.add(device);

        return list;
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
            NetworkDevice device = getItem(position);
            View parentView = holder.itemView;
            boolean hotspotNetwork = device instanceof NetworkSpecifier;

            TextView deviceText = parentView.findViewById(R.id.text2);
            TextView userText = parentView.findViewById(R.id.text1);
            ImageView userImage = parentView.findViewById(R.id.image);
            ImageView statusImage = parentView.findViewById(R.id.imageStatus);

            userText.setText(device.nickname);
            deviceText.setText(hotspotNetwork ? getContext().getString(R.string.text_trebleshotHotspot) : device.model);
            NetworkDeviceLoader.showPictureIntoView(device, userImage, mIconBuilder);

            if (device.isRestricted) {
                statusImage.setVisibility(View.VISIBLE);
                statusImage.setImageResource(R.drawable.ic_block_white_24dp);
            } else if (device.isTrusted) {
                statusImage.setVisibility(View.VISIBLE);
                statusImage.setImageResource(R.drawable.ic_vpn_key_white_24dp);
            } else {
                statusImage.setVisibility(View.GONE);
            }
        } catch (NotReadyException e) {
            e.printStackTrace();
        }
    }

    public static class NetworkSpecifier<T> extends NetworkDevice
    {
        public T object;

        public NetworkSpecifier(String nickname, T object)
        {
            super();

            this.nickname = AppUtils.getFriendlySSID(nickname);
            this.object = object;
            this.clientVersion = BuildConfig.CLIENT_VERSION;
            this.versionName = "stamp";
            this.versionCode = -1;
        }

        @Override
        public long getId()
        {
            return object.hashCode();
        }
    }

    /**
     * Aimed to be used with Android 9 and below
     */
    @Deprecated
    public static class HotspotNetwork extends NetworkSpecifier<WifiConfiguration>
    {
        public HotspotNetwork(WifiConfiguration configuration)
        {
            super(configuration.SSID, configuration);
        }
    }

    /**
     * Aimed to be used with Android 9 and below
     */
    @Deprecated
    public static class UnfamiliarNetwork extends NetworkSpecifier<NetworkDescription>
    {
        public UnfamiliarNetwork(NetworkDescription networkObject)
        {
            super(networkObject.ssid, networkObject);
        }
    }

    @TargetApi(29)
    public static class NetworkSuggestion extends NetworkSpecifier<WifiNetworkSuggestion>
    {
        public NetworkSuggestion(String nickname, WifiNetworkSuggestion networkObject)
        {
            super(nickname, networkObject);
        }
    }

    public static class NetworkDescription
    {
        public String ssid;
        public String bssid;
        public String password;

        public NetworkDescription(String ssid, @Nullable String bssid, String password)
        {
            this.ssid = ssid;
            this.bssid = bssid;
            this.password = password;
        }
    }
}
