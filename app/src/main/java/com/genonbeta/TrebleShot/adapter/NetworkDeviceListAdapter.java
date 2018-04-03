package com.genonbeta.TrebleShot.adapter;

import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

public class NetworkDeviceListAdapter extends RecyclerViewAdapter<NetworkDevice, RecyclerViewAdapter.ViewHolder>
{
	private AccessDatabase mDatabase;
	private NetworkDeviceListFragment mFragment;
	private SharedPreferences mPreferences;
	private ArrayList<NetworkDevice> mList = new ArrayList<>();

	public NetworkDeviceListAdapter(NetworkDeviceListFragment fragment, AccessDatabase database, SharedPreferences preferences)
	{
		super(fragment.getActivity());

		mFragment = fragment;
		mDatabase = database;
		mPreferences = preferences;
	}

	@Override
	public ArrayList<NetworkDevice> onLoad()
	{
		ArrayList<NetworkDevice> list = new ArrayList<>();

		if (mFragment.canReadScanResults(getContext())) {
			for (ScanResult resultIndex : mFragment.getWifiManager().getScanResults()) {
				if (!resultIndex.SSID.startsWith(AppConfig.PREFIX_ACCESS_POINT))
					continue;

				HotspotNetwork hotspotNetwork = new HotspotNetwork();

				hotspotNetwork.lastUsageTime = System.currentTimeMillis();
				hotspotNetwork.SSID = resultIndex.SSID;
				hotspotNetwork.BSSID = resultIndex.BSSID;
				hotspotNetwork.nickname = getFriendlySSID(resultIndex.SSID);

				list.add(hotspotNetwork);
			}
		}

		if (list.size() == 0 && mFragment.isConnectionSelfNetwork()) {
			WifiInfo wifiInfo = mFragment.getWifiManager().getConnectionInfo();

			HotspotNetwork hotspotNetwork = new HotspotNetwork();

			hotspotNetwork.lastUsageTime = System.currentTimeMillis();
			hotspotNetwork.SSID = wifiInfo.getSSID();
			hotspotNetwork.BSSID = wifiInfo.getBSSID();
			hotspotNetwork.nickname = getFriendlySSID(wifiInfo.getSSID());

			list.add(hotspotNetwork);
		}

		for (NetworkDevice device : mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICES)
				.setOrderBy(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME + " DESC"), NetworkDevice.class))
			if (device instanceof HotspotNetwork
					|| !device.isLocalAddress || mPreferences.getBoolean("developer_mode", false))
				list.add(device);

		return list;
	}

	@Override
	public void onUpdate(ArrayList<NetworkDevice> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	public String getFriendlySSID(String ssid)
	{
		return ssid
				.replace("\"", "")
				.substring(AppConfig.PREFIX_ACCESS_POINT.length())
				.replace("_", " ");
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
	{
		return new ViewHolder(getInflater().inflate(R.layout.list_network_device, parent, false));
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position)
	{
		View parentView = holder.getView();
		NetworkDevice device = getList().get(position);

		String firstLetters = TextUtils.getLetters(device.nickname, 0);
		boolean hotspotNetwork = device instanceof HotspotNetwork;

		TextView deviceText = parentView.findViewById(R.id.network_device_list_device_text);
		TextView userText = parentView.findViewById(R.id.network_device_list_user_text);
		ImageView userImage = parentView.findViewById(R.id.network_device_list_device_image);

		userText.setText(device.nickname);
		deviceText.setText(hotspotNetwork ? mContext.getString(R.string.text_trebleshotHotspot) : device.model);

		userImage.setImageDrawable(TextDrawable.builder().buildRoundRect(firstLetters.length() > 0
				? firstLetters
				: "?", ContextCompat.getColor(mContext, hotspotNetwork ? R.color.hotspotNetworkRipple : R.color.networkDeviceRipple), 100));
	}

	@Override
	public long getItemId(int p1)
	{
		return p1;
	}

	@Override
	public int getItemCount()
	{
		return mList.size();
	}

	@Override
	public ArrayList<NetworkDevice> getList()
	{
		return mList;
	}

	public static class HotspotNetwork extends NetworkDevice
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
	}
}
