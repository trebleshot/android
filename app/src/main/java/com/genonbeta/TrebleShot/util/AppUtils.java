package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.genonbeta.TrebleShot.R;

import java.io.File;

public class AppUtils
{
	public static final String TAG = AppUtils.class.getSimpleName();

	private static int mUniqueNumber = 0;

	public static File getApplicationDirectory(Context context)
	{
		String defaultPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + context.getString(R.string.text_appName);
		SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
		File storagePath = new File(sharedPreferences.getString("storage_path", defaultPath));

		if (!storagePath.exists())
			storagePath.mkdirs();

		if (storagePath.canWrite())
			return storagePath;

		return new File(defaultPath);
	}

	public static String getFirstLetters(String text, int breakAfter)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (String letter : text.split(" ")) {
			if (stringBuilder.length() > breakAfter)
				break;

			stringBuilder.append(letter.charAt(0));
		}

		return stringBuilder.toString().toUpperCase();
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
		NetworkDevice device = new NetworkDevice(Build.SERIAL);

		device.brand = Build.BRAND;
		device.model = Build.MODEL;
		device.user = AppUtils.getLocalDeviceName(context);
		device.isRestricted = false;
		device.isLocalAddress = true;

		return device;
	}

	public static boolean searchWord(String word, String searchThis)
	{
		return word.toLowerCase().contains(searchThis);
	}
}