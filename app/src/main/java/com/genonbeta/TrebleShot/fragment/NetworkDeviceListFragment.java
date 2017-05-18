package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NotificationUtils;
import com.genonbeta.TrebleShot.provider.ScanDevicesActionProvider;
import com.genonbeta.TrebleShot.receiver.DeviceScannerProvider;
import com.genonbeta.TrebleShot.support.FragmentTitle;

public class NetworkDeviceListFragment extends ListFragment implements FragmentTitle
{
	private IntentFilter mIntentFilter = new IntentFilter();
	private SelfReceiver mReceiver = new SelfReceiver();
	private NetworkDeviceListAdapter mListAdapter;
	private NotificationUtils mNotification;
	private SharedPreferences mPreferences;
	private MenuItem mAnimatedSearchMenuItem;
	private AbsListView.OnItemClickListener mClickListener;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mIntentFilter.addAction(DeviceScannerProvider.ACTION_DEVICE_FOUND);
		mIntentFilter.addAction(DeviceScannerProvider.ACTION_SCAN_STARTED);
		mIntentFilter.addAction(DeviceScannerProvider.ACTION_DEVICE_SCAN_COMPLETED);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mNotification = new NotificationUtils(getActivity());
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mListAdapter = new NetworkDeviceListAdapter(getActivity(), mPreferences.getBoolean("developer_mode", false));

		setListAdapter(mListAdapter);
		setHasOptionsMenu(true);

		getListView().setDividerHeight(0);

		setEmptyText(getString(R.string.find_device_hint));

		if (mPreferences.getBoolean("developer_mode", false))
		{
			NetworkDevice device = new NetworkDevice("127.0.0.1");
			device.isLocalAddress = true;

			mListAdapter.getDeviceRegistry().registerDevice(device);

			getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_ADD_IP)
					.putExtra(DeviceScannerProvider.EXTRA_DEVICE_IP, "127.0.0.1"));
		}

		if (mPreferences.getBoolean("scan_devices_auto", false))
			getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final NetworkDevice device = (NetworkDevice) mListAdapter.getItem(position);

		if (mClickListener != null)
		{
			this.mClickListener.onItemClick(l, v, position, id);
		}
		else if (device.brand != null && device.model != null)
		{
			new DeviceInfoDialog(getContext(), mListAdapter.getDeviceRegistry(), mNotification, device).show();
		}
	}

	@Override
	public void onResume()
	{
		super.onResume();

		mListAdapter.notifyDataSetChanged();
		getActivity().registerReceiver(mReceiver, mIntentFilter);
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.network_devices_options, menu);

		mAnimatedSearchMenuItem = menu.findItem(R.id.network_devices_scan);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.network_devices_scan:
				getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
				return true;
			case R.id.network_devices_clear_list:
				mListAdapter.getDeviceRegistry().removeAll();
				mListAdapter.notifyDataSetChanged();
				mNotification.showToast(R.string.device_list_cleared_msg);
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public CharSequence getFragmentTitle(Context context)
	{
		return context.getString(R.string.device_list);
	}

	public void setOnListClickListener(AbsListView.OnItemClickListener listener)
	{
		this.mClickListener = listener;
	}

	private void showSnackbar(int resId)
	{
		Snackbar.make(NetworkDeviceListFragment.this.getActivity().findViewById(android.R.id.content), resId, Snackbar.LENGTH_SHORT).show();
	}

	private class SelfReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			((ScanDevicesActionProvider) MenuItemCompat.getActionProvider(mAnimatedSearchMenuItem)).refreshStatus();

			if (DeviceScannerProvider.ACTION_DEVICE_FOUND.equals(intent.getAction()))
			{
				mListAdapter.notifyDataSetChanged();
			}
			else if (DeviceScannerProvider.ACTION_SCAN_STARTED.equals(intent.getAction()) && intent.hasExtra(DeviceScannerProvider.EXTRA_SCAN_STATUS))
			{
				String scanStatus = intent.getStringExtra(DeviceScannerProvider.EXTRA_SCAN_STATUS);

				if (DeviceScannerProvider.STATUS_OK.equals(scanStatus))
					showSnackbar(R.string.devices_scanning_msg);
				else if (DeviceScannerProvider.STATUS_NO_NETWORK_INTERFACE.equals(scanStatus))
				{
					Snackbar bar = Snackbar.make(NetworkDeviceListFragment.this.getActivity().findViewById(android.R.id.content), R.string.no_network_interface_msg, Snackbar.LENGTH_SHORT);

					bar.setAction(R.string.open_wifi_settings_short, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
						}
					});

					bar.show();
				}
			}
			else if (DeviceScannerProvider.ACTION_DEVICE_SCAN_COMPLETED.equals(intent.getAction()))
			{
				showSnackbar(R.string.scan_completed);
			}
		}
	}
}
