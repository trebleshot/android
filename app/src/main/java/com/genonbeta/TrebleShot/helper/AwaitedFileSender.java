package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;
import android.net.Uri;

import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.android.database.CursorItem;

import java.io.File;

public class AwaitedFileSender extends AwaitedTransaction
{
	public Uri fileUri;
	public int port;

	public AwaitedFileSender(NetworkDevice networkDevice, int requestId, int groupId, String fileName, long fileSize, Uri fileUri)
	{
		super(networkDevice.deviceId, requestId, groupId, networkDevice.ip, fileName, fileSize);

		this.fileUri = fileUri;
	}

	public AwaitedFileSender(CursorItem item)
	{
		super(item);
	}

	@Override
	public void onDatabaseObject(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_FILE, fileUri.toString());
		values.put(MainDatabase.FIELD_TRANSFER_ACCESSPORT, port);
		values.put(MainDatabase.FIELD_TRANSFER_TYPE, MainDatabase.TYPE_TRANSFER_TYPE_OUTGOING);
	}

	@Override
	public void onCreate(CursorItem item)
	{
		this.fileUri = Uri.parse(item.getString(MainDatabase.FIELD_TRANSFER_FILE));
		this.port = item.getInt(MainDatabase.FIELD_TRANSFER_ACCESSPORT);
	}
}
