package com.genonbeta.TrebleShot.util;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;

import com.genonbeta.TrebleShot.service.CommunicationService;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

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
		String serial = Build.VERSION.SDK_INT < 26
				? Build.SERIAL
				: (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED ? Build.getSerial() : null);

		NetworkDevice device = new NetworkDevice(serial);

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

	public static void startForegroundService(Context context, Intent intent)
	{
		if (Build.VERSION.SDK_INT >= 26)
			context.startForegroundService(intent);
		else
			context.startService(intent);
	}
}