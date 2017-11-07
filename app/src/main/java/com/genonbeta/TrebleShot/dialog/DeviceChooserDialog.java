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
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.android.database.SQLQuery;

import java.util.ArrayList;

/**
 * Created by: veli
 * Date: 5/19/17 12:18 AM
 */

public class DeviceChooserDialog extends AlertDialog.Builder
{
	private ArrayList<NetworkDevice.Connection> mAvailableInterfaces = new ArrayList<>();
	private NetworkDevice mNetworkDevice;
	private AccessDatabase mDatabase;
	private OnDeviceSelectedListener mDeviceSelectedListener;

	public DeviceChooserDialog(Context context, AccessDatabase database, NetworkDevice networkDevice, final OnDeviceSelectedListener listener)
	{
		super(context);

		mDatabase = database;
		mNetworkDevice = networkDevice;
		mDeviceSelectedListener = listener;
	}

	@Override
	public AlertDialog show()
	{
		mAvailableInterfaces.clear();

		// FIXME: 3.11.2017 Use new structure
		/*

		HashMap<NetworkInterface, String> networkInterfaces = NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES);
		ArrayList<Network> prefixInterfaces = new ArrayList<>();

		for (NetworkInterface address : networkInterfaces.keySet()) {
			String interfaceAddress = networkInterfaces.get(address);
			AddressHolder holder = new AddressHolder();

			holder.adapterName = address.getDisplayName();
			holder.address = NetworkUtils.getAddressPrefix(interfaceAddress);

			prefixInterfaces.add(holder);
		}

		for (String deviceAddress : mNetworkDevice.availableConnections)
			for (AddressHolder prefixTested : prefixInterfaces)
				if (deviceAddress.startsWith(prefixTested.address)) {
					AddressHolder holder = new AddressHolder();

					holder.adapterName = prefixTested.adapterName;
					holder.address = deviceAddress;

					mAvailableInterfaces.add(holder);
					break;
				}
				*/

		mAvailableInterfaces.addAll(mDatabase.castQuery(new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
				.setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", mNetworkDevice.deviceId), NetworkDevice.Connection.class));

		setTitle(R.string.text_availableNetworks);
		setNegativeButton(R.string.butn_cancel, null);

		if (mAvailableInterfaces.size() > 0)
			setAdapter(new DeviceListAdapter(), new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					mDeviceSelectedListener.onDeviceSelected(mAvailableInterfaces.get(which), mAvailableInterfaces);
				}
			});
		else
			setMessage(R.string.text_noNetworkAvailable);

		return super.show();
	}

	public abstract static class OnDeviceSelectedListener
	{
		public abstract void onDeviceSelected(NetworkDevice.Connection connection, ArrayList<NetworkDevice.Connection> availableInterfaces);
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

			NetworkDevice.Connection address = (NetworkDevice.Connection) getItem(position);

			TextView textView1 = (TextView) convertView.findViewById(R.id.pending_available_interface_text1);
			TextView textView2 = (TextView) convertView.findViewById(R.id.pending_available_interface_text2);

			textView1.setText(address.ipAddress);
			textView2.setText(address.adapterName);

			return convertView;
		}
	}
}
