package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.TextUtils;
import com.genonbeta.TrebleShot.widget.ListAdapter;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

public class NetworkDeviceListAdapter extends ListAdapter<NetworkDevice>
{
	private boolean mShowLocalAddresses = false;
	private ArrayList<NetworkDevice> mList = new ArrayList<>();
	private AccessDatabase mDatabase;

	public NetworkDeviceListAdapter(Context context, boolean showLocalAddresses)
	{
		super(context);

		mShowLocalAddresses = showLocalAddresses;
		mDatabase = new AccessDatabase(getContext());
	}

	@Override
	public ArrayList<NetworkDevice> onLoad()
	{
		ArrayList<NetworkDevice> list = new ArrayList<>();
		ArrayList<NetworkDevice> dbList = mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICES)
				.setOrderBy(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME + " DESC"), NetworkDevice.class);

		for (NetworkDevice device : dbList)
			if (device.user != null && device.model != null && device.brand != null && (mShowLocalAddresses || !device.isLocalAddress))
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

		TextView deviceText = view.findViewById(R.id.network_device_list_device_text);
		TextView userText = view.findViewById(R.id.network_device_list_user_text);
		ImageView userImage = view.findViewById(R.id.network_device_list_device_image);

		NetworkDevice device = (NetworkDevice) getItem(position);
		String firstLetters = TextUtils.getFirstLetters(device.user, 1);

		TextDrawable drawable = TextDrawable.builder().buildRoundRect(firstLetters.length() > 0 ? firstLetters : "?", ContextCompat.getColor(mContext, R.color.colorTextDrawable), 100);

		deviceText.setText(device.model);
		userText.setText(device.user);
		userImage.setImageDrawable(drawable);

		return view;
	}
}
