package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter;

import java.util.ArrayList;

import androidx.annotation.NonNull;

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
    public ArrayList<EditableNetworkDevice> onLoad()
    {
        ArrayList<EditableNetworkDevice> list = new ArrayList<>();

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
            if (!device.isLocalAddress || AppUtils.getDefaultPreferences(getContext()).getBoolean("developer_mode", false))
                list.add(device);

        return list;
    }

    @NonNull
    @Override
    public EditableListAdapter.EditableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
    {
        return new EditableListAdapter.EditableViewHolder(getInflater().inflate(R.layout.list_network_device, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull EditableListAdapter.EditableViewHolder holder, int position)
    {
        View parentView = holder.getView();
        NetworkDevice device = getList().get(position);

        boolean hotspotNetwork = device instanceof HotspotNetwork;

        TextView deviceText = parentView.findViewById(R.id.text2);
        TextView userText = parentView.findViewById(R.id.text1);
        ImageView userImage = parentView.findViewById(R.id.image);

        userText.setText(device.nickname);
        deviceText.setText(hotspotNetwork ? getContext().getString(R.string.text_trebleshotHotspot) : device.model);

        userImage.setImageDrawable(mIconBuilder.buildRound(device.nickname));
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
