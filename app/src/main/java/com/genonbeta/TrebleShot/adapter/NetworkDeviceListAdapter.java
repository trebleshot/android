package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationSet;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.GAnimater;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.widget.ListAdapter;

import java.util.ArrayList;

public class NetworkDeviceListAdapter extends ListAdapter<NetworkDevice>
{
	private boolean mShowLocalAddresses = false;
	private DeviceRegistry mDeviceRegistry;
	private ArrayList<NetworkDevice> mList = new ArrayList<>();

	public NetworkDeviceListAdapter(Context context, boolean showLocalAddresses)
	{
		super(context);

		mShowLocalAddresses = showLocalAddresses;
		mDeviceRegistry = new DeviceRegistry(context);
	}

	@Override
	public ArrayList<NetworkDevice> onLoad()
	{
		ArrayList<NetworkDevice> list = new ArrayList<>();

		for (NetworkDevice device : mDeviceRegistry.getDeviceList())
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

	public DeviceRegistry getDeviceRegistry()
	{
		return mDeviceRegistry;
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
	public View getView(int position, View view, ViewGroup viewGroup)
	{
		if (view == null)
		{
			view = getInflater().inflate(R.layout.list_network_device, viewGroup, false);
			AnimationSet set = GAnimater.getAnimation(GAnimater.APPEAR);
			view.setAnimation(set);
		}

		TextView deviceText = (TextView) view.findViewById(R.id.network_device_list_device_text);
		TextView userText = (TextView) view.findViewById(R.id.network_device_list_user_text);
		ImageView userImage = (ImageView) view.findViewById(R.id.network_device_list_device_image);

		NetworkDevice device = (NetworkDevice) getItem(position);
		String firstLetters = ApplicationHelper.getFirstLetters(device.user, 1);

		TextDrawable drawable = TextDrawable.builder().buildRoundRect(firstLetters.length() > 0 ? firstLetters : "?", ContextCompat.getColor(mContext, R.color.colorTextDrawable), 100);

		deviceText.setText(device.model);
		userText.setText(device.user);
		userImage.setImageDrawable(drawable);

		return view;
	}
}
