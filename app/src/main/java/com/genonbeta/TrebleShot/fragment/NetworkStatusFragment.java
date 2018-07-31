package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.GlideApp;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.receiver.NetworkStatusReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.UIConnectionUtils;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import org.json.JSONObject;

/**
 * created by: veli
 * date: 11/04/18 20:53
 */
public class NetworkStatusFragment
		extends com.genonbeta.android.framework.app.Fragment
		implements TitleSupport, SnackbarSupport, com.genonbeta.android.framework.app.FragmentImpl
{
	public static final int REQUEST_LOCATION_PERMISSION = 643;

	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();
	private UIConnectionUtils mConnectionUtils;

	private View mContainerText1;
	private View mContainerText2;
	private View mContainerText3;
	private ImageView mTextIcon1;
	private ImageView mTextIcon2;
	private ImageView mTextIcon3;
	private TextView mText1;
	private TextView mText2;
	private TextView mText3;
	private ImageView mCodeView;
	private AppCompatButton mToggleButton;
	private NetworkDeviceSelectedListener mDeviceSelectedListener;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mIntentFilter.addAction(CommunicationService.ACTION_DEVICE_ACQUAINTANCE);
		mIntentFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
		mIntentFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
		mIntentFilter.addAction(NetworkStatusReceiver.WIFI_AP_STATE_CHANGED);
		mIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
		mIntentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = getLayoutInflater().inflate(R.layout.layout_hotspot_status, container, false);

		mCodeView = view.findViewById(R.id.layout_hotspot_status_qr_image);
		mContainerText1 = view.findViewById(R.id.layout_hotspot_status_info_container_text1_container);
		mContainerText2 = view.findViewById(R.id.layout_hotspot_status_info_container_text2_container);
		mContainerText3 = view.findViewById(R.id.layout_hotspot_status_info_container_text3_container);
		mTextIcon1 = view.findViewById(R.id.layout_hotspot_status_info_container_text1_icon);
		mTextIcon2 = view.findViewById(R.id.layout_hotspot_status_info_container_text2_icon);
		mTextIcon3 = view.findViewById(R.id.layout_hotspot_status_info_container_text3_icon);
		mText1 = view.findViewById(R.id.layout_hotspot_status_info_container_text1);
		mText2 = view.findViewById(R.id.layout_hotspot_status_info_container_text2);
		mText3 = view.findViewById(R.id.layout_hotspot_status_info_container_text3);
		mToggleButton = view.findViewById(R.id.layout_hotspot_status_info_toggle_button);

		mToggleButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				getUIConnectionUtils().toggleHotspot(true, getActivity(), REQUEST_LOCATION_PERMISSION);
			}
		});

		return view;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
	{
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		if (REQUEST_LOCATION_PERMISSION == requestCode)
			getUIConnectionUtils().showConnectionOptions(getActivity(), REQUEST_LOCATION_PERMISSION);
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getContext().registerReceiver(mStatusReceiver, mIntentFilter);

		updateState();
	}

	@Override

	public void onPause()
	{
		super.onPause();
		getContext().unregisterReceiver(mStatusReceiver);
	}

	public ConnectionUtils getConnectionUtils()
	{
		return getUIConnectionUtils().getConnectionUtils();
	}

	public UIConnectionUtils getUIConnectionUtils()
	{
		if (mConnectionUtils == null)
			mConnectionUtils = new UIConnectionUtils(ConnectionUtils.getInstance(getContext()), this);

		return mConnectionUtils;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_network);
	}

	public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
	{
		mDeviceSelectedListener = listener;
	}

	public void updateViewsWithBlank()
	{
		updateViews(null, getString(R.string.text_qrCodeHotspotDisabledHelp), null, null);
	}

	// for connection addressing purpose
	public void updateViews(String networkName, String ipAddress, String bssid)
	{
		mTextIcon2.setImageResource(R.drawable.ic_wifi_black_24dp);
		mTextIcon3.setImageResource(R.drawable.ic_device_hub_white_24dp);

		try {
			JSONObject object = new JSONObject()
					.put(Keyword.NETWORK_ADDRESS_IP, ipAddress)
					.put(Keyword.NETWORK_ADDRESS_BSSID, bssid);

			updateViews(object, getString(R.string.text_easyDiscoveryHelp), networkName, ipAddress);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// for hotspot
	public void updateViews(String networkName, String password, int keyManagement)
	{
		mTextIcon2.setImageResource(R.drawable.ic_wifi_tethering_white_24dp);
		mTextIcon3.setImageResource(R.drawable.ic_vpn_key_black_24dp);

		try {
			JSONObject object = new JSONObject()
					.put(Keyword.NETWORK_NAME, networkName)
					.put(Keyword.NETWORK_PASSWORD, password)
					.put(Keyword.NETWORK_KEYMGMT, keyManagement);

			updateViews(object, getString(R.string.text_qrCodeAvailableHelp), networkName, password);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateViews(@Nullable JSONObject codeIndex, @Nullable String text1, @Nullable String text2, @Nullable String text3)
	{
		try {
			if (codeIndex != null) {
				int networkPin = AppUtils.getUniqueNumber();

				codeIndex.put(Keyword.NETWORK_PIN, networkPin);

				AppUtils.getDefaultPreferences(getContext()).edit()
						.putInt(Keyword.NETWORK_PIN, networkPin)
						.apply();
			}

			MultiFormatWriter formatWriter = new MultiFormatWriter();
			String codeString = codeIndex == null ? null : codeIndex.toString();

			if (codeString != null && getContext() != null) {
				BitMatrix bitMatrix = formatWriter.encode(codeString, BarcodeFormat.QR_CODE, 400, 400);
				BarcodeEncoder encoder = new BarcodeEncoder();
				Bitmap bitmap = encoder.createBitmap(bitMatrix);

				GlideApp.with(getContext())
						.load(bitmap)
						.into(mCodeView);
			} else
				mCodeView.setImageBitmap(null);

			mContainerText1.setVisibility(text1 == null ? View.GONE : View.VISIBLE);
			mContainerText2.setVisibility(text2 == null ? View.GONE : View.VISIBLE);
			mContainerText3.setVisibility(text3 == null ? View.GONE : View.VISIBLE);

			mText1.setText(text1);
			mText2.setText(text2);
			mText3.setText(text3);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateState()
	{
		boolean isEnabled = getUIConnectionUtils().getConnectionUtils().getHotspotUtils().isEnabled();

		mToggleButton.setText(isEnabled ? R.string.butn_stopHotspot : R.string.butn_startHotspot);

		WifiConfiguration wifiConfiguration = getConnectionUtils().getHotspotUtils().getConfiguration();

		if (!isEnabled) {
			WifiInfo connectionInfo = getConnectionUtils().getWifiManager().getConnectionInfo();

			if (!getConnectionUtils().isConnectedToAnyNetwork())
				updateViewsWithBlank();
			else {
				updateViews(ConnectionUtils.getCleanNetworkName(connectionInfo.getSSID()),
						NetworkUtils.convertInet4Address(connectionInfo.getIpAddress()),
						connectionInfo.getBSSID());
			}
		} else {
			if (getConnectionUtils().getHotspotUtils() instanceof HotspotUtils.HackAPI
					&& wifiConfiguration != null)
				updateViews(wifiConfiguration.SSID, wifiConfiguration.preSharedKey, NetworkUtils.getAllowedKeyManagement(wifiConfiguration));
			else if (Build.VERSION.SDK_INT >= 26)
				AppUtils.startForegroundService(getActivity(),
						new Intent(getActivity(), CommunicationService.class)
								.setAction(CommunicationService.ACTION_REQUEST_HOTSPOT_STATUS));

			if (wifiConfiguration != null
					&& getConnectionUtils().getHotspotUtils().isEnabled()) {
				if (getUIConnectionUtils().notifyShowHotspotHandled()
						&& AppUtils.getHotspotName(getActivity()).equals(wifiConfiguration.SSID)) {
					final Snackbar snackbar = createSnackbar(R.string.mesg_hotspotCreatedInfo, getConnectionUtils().getHotspotUtils().getConfiguration().SSID, AppUtils.getFriendlySSID(getConnectionUtils().getHotspotUtils().getConfiguration().SSID));

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
				}
			}
		}
	}

	private class StatusReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (NetworkStatusReceiver.WIFI_AP_STATE_CHANGED.equals(intent.getAction())
					|| WifiManager.WIFI_STATE_CHANGED_ACTION.equals(intent.getAction())
					|| ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction()))
				updateState();
			else if (CommunicationService.ACTION_HOTSPOT_STATUS.equals(intent.getAction())
					&& intent.getBooleanExtra(CommunicationService.EXTRA_HOTSPOT_ENABLED, false)) {
				updateViews(intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_NAME),
						intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_PASSWORD),
						intent.getIntExtra(CommunicationService.EXTRA_HOTSPOT_KEY_MGMT, 0));
			} else if (CommunicationService.ACTION_DEVICE_ACQUAINTANCE.equals(intent.getAction())
					&& intent.hasExtra(CommunicationService.EXTRA_DEVICE_ID)
					&& intent.hasExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME)) {
				NetworkDevice device = new NetworkDevice(intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID));
				NetworkDevice.Connection connection = new NetworkDevice.Connection(device.deviceId, intent.getStringExtra(CommunicationService.EXTRA_CONNECTION_ADAPTER_NAME));

				try {
					AppUtils.getDatabase(getContext()).reconstruct(device);
					AppUtils.getDatabase(getContext()).reconstruct(connection);

					if (mDeviceSelectedListener != null)
						mDeviceSelectedListener.onNetworkDeviceSelected(device, connection);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
