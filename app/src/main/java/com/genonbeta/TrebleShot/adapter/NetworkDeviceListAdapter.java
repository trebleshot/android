package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;
import java.util.List;

public class NetworkDeviceListAdapter extends EditableListAdapter<NetworkDeviceListAdapter.EditableNetworkDevice, EditableListAdapter.EditableViewHolder>
{
    private ConnectionUtils mConnectionUtils;
    private TextDrawable.IShapeBuilder mIconBuilder;

    public NetworkDeviceListAdapter(Context context, ConnectionUtils connectionUtils)
    {
        super(context);

        mConnectionUtils = connectionUtils;
        mIconBuilder = AppUtils.getDefaultIconBuilder(context);
    }

    @Override
    public List<EditableNetworkDevice> onLoad()
    {
        List<EditableNetworkDevice> list = new ArrayList<>();

        if (mConnectionUtils.canReadScanResults()) {
            for (ScanResult resultIndex : mConnectionUtils.getWifiManager().getScanResults()) {
                if (!resultIndex.SSID.startsWith(AppConfig.PREFIX_ACCESS_POINT))
                    continue;

                HotspotNetwork hotspotNetwork = new HotspotNetwork();

                hotspotNetwork.lastUsageTime = System.currentTimeMillis();
                hotspotNetwork.SSID = resultIndex.SSID;
                hotspotNetwork.BSSID = resultIndex.BSSID;
                hotspotNetwork.nickname = AppUtils.getFriendlySSID(resultIndex.SSID);

                list.add(hotspotNetwork);
            }
        }

        if (list.size() == 0 && mConnectionUtils.isConnectionSelfNetwork()) {
            WifiInfo wifiInfo = mConnectionUtils.getWifiManager().getConnectionInfo();

            HotspotNetwork hotspotNetwork = new HotspotNetwork();

            hotspotNetwork.lastUsageTime = System.currentTimeMillis();
            hotspotNetwork.SSID = wifiInfo.getSSID();
            hotspotNetwork.BSSID = wifiInfo.getBSSID();
            hotspotNetwork.nickname = AppUtils.getFriendlySSID(wifiInfo.getSSID());

            list.add(hotspotNetwork);
        }

        for (EditableNetworkDevice device : AppUtils.getDatabase(getContext()).castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICES)
                .setOrderBy(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME + " DESC"), EditableNetworkDevice.class))
            if (filterItem(device) && (!device.isLocalAddress || AppUtils.getDefaultPreferences(getContext()).getBoolean("developer_mode", false)))
                list.add(device);

        return list;
    }

    @NonNull
    @Override
    public EditableListAdapter.EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new EditableListAdapter.EditableViewHolder(getInflater().inflate(
                isHorizontalOrientation() || isGridLayoutRequested()
                        ? R.layout.list_network_device_grid
                        : R.layout.list_network_device, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EditableListAdapter.EditableViewHolder holder, int position)
    {
        try {
            NetworkDevice device = getItem(position);
            View parentView = holder.getView();
            boolean hotspotNetwork = device instanceof HotspotNetwork;

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

    public static class EditableNetworkDevice
            extends NetworkDevice
            implements Editable
    {
        private boolean mIsSelected = false;

        public EditableNetworkDevice()
        {
            super();
        }

        @Override
        public boolean applyFilter(String[] filteringKeywords)
        {
            for (String keyword : filteringKeywords)
                if (nickname.toLowerCase().contains(keyword.toLowerCase()))
                    return true;

            return false;
        }

        @Override
        public boolean comparisonSupported()
        {
            return true;
        }

        @Override
        public long getId()
        {
            return deviceId.hashCode();
        }

        @Override
        public void setId(long id)
        {

        }

        @Override
        public String getComparableName()
        {
            return nickname;
        }

        @Override
        public long getComparableDate()
        {
            return lastUsageTime;
        }

        @Override
        public long getComparableSize()
        {
            return 0;
        }

        @Override
        public String getSelectableTitle()
        {
            return nickname;
        }

        @Override
        public boolean isSelectableSelected()
        {
            return mIsSelected;
        }

        @Override
        public boolean setSelectableSelected(boolean selected)
        {
            mIsSelected = selected;
            return true;
        }
    }

    public static class HotspotNetwork extends EditableNetworkDevice
    {
        public String SSID;
        public String BSSID;
        public String password;
        public int keyManagement;
        public boolean qrConnection;

        public HotspotNetwork()
        {
            super();

            this.versionName = "stamp";
            this.versionNumber = -1;
        }

        @Override
        public long getId()
        {
            return SSID.hashCode();
        }
    }
}
