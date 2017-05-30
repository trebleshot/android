package com.genonbeta.TrebleShot.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.core.util.NetworkDeviceScanner;

import java.io.File;
import java.net.URI;

public class ApplicationHelper
{
	public static final String TAG = ApplicationHelper.class.getSimpleName();

	private static NetworkDeviceScanner mDeviceScanner = new NetworkDeviceScanner();
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

	public static String getNameOfThisDevice(Context context)
	{
		return PreferenceManager.getDefaultSharedPreferences(context)
				.getString("device_name", Build.BOARD.toUpperCase());
	}

	public static NetworkDeviceScanner getNetworkDeviceScanner()
	{
		return mDeviceScanner;
	}

	public static int getUniqueNumber()
	{
		return (int) System.currentTimeMillis() + (++mUniqueNumber);
	}

	public static boolean searchWord(String word, String searchThis)
	{
		return word.toLowerCase().contains(searchThis);
	}
}