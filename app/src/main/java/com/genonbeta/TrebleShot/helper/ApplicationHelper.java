package com.genonbeta.TrebleShot.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.core.util.NetworkDeviceScanner;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;

public class ApplicationHelper
{
	public static final String TAG = ApplicationHelper.class.getSimpleName();

	private static HashMap<String, NetworkDevice> mDeviceList = new HashMap<String, NetworkDevice>();
	private static NetworkDeviceScanner mDeviceScanner = new NetworkDeviceScanner();
	private static int mUniqueNumber = 0;

	public static File getApplicationDirectory(Context context)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

		File testPath = new File(prefs.getString("storage_path", Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getString(R.string.app_name)));

		if (testPath.isDirectory())
			return testPath;
		else if (!testPath.exists() && testPath.mkdirs())
			return testPath;

		File appDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + context.getString(R.string.app_name));

		if (!appDir.exists())
			appDir.mkdirs();

		return appDir;
	}

	public static HashMap<String, NetworkDevice> getDeviceList()
	{
		return mDeviceList;
	}

	public static NetworkDevice getNetworkDeviceByAddress(String ipAddress)
	{
		NetworkDevice device = getDeviceList().get(ipAddress);
		return device == null ? new NetworkDevice(ipAddress, null, null, null) : device;
	}

	public static File getFileFromUri(Context context, Uri fileUri)
	{
		String fileUriString = fileUri.toString();
		File file = null;

		if (fileUriString.startsWith("content"))
		{
			Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null);

			if (cursor != null)
			{
				if (cursor.moveToFirst())
				{
					int dataIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);
					String dataPath = cursor.getString(dataIndex);

					file = new File(dataPath);
				}

				cursor.close();
			}
		}
		else if (fileUriString.startsWith("file"))
			file = new File(URI.create(fileUriString));

		return file;
	}

	public static String getFirstLetters(String text, int breakAfter)
	{
		StringBuilder stringBuilder = new StringBuilder();

		for (String letter : text.split(" "))
		{
			if (stringBuilder.length() > breakAfter)
				break;

			stringBuilder.append(letter.charAt(0));
		}

		return stringBuilder.toString().toUpperCase();
	}

	public static NetworkDeviceScanner getNetworkDeviceScanner()
	{
		return mDeviceScanner;
	}

	public static int getUniqueNumber()
	{
		// TODO: 4/28/17 this isn't secure. Check other solutions
		return (int)System.currentTimeMillis() + (++mUniqueNumber);
	}

	public static boolean searchWord(String word, String searchThis)
	{
		return word.toLowerCase().contains(searchThis);
	}
}
