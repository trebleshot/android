package com.genonbeta.TrebleShot.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.RecyclerViewFragment;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.receiver.NetworkStatusReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.DeviceScannerService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DetachListener;
import com.genonbeta.TrebleShot.util.FABSupport;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.NsdDiscovery;
import com.genonbeta.TrebleShot.util.TitleSupport;
import com.genonbeta.TrebleShot.widget.RecyclerViewAdapter;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import static junit.framework.Assert.fail;

public class NetworkDeviceListFragment
		extends RecyclerViewFragment<NetworkDevice, RecyclerViewAdapter.ViewHolder, NetworkDeviceListAdapter>
		implements TitleSupport, DetachListener
{
	public static final int REQUEST_LOCATION_PERMISSION = 643;

	private NsdDiscovery mNsdDiscovery;
	private RecyclerViewAdapter.OnClickListener mClickListener;
	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();
	private SwipeRefreshLayout mSwipeRefreshLayout;
	private WifiManager mWifiManager;
	private LocationManager mLocationManager;
	private boolean mWirelessEnableRequested = false;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		mLocationManager = (LocationManager) getContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

		mIntentFilter.addAction(DeviceScannerService.ACTION_SCAN_STARTED);
		mIntentFilter.addAction(DeviceScannerService.ACTION_DEVICE_SCAN_COMPLETED);
		mIntentFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
		mIntentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);

		mNsdDiscovery = new NsdDiscovery(getContext(), getDatabase(), getDefaultPreferences());
	}

	@Override
	protected RecyclerView onListView(View mainContainer, ViewGroup listViewContainer)
	{
		Context context = mainContainer.getContext();

		mSwipeRefreshLayout = new SwipeRefreshLayout(context);

		mSwipeRefreshLayout.setColorSchemeColors(ContextCompat.getColor(context, R.color.colorPrimary),
				ContextCompat.getColor(context, R.color.colorAccent));

		mSwipeRefreshLayout.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

		listViewContainer.addView(mSwipeRefreshLayout);

		return super.onListView(mainContainer, mSwipeRefreshLayout);
	}

	@Override
	public void onViewCreated(@NonNull View view, final Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_devices_white_24dp);
		setEmptyText(getString(R.string.text_findDevicesHint));

		useEmptyActionButton(getString(R.string.butn_scan), new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				requestRefresh();
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

		if (getDefaultPreferences().getBoolean("scan_devices_auto", false))
			requestRefresh();
	}

	@Override
	public NetworkDeviceListAdapter onAdapter()
	{
		final AppUtils.QuickActions<RecyclerViewAdapter.ViewHolder> quickActions = new AppUtils.QuickActions<RecyclerViewAdapter.ViewHolder>()
		{
			@Override
			public void onQuickActions(final RecyclerViewAdapter.ViewHolder clazz)
			{
				clazz.getView().setOnClickListener(new View.OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						final NetworkDevice device = getAdapter().getList().get(clazz.getAdapterPosition());

						if (mClickListener != null) {
							if (device.versionNumber != -1
									&& device.versionNumber < AppConfig.SUPPORTED_MIN_VERSION)
								createSnackbar(R.string.mesg_versionNotSupported)
										.show();
							else
								mClickListener.onClick(clazz);
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
							new DeviceInfoDialog(getActivity(), getDatabase(), getDefaultPreferences(), device).show();
					}
				});
			}
		};

		return new NetworkDeviceListAdapter(this, getDatabase(), getDefaultPreferences())
		{
			@NonNull
			@Override
			public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
			}
		};
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
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (REQUEST_LOCATION_PERMISSION == requestCode)
			showConnectionOptions();
	}

	@Override
	public void onPrepareDetach()
	{
		showCustomView(false);
	}

	public boolean canReadScanResults(Context context)
	{
		return getWifiManager().isWifiEnabled()
				&& (Build.VERSION.SDK_INT < 23 || (hasLocationPermission(context) && isLocationServiceEnabled()));
	}

	public void checkRefreshing()
	{
		mSwipeRefreshLayout.setRefreshing(!DeviceScannerService
				.getDeviceScanner()
				.isScannerAvailable());
	}

	public boolean disableCurrentNetwork()
	{
		if (!isConnectedToAnyNetwork())
			return false;

		return getWifiManager().disableNetwork(getWifiManager().getConnectionInfo().getNetworkId());
	}

	public String getCleanNetworkName(String networkName)
	{
		if (networkName == null)
			return "";

		return networkName.replace("\"", "");
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

	public boolean hasLocationPermission(Context context)
	{
		return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}

	public boolean isConnectionSelfNetwork()
	{
		WifiInfo wifiInfo = getWifiManager().getConnectionInfo();

		return wifiInfo != null
				&& getCleanNetworkName(wifiInfo.getSSID()).startsWith(AppConfig.PREFIX_ACCESS_POINT);
	}

	public boolean isConnectedToAnyNetwork()
	{
		return getWifiManager().getConnectionInfo() != null;
	}

	public boolean isConnectedToNetwork(NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
	{
		if (!isConnectedToAnyNetwork())
			return false;

		if (hotspotNetwork.BSSID != null)
			return hotspotNetwork.BSSID.equals(getWifiManager().getConnectionInfo().getBSSID());

		return hotspotNetwork.SSID.equals(getCleanNetworkName(getWifiManager().getConnectionInfo().getSSID()));
	}

	public boolean isLocationServiceEnabled()
	{
		return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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
		if (!getWifiManager().isWifiEnabled())
			createSnackbar(R.string.mesg_suggestSelfHotspot)
					.setAction(R.string.butn_enable, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							mWirelessEnableRequested = true;
							getWifiManager().setWifiEnabled(true);
						}
					})
					.show();
		else if (validateLocationPermission())
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

	public boolean toggleConnection(NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
	{
		if (!isConnectedToNetwork(hotspotNetwork)) {
			if (isConnectedToAnyNetwork())
				disableCurrentNetwork();

			WifiConfiguration config = new WifiConfiguration();

			config.SSID = String.format("\"%s\"", hotspotNetwork.SSID);

			switch (hotspotNetwork.keyManagement) {
				case 0: // OPEN
					config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					break;
				case 1: // WEP64
					config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
					config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
					config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

					if (hotspotNetwork.password != null
							&& hotspotNetwork.password.matches("[0-9A-Fa-f]*")) {
						config.wepKeys[0] = hotspotNetwork.password;
					} else {
						fail("Please type hex pair for the password");
					}
					break;
				case 2: // WEP128
					config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
					config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
					config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
					config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

					if (hotspotNetwork.password != null
							&& hotspotNetwork.password.matches("[0-9A-Fa-f]*")) {
						config.wepKeys[0] = hotspotNetwork.password;
					} else {
						fail("Please type hex pair for the password");
					}
					break;
				case 3: // WPA_TKIP
					config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
					config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
					config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
					config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
					config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

					if (hotspotNetwork.password != null
							&& hotspotNetwork.password.matches("[0-9A-Fa-f]{64}")) {
						config.preSharedKey = hotspotNetwork.password;
					} else {
						config.preSharedKey = '"' + hotspotNetwork.password + '"';
					}
					break;
				case 4: // WPA2_AES
					config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
					config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
					config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
					config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
					config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
					config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

					if (hotspotNetwork.password != null
							&& hotspotNetwork.password.matches("[0-9A-Fa-f]{64}")) {
						config.preSharedKey = hotspotNetwork.password;
					} else {
						config.preSharedKey = '"' + hotspotNetwork.password + '"';
					}
					break;
			}

			int netId = getWifiManager().addNetwork(config);

			getWifiManager().disconnect();
			getWifiManager().enableNetwork(netId, true);

			return getWifiManager().reconnect();
		}

		disableCurrentNetwork();

		return false;
	}

	public void setOnListClickListener(RecyclerViewAdapter.OnClickListener listener)
	{
		mClickListener = listener;
	}

	public boolean validateLocationPermission()
	{
		if (Build.VERSION.SDK_INT < 23)
			return true;

		if (!hasLocationPermission(getContext())) {
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

			return false;
		}

		if (!isLocationServiceEnabled()) {
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

			return false;
		}

		return true;
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
					&& AccessDatabase.TABLE_DEVICES.equals(intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME))
			))
				refreshList();
			else if (mWirelessEnableRequested
					&& WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
					&& WifiManager.WIFI_STATE_ENABLED == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)) {
				mWirelessEnableRequested = false;
				requestRefresh();
			}
		}
	}
}
