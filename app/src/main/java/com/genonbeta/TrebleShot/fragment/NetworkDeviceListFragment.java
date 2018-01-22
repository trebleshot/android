package com.genonbeta.TrebleShot.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
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
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.receiver.NetworkStatusReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.FABSupport;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.NsdDiscovery;
import com.genonbeta.TrebleShot.util.TitleSupport;

public class NetworkDeviceListFragment
		extends ListFragment<NetworkDevice, NetworkDeviceListAdapter>
		implements TitleSupport, FABSupport
{
	public static final int REQUEST_LOCATION_PERMISSION = 643;

	private NsdDiscovery mNsdDiscovery;
	private SharedPreferences mPreferences;
	private AbsListView.OnItemClickListener mClickListener;
	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private FloatingActionButton mFAB;
	private HotspotUtils mHotspotUtils;
	private WifiManager mWifiManager;
	private ConnectivityManager mConnectivityManager;
	private boolean mShowHotspotInfo = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mHotspotUtils = HotspotUtils.getInstance(getContext());
		mWifiManager = mHotspotUtils.getWifiManager();
		mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

		mIntentFilter.addAction(DeviceScannerService.ACTION_SCAN_STARTED);
		mIntentFilter.addAction(DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED);
		mIntentFilter.addAction(NetworkStatusReceiver.WIFI_AP_STATE_CHANGED);
		mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
		mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

		super.onCreate(savedInstanceState);

		mNsdDiscovery = new NsdDiscovery(getContext(), getAdapter().getDatabase());
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		final View listFragmentView = super.onCreateView(inflater, container, savedInstanceState);

		mSwipeRefreshLayout = new SwipeRefreshLayout(getContext());

		mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(getActivity(), R.color.colorPrimary),
				ContextCompat.getColor(getActivity(), R.color.colorAccent));

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
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount)
			{
			}
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
		return new NetworkDeviceListAdapter(this, mPreferences.getBoolean("developer_mode", false));
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id)
	{
		super.onListItemClick(l, v, position, id);

		final NetworkDevice device = (NetworkDevice) getAdapter().getItem(position);

		if (mClickListener != null) {
			if (device.versionNumber != -1
					&& device.versionNumber < AppConfig.SUPPORTED_MIN_VERSION)
				createSnackbar(R.string.mesg_versionNotSupported)
						.show();
			else
				mClickListener.onItemClick(l, v, position, id);
		} else if (device instanceof NetworkDeviceListAdapter.HotspotNetwork) {
			final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = (NetworkDeviceListAdapter.HotspotNetwork) device;

			AlertDialog.Builder builder = new AlertDialog.Builder(getContext());

			builder.setTitle(hotspotNetwork.nickname);
			builder.setMessage(R.string.text_trebleshotHotspotDescription);
			builder.setNegativeButton(R.string.butn_close, null);
			builder.setPositiveButton(isConnectedToNetwork(hotspotNetwork) ? R.string.butn_disconnect : R.string.butn_connect, new DialogInterface.OnClickListener()
			{
				@Override
				public void onClick(DialogInterface dialog, int which)
				{
					toggleConnection(hotspotNetwork);
				}
			});

			builder.show();
		} else if (device.brand != null && device.model != null)
			new DeviceInfoDialog(getActivity(), getAdapter().getDatabase(), device).show();
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mStatusReceiver, mIntentFilter);

		refreshList();
		checkRefreshing();

		mNsdDiscovery.startDiscovering();
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mStatusReceiver);

		mNsdDiscovery.stopDiscovering();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.actions_network_device, menu);
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

	@Override
	public boolean onFABRequested(FloatingActionButton floatingActionButton)
	{
		if (!HotspotUtils.isSupported())
			return false;

		mFAB = floatingActionButton;

		mFAB.setImageResource(R.drawable.ic_wifi_tethering_white_24dp);
		mFAB.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(mFAB.getContext())) {
					Snackbar.make(v, R.string.mesg_errorHotspotPermission, Snackbar.LENGTH_LONG)
							.setAction(R.string.butn_settings, new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
											.setData(Uri.parse("package:" + mFAB.getContext().getPackageName()))
											.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
								}
							})
							.show();
				} else if (!mHotspotUtils.isEnabled() && isMobileDataActive()) {
					Snackbar.make(v, R.string.mesg_warningHotspotMobileActive, Snackbar.LENGTH_LONG)
							.setAction(R.string.butn_skip, new View.OnClickListener()
							{
								@Override
								public void onClick(View v)
								{
									toggleHotspot();
								}
							})
							.show();
				} else
					toggleHotspot();
			}
		});

		return true;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (REQUEST_LOCATION_PERMISSION == requestCode)
			showConnectionOptions();
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

	public WifiManager getWifiManager()
	{
		return mWifiManager;
	}

	public boolean isConnectedToNetwork(NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
	{
		return getWifiManager().getConnectionInfo() != null
				&& hotspotNetwork.BSSID.equals(getWifiManager().getConnectionInfo().getBSSID());
	}

	public boolean isMobileDataActive()
	{
		return mConnectivityManager.getActiveNetworkInfo() != null
				&& mConnectivityManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;
	}

	public boolean isConnectionSelfNetwork()
	{
		WifiInfo wifiInfo = getWifiManager().getConnectionInfo();

		return wifiInfo != null
				&& wifiInfo.getSSID() != null
				&& wifiInfo.getSSID().replace("\"", "").startsWith(AppConfig.ACCESS_POINT_PREFIX);
	}

	public void requestRefresh()
	{
		getWifiManager().startScan();

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

	public void showConnectionOptions()
	{
		if (getWifiManager().isWifiEnabled()) {
			if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
				createSnackbar(R.string.mesg_locationPermissionRequiredSelfHotspot)
						.setAction(R.string.butn_locationSettings, new View.OnClickListener()
						{
							@Override
							public void onClick(View view)
							{
								if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
									getActivity().requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
											Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
								}
							}
						})
						.show();
			} else {
				LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
						&& !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
						&& !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
					createSnackbar(R.string.mesg_locationDisabledSelfHotspot)
							.setAction(R.string.butn_locationSettings, new View.OnClickListener()
							{
								@Override
								public void onClick(View view)
								{
									startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
								}
							})
							.show();
				else
					createSnackbar(R.string.mesg_scanningSelfHotspot)
							.setAction(R.string.butn_wifiSettings, new View.OnClickListener()
							{
								@Override
								public void onClick(View view)
								{
									startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
								}
							})
							.show();
			}
		} else {
			createSnackbar(R.string.mesg_suggestSelfHotspot)
					.setAction(R.string.butn_enable, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							getWifiManager().setWifiEnabled(true);
						}
					})
					.show();
		}
	}

	public boolean toggleConnection(NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
	{
		if (!isConnectedToNetwork(hotspotNetwork)) {
			WifiConfiguration wifiConfig = new WifiConfiguration();

			wifiConfig.SSID = String.format("\"%s\"", hotspotNetwork.SSID);
			wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);

			int netId = getWifiManager().addNetwork(wifiConfig);

			getWifiManager().disconnect();
			getWifiManager().enableNetwork(netId, true);
			getWifiManager().reconnect();

			return true;
		}

		getWifiManager().disableNetwork(getWifiManager().getConnectionInfo().getNetworkId());
		return false;
	}

	public void toggleHotspot()
	{
		if (!mHotspotUtils.isEnabled()
				|| AppUtils.getHotspotName(getActivity()).equals(mHotspotUtils.getConfiguration().SSID))
			createSnackbar(mHotspotUtils.isEnabled()
					? R.string.mesg_stoppingSelfHotspot
					: R.string.mesg_startingSelfHotspot)
					.show();

		AppUtils.startForegroundService(mFAB.getContext(), new Intent(mFAB.getContext(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_TOGGLE_HOTSPOT));

		mShowHotspotInfo = true;
	}

	public void updateHotspotState()
	{
		if (mFAB == null)
			return;

		boolean isEnabled = mHotspotUtils.isEnabled();

		mFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), isEnabled ? R.color.fabHotspotEnabled : R.color.fabHotspotDisabled)));

		if (mHotspotUtils.isEnabled()
				&& mShowHotspotInfo
				&& AppUtils.getHotspotName(getActivity()).equals(mHotspotUtils.getConfiguration().SSID)) {
			final Snackbar snackbar = createSnackbar(R.string.mesg_hotspotCreatedInfo, mHotspotUtils.getConfiguration().SSID, getAdapter().getFriendlySSID(mHotspotUtils.getConfiguration().SSID));

			snackbar.setAction(R.string.butn_gotIt, new View.OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					snackbar.dismiss();
				}
			});

			snackbar.setDuration(Snackbar.LENGTH_INDEFINITE)
					.show();

			mShowHotspotInfo = false;
		}
	}

	public void setOnListClickListener(AbsListView.OnItemClickListener listener)
	{
		mClickListener = listener;
	}

	private class StatusReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			checkRefreshing();

			if (DeviceScannerService.ACTION_SCAN_STARTED.equals(intent.getAction()) && intent.hasExtra(DeviceScannerService.EXTRA_SCAN_STATUS)) {
				String scanStatus = intent.getStringExtra(DeviceScannerService.EXTRA_SCAN_STATUS);

				if (DeviceScannerService.STATUS_OK.equals(scanStatus) || DeviceScannerService.SCANNER_NOT_AVAILABLE.equals(scanStatus)) {
					boolean selfNetwork = isConnectionSelfNetwork();

					if (!selfNetwork)
						createSnackbar(DeviceScannerService.STATUS_OK.equals(scanStatus) ?
								R.string.mesg_scanningDevices
								: R.string.mesg_stillScanning)
								.show();
					else
						createSnackbar(R.string.mesg_scanningDevicesSelfHotspot)
								.setAction(R.string.butn_disconnect, new View.OnClickListener()
								{
									@Override
									public void onClick(View v)
									{
										getWifiManager().disconnect();
									}
								})
								.show();
				} else if (DeviceScannerService.STATUS_NO_NETWORK_INTERFACE.equals(scanStatus))
					showConnectionOptions();
			} else if (DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED.equals(intent.getAction())) {
				createSnackbar(R.string.mesg_scanCompleted)
						.show();
			} else if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(intent.getAction())
					|| (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_DEVICES)
			))
				refreshList();
			else if (NetworkStatusReceiver.WIFI_AP_STATE_CHANGED.equals(intent.getAction()))
				updateHotspotState();
			else if (WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
					&& WifiManager.WIFI_STATE_ENABLED == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1))
				requestRefresh();
		}
	}
}
