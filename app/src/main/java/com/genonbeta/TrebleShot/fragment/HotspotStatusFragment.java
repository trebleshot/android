package com.genonbeta.TrebleShot.fragment;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Fragment;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.receiver.NetworkStatusReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
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
public class HotspotStatusFragment
		extends Fragment
		implements TitleSupport
{
	public static final int REQUEST_LOCATION_PERMISSION = 643;

	private boolean mShowHotspotInfo = false;
	private IntentFilter mIntentFilter = new IntentFilter();
	private StatusReceiver mStatusReceiver = new StatusReceiver();
	private HotspotUtils mHotspotUtils;
	private LocationManager mLocationManager;
	private ConnectivityManager mConnectivityManager;

	private ImageView mCodeImageView;
	private TextView mInfoTextView;
	private TextView mNameTextView;
	private TextView mPassTextView;
	private AppCompatButton mToggleButton;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		mHotspotUtils = HotspotUtils.getInstance(getContext());
		mConnectivityManager = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
		mLocationManager = (LocationManager) getContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

		mIntentFilter.addAction(CommunicationService.ACTION_HOTSPOT_STATUS);
		mIntentFilter.addAction(NetworkStatusReceiver.WIFI_AP_STATE_CHANGED);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		View view = getLayoutInflater().inflate(R.layout.layout_hotspot_status, container, false);

		mCodeImageView = view.findViewById(R.id.layout_hotspot_status_qr_image);
		mInfoTextView = view.findViewById(R.id.layout_hotspot_status_info_container_info_text);
		mNameTextView = view.findViewById(R.id.layout_hotspot_status_info_container_network_text);
		mPassTextView = view.findViewById(R.id.layout_hotspot_status_info_container_password_text);
		mToggleButton = view.findViewById(R.id.layout_hotspot_status_info_toggle_button);

		mToggleButton.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				toggleHotspot(true);
			}
		});

		return view;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getContext().registerReceiver(mStatusReceiver, mIntentFilter);

		if (Build.VERSION.SDK_INT >= 26)
			AppUtils.startForegroundService(getActivity(),
					new Intent(getActivity(), CommunicationService.class)
							.setAction(CommunicationService.ACTION_REQUEST_HOTSPOT_STATUS));
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getContext().unregisterReceiver(mStatusReceiver);
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_hotspot);
	}

	public boolean hasLocationPermission(Context context)
	{
		return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
	}

	public boolean isLocationServiceEnabled()
	{
		return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
	}

	public boolean isMobileDataActive()
	{
		return mConnectivityManager.getActiveNetworkInfo() != null
				&& mConnectivityManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;
	}

	public boolean toggleHotspot(boolean conditional)
	{
		if (!HotspotUtils.isSupported())
			return false;

		if (conditional) {
			if (Build.VERSION.SDK_INT >= 26 && !validateLocationPermission())
				return false;

			if (Build.VERSION.SDK_INT >= 23
					&& !Settings.System.canWrite(getContext())) {
				createSnackbar(R.string.mesg_errorHotspotPermission)
						.setDuration(Snackbar.LENGTH_LONG)
						.setAction(R.string.butn_settings, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
										.setData(Uri.parse("package:" + getContext().getPackageName()))
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

		AppUtils.startForegroundService(getContext(), new Intent(getContext(), CommunicationService.class)
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

			if (networkName != null) {
				mInfoTextView.setText(R.string.text_qrCodeAvailableHelp);

				int scaleUsing = getView().getWidth() > getView().getHeight()
						? getView().getHeight()
						: getView().getWidth();

				if (scaleUsing == 0)
					scaleUsing = 200;

				int scaledSize = (int) (scaleUsing / 1.7);

				BitMatrix bitMatrix = formatWriter.encode(text, BarcodeFormat.QR_CODE, scaledSize, scaledSize);
				BarcodeEncoder encoder = new BarcodeEncoder();
				Bitmap bitmap = encoder.createBitmap(bitMatrix);

				mCodeImageView.setImageBitmap(bitmap);

				mNameTextView.setText(networkName);
				mPassTextView.setText(password == null ? "-" : password);
			} else {
				mCodeImageView.setImageResource(R.drawable.ic_qrcode_grey600_48dp);
				mInfoTextView.setText(R.string.text_qrCodeHotspotDisabledHelp);
				mNameTextView.setText(R.string.text_unknown);
				mPassTextView.setText(R.string.text_unknown);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void updateHotspotState()
	{
		boolean isEnabled = mHotspotUtils.isEnabled();

		mToggleButton.setText(isEnabled ? R.string.butn_disconnect : R.string.butn_connect);

		WifiConfiguration wifiConfiguration = mHotspotUtils.getConfiguration();

		if (!isEnabled)
			updateQRViews(null, null, 0);
		else if (mHotspotUtils instanceof HotspotUtils.HackAPI
				&& wifiConfiguration != null)
			updateQRViews(wifiConfiguration.SSID, wifiConfiguration.preSharedKey, NetworkUtils.getAllowedKeyManagement(wifiConfiguration));

		if (wifiConfiguration != null
				&& mHotspotUtils.isEnabled()) {
			if (mShowHotspotInfo
					&& AppUtils.getHotspotName(getActivity()).equals(wifiConfiguration.SSID)) {
				final Snackbar snackbar = createSnackbar(R.string.mesg_hotspotCreatedInfo, mHotspotUtils.getConfiguration().SSID, AppUtils.getFriendlySSID(mHotspotUtils.getConfiguration().SSID));

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
			if (NetworkStatusReceiver.WIFI_AP_STATE_CHANGED.equals(intent.getAction()))
				updateHotspotState();
			else if (CommunicationService.ACTION_HOTSPOT_STATUS.equals(intent.getAction())
					&& intent.getBooleanExtra(CommunicationService.EXTRA_HOTSPOT_ENABLED, false)) {
				updateQRViews(intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_NAME),
						intent.getStringExtra(CommunicationService.EXTRA_HOTSPOT_PASSWORD),
						intent.getIntExtra(CommunicationService.EXTRA_HOTSPOT_KEY_MGMT, 0));
			}
		}
	}
}
