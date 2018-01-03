package com.genonbeta.TrebleShot.util;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.object.NetworkDevice;

public class AppUtils
{
	public static final String TAG = AppUtils.class.getSimpleName();

	private static int mUniqueNumber = 0;

	public static String getHotspotName(Context context)
	{
		String apName = AppUtils.getLocalDeviceName(context)
				.replace(" ", "_");

		return AppConfig.ACCESS_POINT_PREFIX + (apName.length() > 10 ? apName.substring(0, 9) : apName);
	}

	public static String getLocalDeviceName(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString("device_name", Build.BOARD.toUpperCase());
	}

	public static int getUniqueNumber()
	{
		return (int) System.currentTimeMillis() + (++mUniqueNumber);
	}

	public static NetworkDevice getLocalDevice(Context context)
	{
		String serial = Build.VERSION.SDK_INT < 26
				? Build.SERIAL
				: (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ? Build.getSerial() : null);

		NetworkDevice device = new NetworkDevice(serial);

		device.brand = Build.BRAND;
		device.model = Build.MODEL;
		device.nickname = AppUtils.getLocalDeviceName(context);
		device.isRestricted = false;
		device.isLocalAddress = true;

		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationInfo().packageName, 0);

			device.versionNumber = packageInfo.versionCode;
			device.versionName = packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		return device;
	}

	public static void startForegroundService(Context context, Intent intent)
	{
		if (Build.VERSION.SDK_INT >= 26)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}
}