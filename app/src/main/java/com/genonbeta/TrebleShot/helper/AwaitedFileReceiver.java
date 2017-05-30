package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.android.database.CursorItem;

import java.io.File;

public class AwaitedFileReceiver extends AwaitedTransaction
{
	public String fileMimeType;
	public String selectedPath;

	public AwaitedFileReceiver(NetworkDevice networkDevice, int requestId, int groupId, String fileName, long fileSize, String fileMime)
	{
		super(networkDevice.deviceId, requestId, groupId, networkDevice.ip, fileName, fileSize);
		this.fileMimeType = fileMime;
	}

	public AwaitedFileReceiver(CursorItem item)
	{
		super(item);
	}

	@Override
	public void onDatabaseObject(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_TYPE, MainDatabase.TYPE_TRANSFER_TYPE_INCOMING);
		values.put(MainDatabase.FIELD_TRANSFER_FILE, selectedPath);
	}

	@Override
	public void onCreate(CursorItem item)
	{
		this.fileMimeType = item.getString(MainDatabase.FIELD_TRANSFER_MIME);
		this.selectedPath = item.exists(MainDatabase.FIELD_TRANSFER_FILE) ? item.getString(MainDatabase.FIELD_TRANSFER_FILE) : null;
	}
}
