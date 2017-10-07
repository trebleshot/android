package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;
import android.util.Log;

import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.android.database.CursorItem;

import java.text.SimpleDateFormat;
import java.util.UUID;

public class AwaitedFileReceiver extends AwaitedTransaction
{
	public String fileMimeType;
	public String fileAddress;

	public AwaitedFileReceiver(NetworkDevice networkDevice, int requestId, int groupId, String fileName, long fileSize, String fileMime)
	{
		super(networkDevice.deviceId, requestId, groupId, networkDevice.ip, fileName, fileSize);
		this.fileMimeType = fileMime;
		this.fileAddress = "." + UUID.randomUUID() + ".tshare";
	}

	public AwaitedFileReceiver(CursorItem item)
	{
		super(item);
	}

	@Override
	public void onDatabaseObject(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_TYPE, MainDatabase.TYPE_TRANSFER_TYPE_INCOMING);
		values.put(MainDatabase.FIELD_TRANSFER_MIME, fileMimeType);
		values.put(MainDatabase.FIELD_TRANSFER_FILE, fileAddress);
	}

	@Override
	public void onCreate(CursorItem item)
	{
		this.fileMimeType = item.getString(MainDatabase.FIELD_TRANSFER_MIME);
		this.fileAddress = item.getString(MainDatabase.FIELD_TRANSFER_FILE);
	}
}
