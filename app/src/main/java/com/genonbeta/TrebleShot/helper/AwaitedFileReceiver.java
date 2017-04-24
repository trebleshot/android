package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.MainDatabase;

public class AwaitedFileReceiver extends AwaitedTransaction
{
	public String ip;
	public String fileMimeType;
	public long fileSize;

	public AwaitedFileReceiver(String ip, int requestId, int acceptId, String fileName, long fileSize, String fileMime)
	{
		super(fileName, requestId, acceptId);

		this.ip = ip;
		this.fileSize = fileSize;
		this.fileMimeType = fileMime;
	}

	@Override
	public void onAddDatabase(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_ACCEPTID, acceptId);
		values.put(MainDatabase.FIELD_TRANSFER_TYPE, MainDatabase.TYPE_TRANSFER_TYPE_INCOMING);
	}
}
