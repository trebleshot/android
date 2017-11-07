package com.genonbeta.TrebleShot.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.service.Keyword;
import com.genonbeta.TrebleShot.util.AddressedInterface;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.NetworkDeviceInfoLoader;
import com.genonbeta.TrebleShot.util.NetworkDeviceScanner;
import com.genonbeta.TrebleShot.util.NetworkUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.HashMap;

public class DeviceScannerProvider extends BroadcastReceiver implements NetworkDeviceScanner.ScannerHandler, NetworkDeviceInfoLoader.OnInfoAvailableListener
{
	public static final String ACTION_SCAN_DEVICES = "genonbeta.intent.action.SCAN_DEVICES";
	public static final String ACTION_SCAN_STARTED = "genonbeta.intent.action.SCAN_STARTED";
	public static final String ACTION_DEVICE_SCAN_COMPLETED = "genonbeta.intent.action.DEVICE_SCAN_COMPLETED";

	public static final String EXTRA_SCAN_STATUS = "genonbeta.intent.extra.SCAN_STATUS";

	public static final String STATUS_OK = "genonbeta.intent.status.OK";
	public static final String STATUS_NO_NETWORK_INTERFACE = "genonbeta.intent.status.NO_NETWORK_INTERFACE";

	private static NetworkDeviceScanner mDeviceScanner = new NetworkDeviceScanner();

	private Context mContext;
	private NetworkDeviceInfoLoader mInfoLoader = new NetworkDeviceInfoLoader(this);
	private AccessDatabase mDatabase;

	@Override
	public void onReceive(Context context, Intent intent)
	{
		mContext = context;
		mDatabase = new AccessDatabase(context);

		if (ACTION_SCAN_DEVICES.equals(intent.getAction())) {
			if (mDeviceScanner.isScannerAvailable()) {
				ArrayList<AddressedInterface> interfaceList = NetworkUtils.getInterfaces(true, AppConfig.DEFAULT_DISABLED_INTERFACES);

				NetworkDevice device = AppUtils.getLocalDevice(context);
				mDatabase.publish(device);

				for (AddressedInterface addressedInterface : interfaceList) {
					NetworkDevice.Connection connection = new NetworkDevice.Connection(addressedInterface.getNetworkInterface().getDisplayName(), addressedInterface.getAssociatedAddress(), Build.SERIAL);
					mDatabase.publish(connection);
				}

				context.sendBroadcast(new Intent(ACTION_SCAN_STARTED).putExtra(EXTRA_SCAN_STATUS, (mDeviceScanner.scan(interfaceList, this)) ? STATUS_OK : STATUS_NO_NETWORK_INTERFACE));
			} else
				Toast.makeText(context, R.string.mesg_stillScanning, Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onDeviceFound(InetAddress address, NetworkInterface networkInterface)
	{
		NetworkDevice.Connection connection = new NetworkDevice.Connection(networkInterface.getDisplayName(), address.getHostAddress(), "-");

		mDatabase.publish(connection);
		mInfoLoader.startLoading(mDatabase, address.getHostAddress());
	}

	@Override
	public void onInfoAvailable(AccessDatabase database, NetworkDevice device, String ipAddress)
	{
		if (device.deviceId != null) {
			NetworkDevice.Connection connection = new NetworkDevice.Connection(ipAddress);
			NetworkDevice localDevice = AppUtils.getLocalDevice(mContext);

			try {
				mDatabase.reconstruct(connection);
			} catch (Exception e) {
				connection.adapterName = Keyword.UNKNOWN_INTERFACE;
			}

			connection.deviceId = device.deviceId;

			mDatabase.publish(connection);

			if (!localDevice.deviceId.equals(device.deviceId))
				mDatabase.publish(device);
			//mContext.sendBroadcast(new Intent(ACTION_DEVICE_FOUND).putExtra(EXTRA_DEVICE_IP, device.ip));
		}
	}

	@Override
	public void onThreadsCompleted()
	{
		mContext.sendBroadcast(new Intent(ACTION_DEVICE_SCAN_COMPLETED));
	}

	public static NetworkDeviceScanner getDeviceScanner()
	{
		return mDeviceScanner;
	}
}
