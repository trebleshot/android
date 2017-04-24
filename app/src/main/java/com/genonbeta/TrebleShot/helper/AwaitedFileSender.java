package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.MainDatabase;

import java.io.File;

public class AwaitedFileSender extends AwaitedTransaction
{
	public String ip;
	public File file;
	public int port;

	public AwaitedFileSender(String ip, String fileName, File file, int requestId, int acceptId)
	{
		super(fileName, requestId, acceptId);

		this.ip = ip;
		this.file = file;
	}

	public void setPort(int port)
	{
		this.port = port;
	}

	@Override
	public void onAddDatabase(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_FILE, file.getAbsolutePath());
		values.put(MainDatabase.FIELD_TRANSFER_TYPE, MainDatabase.TYPE_TRANSFER_TYPE_OUTGOING);
	}
}
