package com.genonbeta.TrebleShot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.helper.ApplicationHelper;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NetworkDeviceInfoLoader;
import com.genonbeta.core.util.NetworkDeviceScanner;
import com.genonbeta.core.util.NetworkUtils;

import java.net.InetAddress;
import java.util.ArrayList;

public class DeviceScannerProvider extends BroadcastReceiver implements NetworkDeviceScanner.ScannerHandler, NetworkDeviceInfoLoader.OnInfoAvailableListener
{
	public static final String ACTION_SCAN_DEVICES = "genonbeta.intent.action.SCAN_DEVICES";
	public static final String ACTION_SCAN_STARTED = "genonbeta.intent.action.SCAN_STARTED";
	public static final String ACTION_DEVICE_FOUND = "genonbeta.intent.action.DEVICE_FOUND";
	public static final String ACTION_DEVICE_SCAN_COMPLETED = "genonbeta.intent.action.DEVICE_SCAN_COMPLETED";
	public static final String ACTION_ADD_IP = "genonbeta.intent.action.ADD_IP";

	public static final String EXTRA_DEVICE_IP = "genonbeta.intent.extra.DEVICE_IP";
	public static final String EXTRA_SCAN_STATUS = "genonbeta.intent.extra.SCAN_STATUS";
	public static final String EXTRA_NETWORK_INTERFACE_PREFIX = "genonbeta.intent.extra.NETWORK_INTERFACE_PREFIX";

	public static final String STATUS_OK = "genonbeta.intent.status.OK";
	public static final String STATUS_NO_NETWORK_INTERFACE = "genonbeta.intent.status.NO_NETWORK_INTERFACE";

	private Context mContext;
	private NetworkDeviceInfoLoader mInfoLoader = new NetworkDeviceInfoLoader(this);
	private SharedPreferences mPreferences;
	private DeviceRegistry mDeviceRegistry;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		mContext = context;
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
		mDeviceRegistry = new DeviceRegistry(context);

		if (ACTION_SCAN_DEVICES.equals(intent.getAction()))
		{
			if (ApplicationHelper.getNetworkDeviceScanner().isScannerAvaiable())
			{
				ArrayList<String> list = NetworkUtils.getInterfacesWithOnlyIp(true, AppConfig.DEFAULT_DISABLED_INTERFACES);

				for (String ip : list)
				{
					NetworkDevice device = new NetworkDevice(ip);
					device.isLocalAddress = true;

					mDeviceRegistry.registerDevice(device);
				}

				context.sendBroadcast(new Intent(ACTION_SCAN_STARTED).putExtra(EXTRA_SCAN_STATUS, (ApplicationHelper.getNetworkDeviceScanner().scan(list, this)) ? STATUS_OK : STATUS_NO_NETWORK_INTERFACE));
			}
			else
				Toast.makeText(context, R.string.concurrent_caution_scan, Toast.LENGTH_SHORT).show();
		}
		else if (ACTION_ADD_IP.equals(intent.getAction()) && intent.hasExtra(EXTRA_DEVICE_IP))
		{
			mInfoLoader.startLoading(mDeviceRegistry, intent.getStringExtra(EXTRA_DEVICE_IP));
		}
	}

	@Override
	public void onDeviceFound(InetAddress address)
	{
		mInfoLoader.startLoading(mDeviceRegistry, address.getHostAddress());
	}

	@Override
	public void onInfoAvailable(NetworkDevice device)
	{
		if (device.deviceId != null)
		{
			mDeviceRegistry.registerDevice(device);
			mContext.sendBroadcast(new Intent(ACTION_DEVICE_FOUND).putExtra(EXTRA_DEVICE_IP, device.ip));
		}
	}

	@Override
	public void onThreadsCompleted()
	{
		mContext.sendBroadcast(new Intent(ACTION_DEVICE_SCAN_COMPLETED));
	}
}
