package com.genonbeta.TrebleShot.ui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.SnackbarSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;

/**
 * created by: veli
 * date: 15/04/18 18:44
 */
public class UIConnectionUtils extends ConnectionUtils
{
	private SnackbarSupport mSnackbarSupport;
	private boolean mShowHotspotInfo = false;
	private boolean mWirelessEnableRequested = false;

	public UIConnectionUtils(Context context, SnackbarSupport snackbarSupport)
	{
		super(context);
		mSnackbarSupport = snackbarSupport;
	}

	public SnackbarSupport getSnackbarSupport()
	{
		return mSnackbarSupport;
	}

	public boolean notifyShowHotspotHandled()
	{
		boolean returnedState = mShowHotspotInfo;

		mShowHotspotInfo = false;

		return returnedState;
	}

	public boolean notifyWirelessRequestHandled()
	{
		boolean returnedState = mWirelessEnableRequested;

		mWirelessEnableRequested = false;

		return returnedState;
	}

	public void showConnectionOptions(FragmentActivity activity, int locationPermRequestId)
	{
		if (!getWifiManager().isWifiEnabled())
			getSnackbarSupport().createSnackbar(R.string.mesg_suggestSelfHotspot)
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
		else if (validateLocationPermission(activity, locationPermRequestId))
			getSnackbarSupport().createSnackbar(R.string.mesg_scanningSelfHotspot)
					.setAction(R.string.butn_wifiSettings, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
						}
					})
					.show();
	}

	public boolean toggleHotspot(boolean conditional, final FragmentActivity activity, final int locationPermRequestId)
	{
		if (!HotspotUtils.isSupported())
			return false;

		if (conditional) {
			if (Build.VERSION.SDK_INT >= 26 && !validateLocationPermission(activity, locationPermRequestId))
				return false;

			if (Build.VERSION.SDK_INT >= 23
					&& !Settings.System.canWrite(getContext())) {
				getSnackbarSupport().createSnackbar(R.string.mesg_errorHotspotPermission)
						.setDuration(Snackbar.LENGTH_LONG)
						.setAction(R.string.butn_settings, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								getContext().startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
										.setData(Uri.parse("package:" + getContext().getPackageName()))
										.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
							}
						})
						.show();

				return false;
			} else if (Build.VERSION.SDK_INT < 26
					&& !getHotspotUtils().isEnabled()
					&& isMobileDataActive()) {
				getSnackbarSupport().createSnackbar(R.string.mesg_warningHotspotMobileActive)
						.setDuration(Snackbar.LENGTH_LONG)
						.setAction(R.string.butn_skip, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								toggleHotspot(false, activity, locationPermRequestId);
							}
						})
						.show();

				return false;
			}
		}

		WifiConfiguration wifiConfiguration = getHotspotUtils().getConfiguration();

		if (!getHotspotUtils().isEnabled()
				|| (wifiConfiguration != null && AppUtils.getHotspotName(getContext()).equals(wifiConfiguration.SSID)))
			getSnackbarSupport().createSnackbar(getHotspotUtils().isEnabled()
					? R.string.mesg_stoppingSelfHotspot
					: R.string.mesg_startingSelfHotspot)
					.show();

		AppUtils.startForegroundService(getContext(), new Intent(getContext(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_TOGGLE_HOTSPOT));

		mShowHotspotInfo = true;

		return true;
	}

	public boolean validateLocationPermission(final FragmentActivity activity, final int requestId)
	{
		if (Build.VERSION.SDK_INT < 23)
			return true;

		if (!hasLocationPermission(getContext())) {
			getSnackbarSupport().createSnackbar(R.string.mesg_locationPermissionRequiredSelfHotspot)
					.setAction(R.string.butn_locationSettings, new View.OnClickListener()
					{
						@RequiresApi(api = Build.VERSION_CODES.M)
						@Override
						public void onClick(View view)
						{
								activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
										Manifest.permission.ACCESS_COARSE_LOCATION}, requestId);
						}
					})
					.show();

			return false;
		}

		if (!isLocationServiceEnabled()) {
			getSnackbarSupport().createSnackbar(R.string.mesg_locationDisabledSelfHotspot)
					.setAction(R.string.butn_locationSettings, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						}
					})
					.show();

			return false;
		}

		return true;
	}
}
