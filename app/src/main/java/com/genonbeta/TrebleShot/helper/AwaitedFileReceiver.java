package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.android.database.CursorItem;

public class AwaitedFileReceiver extends AwaitedTransaction
{
	public String fileMimeType;
	public long fileSize;

	public AwaitedFileReceiver(int requestId, int acceptId, String ip, String fileName, long fileSize, String fileMime)
	{
		super(requestId, acceptId, ip, fileName);

		this.fileSize = fileSize;
		this.fileMimeType = fileMime;
	}

	public AwaitedFileReceiver(CursorItem item)
	{
		super(item);
	}

	@Override
	public void onDatabaseObject(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_SIZE, fileSize);
		values.put(MainDatabase.FIELD_TRANSFER_TYPE, MainDatabase.TYPE_TRANSFER_TYPE_INCOMING);
	}

	@Override
	public void onCreate(CursorItem item)
	{
		this.fileSize = item.getLong(MainDatabase.FIELD_TRANSFER_SIZE);
		this.fileMimeType = item.getString(MainDatabase.FIELD_TRANSFER_MIME);
	}
}
