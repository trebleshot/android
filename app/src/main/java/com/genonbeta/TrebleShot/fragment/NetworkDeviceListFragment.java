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
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.ListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class NetworkDeviceListFragment
		extends ListFragment<NetworkDevice, NetworkDeviceListAdapter>
		implements TitleSupport
{
	private SharedPreferences mPreferences;
	private MenuItem mSearchMenuItem;
	private AbsListView.OnItemClickListener mClickListener;
	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();
	private SwipeRefreshLayout mSwipeRefreshLayout;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

		mIntentFilter.addAction(DeviceScannerService.ACTION_SCAN_STARTED);
		mIntentFilter.addAction(DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED);
		mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);

		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

		mSwipeRefreshLayout = new SwipeRefreshLayout(getContext());

		mSwipeRefreshLayout.addView(listFragmentView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		mSwipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		return mSwipeRefreshLayout;
	}

	@Override
	public void onViewCreated(View view, final Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		getListView().setOnScrollListener(new AbsListView.OnScrollListener()
		{
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState)
			{
				if (scrollState == SCROLL_STATE_IDLE)
					mSwipeRefreshLayout.setEnabled(view.getFirstVisiblePosition() == 0);
			}

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {}
		});

		mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener()
		{
			@Override
			public void onRefresh()
			{
				requestRefresh();
			}
		});
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
		setEmptyText(getString(R.string.text_findDevicesHint));

		getListView().setDividerHeight(0);

		if (mPreferences.getBoolean("scan_devices_auto", false))
			requestRefresh();
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

		checkRefreshing();
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

		mSearchMenuItem = menu.findItem(R.id.network_devices_scan);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.network_devices_scan:
				requestRefresh();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	public void checkRefreshing()
	{
		mSwipeRefreshLayout.setRefreshing(!DeviceScannerService
				.getDeviceScanner()
				.isScannerAvailable());
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_deviceList);
	}

	public void requestRefresh()
	{
		if (DeviceScannerService.getDeviceScanner().isScannerAvailable())
			getContext().startService(new Intent(getContext(), DeviceScannerService.class)
					.setAction(DeviceScannerService.ACTION_SCAN_DEVICES));
		else {
			Toast.makeText(getContext(), R.string.mesg_stopping, Toast.LENGTH_SHORT).show();

			DeviceScannerService
					.getDeviceScanner()
					.interrupt();
		}
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
			checkRefreshing();

			if (DeviceScannerService.ACTION_SCAN_STARTED.equals(intent.getAction()) && intent.hasExtra(DeviceScannerService.EXTRA_SCAN_STATUS)) {
				String scanStatus = intent.getStringExtra(DeviceScannerService.EXTRA_SCAN_STATUS);

				if (DeviceScannerService.STATUS_OK.equals(scanStatus) || DeviceScannerService.SCANNER_NOT_AVAILABLE.equals(scanStatus))
					showSnackbar(DeviceScannerService.STATUS_OK.equals(scanStatus) ? R.string.mesg_scanningDevices : R.string.mesg_stillScanning);
				else if (DeviceScannerService.STATUS_NO_NETWORK_INTERFACE.equals(scanStatus)) {
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
			} else if (DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED.equals(intent.getAction())) {
				showSnackbar(R.string.mesg_scanCompleted);
			} else if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_DEVICES)) {
				refreshList();
			}
		}
	}
}
