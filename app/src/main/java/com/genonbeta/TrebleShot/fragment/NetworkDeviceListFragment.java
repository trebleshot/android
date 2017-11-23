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
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.provider.ScanDevicesActionProvider;
import com.genonbeta.TrebleShot.receiver.DeviceScannerProvider;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class NetworkDeviceListFragment extends com.genonbeta.TrebleShot.app.ListFragment<NetworkDevice, NetworkDeviceListAdapter> implements TitleSupport
{
	private SharedPreferences mPreferences;
	private MenuItem mAnimatedSearchMenuItem;
	private AbsListView.OnItemClickListener mClickListener;
	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		mIntentFilter.addAction(DeviceScannerProvider.ACTION_SCAN_STARTED);
		mIntentFilter.addAction(DeviceScannerProvider.ACTION_DEVICE_SCAN_COMPLETED);
		mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);

		super.onCreate(savedInstanceState);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
		setEmptyText(getString(R.string.text_findDevicesHint));

		getListView().setDividerHeight(0);

		if (mPreferences.getBoolean("scan_devices_auto", false))
			getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
	}

	@Override
	public NetworkDeviceListAdapter onAdapter()
	{
		return new NetworkDeviceListAdapter(getActivity(), mPreferences.getBoolean("developer_mode", false));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final NetworkDevice device = (NetworkDevice) getAdapter().getItem(position);

		if (mClickListener != null)
			mClickListener.onItemClick(l, v, position, id);
		else if (device.brand != null && device.model != null)
			new DeviceInfoDialog(getContext(), getAdapter().getDatabase(), device).show();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mStatusReceiver, mIntentFilter);
		refreshList();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mStatusReceiver);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_network_device, menu);

		mAnimatedSearchMenuItem = menu.findItem(R.id.network_devices_scan);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.network_devices_scan:
				getActivity().sendBroadcast(new Intent(DeviceScannerProvider.ACTION_SCAN_DEVICES));
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_deviceList);
	}

	public void setOnListClickListener(AbsListView.OnItemClickListener listener)
	{
		mClickListener = listener;
	}

	private void showSnackbar(int resId)
	{
		Snackbar.make(NetworkDeviceListFragment.this.getActivity().findViewById(android.R.id.content), resId, Snackbar.LENGTH_LONG).show();
	}

	private class StatusReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (mAnimatedSearchMenuItem != null)
				((ScanDevicesActionProvider) MenuItemCompat.getActionProvider(mAnimatedSearchMenuItem)).refreshStatus();

			if (DeviceScannerProvider.ACTION_SCAN_STARTED.equals(intent.getAction()) && intent.hasExtra(DeviceScannerProvider.EXTRA_SCAN_STATUS)) {
				String scanStatus = intent.getStringExtra(DeviceScannerProvider.EXTRA_SCAN_STATUS);

				if (DeviceScannerProvider.STATUS_OK.equals(scanStatus) || DeviceScannerProvider.SCANNER_NOT_AVAILABLE.equals(scanStatus))
					showSnackbar(DeviceScannerProvider.STATUS_OK.equals(scanStatus) ? R.string.mesg_scanningDevices : R.string.mesg_stillScanning);
				else if (DeviceScannerProvider.STATUS_NO_NETWORK_INTERFACE.equals(scanStatus)) {
					Snackbar bar = Snackbar.make(NetworkDeviceListFragment.this.getActivity().findViewById(android.R.id.content), R.string.mesg_noNetwork, Snackbar.LENGTH_SHORT);

					bar.setAction(R.string.butn_wifiSettings, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
						}
					});

					bar.show();
				}
			} else if (DeviceScannerProvider.ACTION_DEVICE_SCAN_COMPLETED.equals(intent.getAction())) {
				showSnackbar(R.string.mesg_scanCompleted);
			} else if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_DEVICES)) {
				refreshList();
			}
		}
	}
}
