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
	public static final String TAG = "ApplicatiobHelper";
	
	private static HashMap<String, NetworkDevice> mDeviceList = new HashMap<String, NetworkDevice>();
	private static HashMap<Integer, AwaitedFileSender> mSenders = new HashMap<Integer, AwaitedFileSender>();
	private static ArrayBlockingQueue<AwaitedFileReceiver> mReceivers = new ArrayBlockingQueue<AwaitedFileReceiver>(2000, true);
	private static ArrayBlockingQueue<AwaitedFileReceiver> mPendingReceivers = new ArrayBlockingQueue<AwaitedFileReceiver>(2000, true);
	private static NetworkDeviceScanner mDeviceScanner = new NetworkDeviceScanner();
	private static int mUniqueNumber = 0;
	
	public static int acceptPendingReceivers(int acceptId)
	{
		int count = 0;
		
		Log.d(TAG, "Receiver count " + getReceivers().size() + "; pending receiver count = " + getPendingReceivers().size() + "; copiedReceivers = " + getPendingReceivers().size());
		
		for (AwaitedFileReceiver receiver : getPendingReceivers())
		{
			Log.d(TAG, "Accept requested id = " + acceptId + "; current receivers id " + receiver.acceptId);
			
			if (receiver.acceptId != acceptId)
				continue;
				
			getReceivers().offer(receiver);
			getPendingReceivers().remove(receiver);
			
			count++;
		}
		
		Log.d(TAG, "After accepting pendingReceivers, current receivers count " + getReceivers().size());
		
		return count;
	}
	
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
	
	public static ArrayBlockingQueue<AwaitedFileReceiver> getReceivers()
	{
		return mReceivers;
	}
	
	public static ArrayBlockingQueue<AwaitedFileReceiver> getPendingReceivers()
	{
		return mPendingReceivers;
	}
	
	public static HashMap<Integer, AwaitedFileSender> getSenders()
	{
		return mSenders;
	}
	
	public static HashMap<String, NetworkDevice> getDeviceList()
	{
		return mDeviceList;
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
	
	public static NetworkDeviceScanner getNetworkDeviceScanner()
	{
		return mDeviceScanner;
	}
	
	public static ArrayList<AwaitedFileReceiver> getPendingReceiversByAcceptId(int acceptId)
	{
		ArrayList<AwaitedFileReceiver> list = new ArrayList<AwaitedFileReceiver>();
		
		for (AwaitedFileReceiver receiver : getPendingReceivers())
		{
			if (receiver.acceptId == acceptId)
				list.add(receiver);
		}
		
		return list;
	}

	public static int getUniqueNumber()
	{
		return mUniqueNumber++;
	}
	
	public static boolean isReceiverExist(AwaitedFileReceiver receiver)
	{
		if (getReceivers().contains(receiver))
			return true;

		return false;	
	}
	
	public static boolean searchWord(String word, String searchThis)
	{
		return word.toLowerCase().contains(searchThis);
	}
	
	public static int removePendingReceivers(int acceptId)
	{
		int count = 0;

		for (AwaitedFileReceiver receiver : getPendingReceivers())
		{
			if (receiver.acceptId != acceptId)
				continue;

			getPendingReceivers().remove(receiver);

			count++;
		}

		return count;
	}
	
	public static int removeReceivers(int acceptId)
	{
		int count = 0;

		for (AwaitedFileReceiver receiver : getReceivers())
		{
			if (receiver.acceptId != acceptId)
				continue;

			getReceivers().remove(receiver);

			count++;
		}

		return count;
	}
	
	public static boolean removeReceiver(AwaitedFileReceiver receiver)
	{
		if (!isReceiverExist(receiver))
			return false;
			
		getReceivers().remove(receiver);

		return true;
	}
	
	public static boolean removeSender(AwaitedFileSender sender)
	{
		if (!ApplicationHelper.getSenders().containsKey(sender.requestId))
			return false;
			
		ApplicationHelper.getSenders().remove(sender.requestId);

		return true;
	}
}
