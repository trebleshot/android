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
import android.graphics.Bitmap;
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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.app.ListFragment;
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
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

import static junit.framework.Assert.fail;

public class NetworkDeviceListFragment
		extends ListFragment<NetworkDevice, NetworkDeviceListAdapter>
		implements TitleSupport, FABSupport, DetachListener
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
	private boolean mWirelessEnableRequested = false;

	private ImageView mCodeImageView;
	private TextView mNetworkInfoTextView;
	private TextView mNetworkNameTextView;
	private TextView mNetworkPassTextView;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		mPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mHotspotUtils = HotspotUtils.getInstance(getContext());
		mWifiManager = mHotspotUtils.getWifiManager();
		mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);

		mIntentFilter.addAction(DeviceScannerService.ACTION_SCAN_STARTED);
		mIntentFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
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
		final View qrLayout = getLayoutInflater().inflate(R.layout.layout_illustrate_qr_code, getCustomViewContainer());

		mCodeImageView = qrLayout.findViewById(R.id.layout_illustrate_qr_code_qr_image);
		mNetworkInfoTextView = qrLayout.findViewById(R.id.layout_illustrate_qr_code_info_container_info_text);
		mNetworkNameTextView = qrLayout.findViewById(R.id.layout_illustrate_qr_code_info_container_network_text);
		mNetworkPassTextView = qrLayout.findViewById(R.id.layout_illustrate_qr_code_info_container_password_text);
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

		applyDefaultEmptyText();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		setHasOptionsMenu(true);
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

		if (Build.VERSION.SDK_INT >= 26)
			AppUtils.startForegroundService(getActivity(),
					new Intent(getActivity(), CommunicationService.class)
							.setAction(CommunicationService.ACTION_REQUEST_HOTSPOT_STATUS));
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

		MenuItem generateQR = menu.findItem(R.id.network_devices_barcode_generate);

		if (generateQR != null)
			generateQR.setVisible(mFAB != null);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId()) {
			case R.id.network_devices_scan:
				requestRefresh();
				return true;
			case R.id.network_devices_barcode_generate:
				toggleCustomView();
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
				toggleHotspot(true);
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

	@Override
	public void onPrepareDetach()
	{
		showCustomView(false);
	}

	public void applyDefaultEmptyText()
	{
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
	}

	public void checkRefreshing()
	{
		mSwipeRefreshLayout.setRefreshing(!DeviceScannerService
				.getDeviceScanner()
				.isScannerAvailable());
	}

	public boolean checkLocationPermission()
	{
		if (Build.VERSION.SDK_INT < 23)
			return false;

		if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

		LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);

		if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
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

	public boolean isConnectionSelfNetwork()
	{
		WifiInfo wifiInfo = getWifiManager().getConnectionInfo();

		return wifiInfo != null
				&& getCleanNetworkName(wifiInfo.getSSID()).startsWith(AppConfig.PREFIX_ACCESS_POINT);
	}

	public boolean isConnectedToNetwork(NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
	{
		if (getWifiManager().getConnectionInfo() == null)
			return false;

		if (hotspotNetwork.BSSID != null)
			return hotspotNetwork.BSSID.equals(getWifiManager().getConnectionInfo().getBSSID());

		return hotspotNetwork.SSID.equals(getCleanNetworkName(getWifiManager().getConnectionInfo().getSSID()));
	}

	public boolean isMobileDataActive()
	{
		return mConnectivityManager.getActiveNetworkInfo() != null
				&& mConnectivityManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;
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
		else if (checkLocationPermission())
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
			getWifiManager().reconnect();

			return true;
		}

		getWifiManager().disableNetwork(getWifiManager().getConnectionInfo().getNetworkId());

		return false;
	}

	public boolean toggleHotspot(boolean conditional)
	{
		if (!HotspotUtils.isSupported())
			return false;

		if (conditional) {
			if (Build.VERSION.SDK_INT >= 26 && !checkLocationPermission())
				return false;

			if (Build.VERSION.SDK_INT >= 23
					&& !Settings.System.canWrite(mFAB.getContext())) {
				createSnackbar(R.string.mesg_errorHotspotPermission)
						.setDuration(Snackbar.LENGTH_LONG)
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

				return false;
			} else if (Build.VERSION.SDK_INT < 26
					&& !mHotspotUtils.isEnabled()
					&& isMobileDataActive()) {
				createSnackbar(R.string.mesg_warningHotspotMobileActive)
						.setDuration(Snackbar.LENGTH_LONG)
						.setAction(R.string.butn_skip, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								toggleHotspot(false);
							}
						})
						.show();

				return false;
			}
		}

		WifiConfiguration wifiConfiguration = mHotspotUtils.getConfiguration();

		if (!mHotspotUtils.isEnabled()
				|| (wifiConfiguration != null && AppUtils.getHotspotName(getActivity()).equals(wifiConfiguration.SSID)))
			createSnackbar(mHotspotUtils.isEnabled()
					? R.string.mesg_stoppingSelfHotspot
					: R.string.mesg_startingSelfHotspot)
					.show();

		AppUtils.startForegroundService(mFAB.getContext(), new Intent(mFAB.getContext(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_TOGGLE_HOTSPOT));

		mShowHotspotInfo = true;

		return true;
	}

	public void updateQRViews(String networkName, String password, int keyManagement)
	{
		try {
			MultiFormatWriter formatWriter = new MultiFormatWriter();

			String text = new JSONObject()
					.put(Keyword.NETWORK_NAME, networkName)
					.put(Keyword.NETWORK_PASSWORD, password)
					.put(Keyword.NETWORK_KEYMGMT, keyManagement)
					.toString();

			showCustomView(networkName != null && mFAB != null);

			if (networkName != null) {
				mNetworkInfoTextView.setText(R.string.text_qrCodeAvailableHelp);

				int scaleUsing = getDefaultViewContainer().getWidth() > getDefaultViewContainer().getHeight()
						? getDefaultViewContainer().getHeight()
						: getDefaultViewContainer().getWidth();

				int scaledSize = (int) (scaleUsing / 1.5);

				BitMatrix bitMatrix = formatWriter.encode(text, BarcodeFormat.QR_CODE, scaledSize, scaledSize);
				BarcodeEncoder encoder = new BarcodeEncoder();
				Bitmap bitmap = encoder.createBitmap(bitMatrix);

				mCodeImageView.setImageBitmap(bitmap);

				mNetworkNameTextView.setText(networkName);
				mNetworkPassTextView.setText(password == null ? "-" : password);
			} else {
				mCodeImageView.setImageResource(R.drawable.ic_qrcode_grey600_48dp);
				mNetworkInfoTextView.setText(R.string.text_qrCodeHotspotDisabledHelp);
				mNetworkNameTextView.setText(R.string.text_unknown);
				mNetworkPassTextView.setText(R.string.text_unknown);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateHotspotState()
	{
		if (mFAB == null)
			return;

		boolean isEnabled = mHotspotUtils.isEnabled();

		mFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), isEnabled ? R.color.fabHotspotEnabled : R.color.fabHotspotDisabled)));

		WifiConfiguration wifiConfiguration = mHotspotUtils.getConfiguration();

		if (mHotspotUtils instanceof HotspotUtils.HackAPI) {
			if (wifiConfiguration != null && isEnabled)
				updateQRViews(wifiConfiguration.SSID, wifiConfiguration.preSharedKey, NetworkUtils.getAllowedKeyManagement(wifiConfiguration));
			else
				updateQRViews(null, null, 0);
		}

		if (wifiConfiguration != null
				&& mHotspotUtils.isEnabled()) {
			if (mShowHotspotInfo
					&& AppUtils.getHotspotName(getActivity()).equals(wifiConfiguration.SSID)) {
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
			else if (CommunicationService.ACTION_HOTSPOT_STATUS.equals(intent.getAction())) {
				updateQRViews(intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_NAME),
						intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_PASSWORD),
						intent.getIntExtra(CommunicationService.EXTRA_HOTSPOT_KEY_MGMT, 0));
			} else if (mWirelessEnableRequested
					&& WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
					&& WifiManager.WIFI_STATE_ENABLED == intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1)) {
				mWirelessEnableRequested = false;
				requestRefresh();
			}
		}
	}
}
