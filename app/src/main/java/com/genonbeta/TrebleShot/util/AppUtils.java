package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;

public class AppUtils
{
	public static final String TAG = AppUtils.class.getSimpleName();

	private static int mUniqueNumber = 0;

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
		NetworkDevice device = new NetworkDevice(Build.SERIAL);

		device.brand = Build.BRAND;
		device.model = Build.MODEL;
		device.user = AppUtils.getLocalDeviceName(context);
		device.isRestricted = false;
		device.isLocalAddress = true;

		try {
			PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getApplicationInfo().packageName, 0);

			device.buildNumber = packageInfo.versionCode;
			device.buildName = packageInfo.versionName;
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		return device;
	}
}