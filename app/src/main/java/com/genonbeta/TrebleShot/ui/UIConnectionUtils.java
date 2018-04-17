package com.genonbeta.TrebleShot.ui;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.WorkerService;
import com.genonbeta.TrebleShot.ui.callback.SnackbarSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.ConnectionUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;

/**
 * created by: veli
 * date: 15/04/18 18:44
 */
public class UIConnectionUtils
{
	public static final String TAG = "UIConnectionUtils";
	public static final int WORKER_TASK_CONNECT_TS_NETWORK = 1;

	private SnackbarSupport mSnackbarSupport;
	private boolean mShowHotspotInfo = false;
	private boolean mWirelessEnableRequested = false;
	private ConnectionUtils mConnectionUtils;

	public UIConnectionUtils(ConnectionUtils connectionUtils, SnackbarSupport snackbarSupport)
	{
		mConnectionUtils = connectionUtils;
		mSnackbarSupport = snackbarSupport;
	}

	public ConnectionUtils getConnectionUtils()
	{
		return mConnectionUtils;
	}

	public SnackbarSupport getSnackbarSupport()
	{
		return mSnackbarSupport;
	}

	public void makeAcquaintance(final AccessDatabase database, final UITask task,
								 final Object object, final int accessPin,
								 final NetworkDeviceLoader.OnDeviceRegisteredListener registerListener)
	{
		WorkerService.RunningTask runningTask = new WorkerService.RunningTask(TAG, WORKER_TASK_CONNECT_TS_NETWORK)
		{
			private boolean mConnected = false;
			private String mRemoteAddress;

			@Override
			public void onRun()
			{
				if (object instanceof NetworkDeviceListAdapter.HotspotNetwork) {
					mRemoteAddress = getConnectionUtils().establishHotspotConnection(getInterrupter(), (NetworkDeviceListAdapter.HotspotNetwork) object, new ConnectionUtils.TimeoutListener()
					{
						@Override
						public boolean onTimePassed(int delimiter, long timePassed)
						{
							return timePassed >= 20000;
						}
					});
				} else if (object instanceof String)
					mRemoteAddress = (String) object;

				if (mRemoteAddress != null) {
					mConnected = getConnectionUtils().setupConnection(database, mRemoteAddress, accessPin, new NetworkDeviceLoader.OnDeviceRegisteredListener()
					{
						@Override
						public void onDeviceRegistered(final AccessDatabase database, NetworkDevice device, final NetworkDevice.Connection connection)
						{
							// we may be working with direct IP scan
							if (object instanceof NetworkDeviceListAdapter.HotspotNetwork) {
								try {
									NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork = (NetworkDeviceListAdapter.HotspotNetwork) object;

									hotspotNetwork.deviceId = device.deviceId;
									database.reconstruct(hotspotNetwork);

									device = hotspotNetwork;
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							final NetworkDevice finalDevice = device;

							new Handler(Looper.getMainLooper()).post(new Runnable()
							{
								@Override
								public void run()
								{
									if (registerListener != null)
										registerListener.onDeviceRegistered(database, finalDevice, connection);
								}
							});
						}
					}) != null;
				}

				if (!mConnected)
					mSnackbarSupport.createSnackbar(R.string.mesg_connectionFailure)
							.show();

				new Handler(Looper.getMainLooper()).post(new Runnable()
				{
					@Override
					public void run()
					{
						task.updateTaskStopped();
					}
				});
				// We can't add dialog outside of the else statement as it may close other dialogs as well
			}
		};

		task.updateTaskStarted(runningTask.getInterrupter());
		WorkerService.run(getConnectionUtils().getContext(), runningTask);
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
		if (!getConnectionUtils().getWifiManager().isWifiEnabled())
			getSnackbarSupport().createSnackbar(R.string.mesg_suggestSelfHotspot)
					.setAction(R.string.butn_enable, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							mWirelessEnableRequested = true;
							getConnectionUtils().getWifiManager().setWifiEnabled(true);
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
							getConnectionUtils().getContext().startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
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
					&& !Settings.System.canWrite(getConnectionUtils().getContext())) {
				getSnackbarSupport().createSnackbar(R.string.mesg_errorHotspotPermission)
						.setDuration(Snackbar.LENGTH_LONG)
						.setAction(R.string.butn_settings, new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								getConnectionUtils().getContext().startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
										.setData(Uri.parse("package:" + getConnectionUtils().getContext().getPackageName()))
										.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
							}
						})
						.show();

				return false;
			} else if (Build.VERSION.SDK_INT < 26
					&& !getConnectionUtils().getHotspotUtils().isEnabled()
					&& getConnectionUtils().isMobileDataActive()) {
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

		WifiConfiguration wifiConfiguration = getConnectionUtils().getHotspotUtils().getConfiguration();

		if (!getConnectionUtils().getHotspotUtils().isEnabled()
				|| (wifiConfiguration != null && AppUtils.getHotspotName(getConnectionUtils().getContext()).equals(wifiConfiguration.SSID)))
			getSnackbarSupport().createSnackbar(getConnectionUtils().getHotspotUtils().isEnabled()
					? R.string.mesg_stoppingSelfHotspot
					: R.string.mesg_startingSelfHotspot)
					.show();

		AppUtils.startForegroundService(getConnectionUtils().getContext(), new Intent(getConnectionUtils().getContext(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_TOGGLE_HOTSPOT));

		mShowHotspotInfo = true;

		return true;
	}

	public boolean validateLocationPermission(final FragmentActivity activity, final int requestId)
	{
		if (Build.VERSION.SDK_INT < 23)
			return true;

		if (!getConnectionUtils().hasLocationPermission(getConnectionUtils().getContext())) {
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

		if (!getConnectionUtils().isLocationServiceEnabled()) {
			getSnackbarSupport().createSnackbar(R.string.mesg_locationDisabledSelfHotspot)
					.setAction(R.string.butn_locationSettings, new View.OnClickListener()
					{
						@Override
						public void onClick(View view)
						{
							getConnectionUtils().getContext().startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
						}
					})
					.show();

			return false;
		}

		return true;
	}
}
