package com.genonbeta.TrebleShot.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.core.util.NetworkUtils;

import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by: veli
 * Date: 5/19/17 12:18 AM
 */

public class DeviceChooserDialog extends AlertDialog.Builder
{
	private ArrayList<AddressHolder> mAvailableInterfaces = new ArrayList<>();

	public DeviceChooserDialog(Context context, NetworkDevice networkDevice, final OnDeviceSelectedListener listener)
	{
		super(context);

		HashMap<NetworkInterface, String> networkInterfaces = NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES);
		ArrayList<AddressHolder> prefixInterfaces = new ArrayList<>();

		for (NetworkInterface address : networkInterfaces.keySet())
		{
			String interfaceAddress = networkInterfaces.get(address);
			AddressHolder holder = new AddressHolder();

			holder.name = address.getDisplayName();
			holder.address = NetworkUtils.getAddressPrefix(interfaceAddress);

			prefixInterfaces.add(holder);
		}

		for (String deviceAddress : networkDevice.availableConnections)
			for (AddressHolder prefixTested : prefixInterfaces)
				if (deviceAddress.startsWith(prefixTested.address))
				{
					AddressHolder holder = new AddressHolder();

					holder.name = prefixTested.name;
					holder.address = deviceAddress;

					mAvailableInterfaces.add(holder);
					break;
				}

		setTitle(R.string.dialog_title_available_networks);
		setNegativeButton(R.string.cancel, null);

		if (mAvailableInterfaces.size() > 0)
			setAdapter(new DeviceListAdapter(), new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					listener.onDeviceSelected(mAvailableInterfaces.get(which), mAvailableInterfaces);
				}
			});
		else
			setMessage(R.string.dialog_message_available_networks_no_network);
	}

	public abstract static class OnDeviceSelectedListener
	{
		public abstract void onDeviceSelected(AddressHolder addressHolder, ArrayList<AddressHolder> availableInterfaces);
	}

	public class AddressHolder
	{
		public String name;
		public String address;
	}

	private class DeviceListAdapter extends BaseAdapter
	{
		@Override
		public int getCount()
		{
			return mAvailableInterfaces.size();
		}

		@Override
		public Object getItem(int position)
		{
			return mAvailableInterfaces.get(position);
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
				convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_available_interface, parent, false);

			AddressHolder address = (AddressHolder) getItem(position);

			TextView textView1 = (TextView) convertView.findViewById(R.id.pending_available_interface_text1);
			TextView textView2 = (TextView) convertView.findViewById(R.id.pending_available_interface_text2);

			textView1.setText(address.address);
			textView2.setText(address.name);

			return convertView;
		}
	}
}
