package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.ListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

public class NetworkDeviceListAdapter extends ListAdapter<NetworkDevice>
{
	private boolean mShowLocalAddresses = false;
	private ArrayList<NetworkDevice> mList = new ArrayList<>();
	private AccessDatabase mDatabase;
	private WifiManager mWifiManager;

	public NetworkDeviceListAdapter(Context context, WifiManager wifiManager, boolean showLocalAddresses)
	{
		super(context);

		mShowLocalAddresses = showLocalAddresses;
		mDatabase = new AccessDatabase(getContext());
		mWifiManager = wifiManager;
	}

	@Override
	public ArrayList<NetworkDevice> onLoad()
	{
		ArrayList<NetworkDevice> list = new ArrayList<>();
		ArrayList<NetworkDevice> dbList = new ArrayList<>();

		for (ScanResult resultIndex : mWifiManager.getScanResults()) {
			if (!resultIndex.SSID.startsWith(AppConfig.ACCESS_POINT_PREFIX))
				continue;

			HotspotNetwork hotspotNetwork = new HotspotNetwork();

			hotspotNetwork.lastUsageTime = System.currentTimeMillis();
			hotspotNetwork.scanResult = resultIndex;
			hotspotNetwork.nickname = resultIndex.SSID
					.substring(AppConfig.ACCESS_POINT_PREFIX.length())
					.replace("_", " ");

			dbList.add(hotspotNetwork);
		}

		dbList.addAll(mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICES)
				.setOrderBy(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME + " DESC"), NetworkDevice.class));

		for (NetworkDevice device : dbList)
			if (device instanceof HotspotNetwork
					|| (device.nickname != null && device.model != null && device.brand != null && (mShowLocalAddresses || !device.isLocalAddress)))
				list.add(device);

		return list;
	}

	@Override
	public void onUpdate(ArrayList<NetworkDevice> passedItem)
	{
		mList.clear();
		mList.addAll(passedItem);
	}

	@Override
	public int getCount()
	{
		return mList.size();
	}

	public AccessDatabase getDatabase()
	{
		return mDatabase;
	}

	@Override
	public Object getItem(int itemId)
	{
		return mList.get(itemId);
	}

	@Override
	public long getItemId(int p1)
	{
		return 0;
	}

	@Override
	public ArrayList<NetworkDevice> getList()
	{
		return mList;
	}

	@Override
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		if (view == null)
			view = getInflater().inflate(R.layout.list_network_device, viewGroup, false);

		NetworkDevice device = (NetworkDevice) getItem(position);
		String firstLetters = TextUtils.getFirstLetters(device.nickname, 1);
		boolean hotspotNetwork = device instanceof HotspotNetwork;

		TextView deviceText = view.findViewById(R.id.network_device_list_device_text);
		TextView userText = view.findViewById(R.id.network_device_list_user_text);
		ImageView userImage = view.findViewById(R.id.network_device_list_device_image);

		userText.setText(device.nickname);
		deviceText.setText(hotspotNetwork ? mContext.getString(R.string.text_trebleshotHotspot) : device.model);

		userImage.setImageDrawable(TextDrawable.builder().buildRoundRect(firstLetters.length() > 0
				? firstLetters
				: "?", ContextCompat.getColor(mContext, hotspotNetwork ? R.color.hotspotNetworkRipple : R.color.networkDeviceRipple), 100));

		return view;
	}

	public static class HotspotNetwork extends NetworkDevice
	{
		public ScanResult scanResult;
	}
}
