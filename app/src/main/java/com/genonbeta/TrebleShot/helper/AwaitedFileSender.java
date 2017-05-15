package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.android.database.CursorItem;

import java.io.File;

public class AwaitedFileSender extends AwaitedTransaction
{
	public File file;
	public int port;

	public AwaitedFileSender(int requestId, int acceptId, String ip, String fileName, long fileSize, File file)
	{
		super(requestId, acceptId, ip, fileName, fileSize);

		this.file = file;
	}

	public AwaitedFileSender(CursorItem item)
	{
		super(item);
	}

	@Override
	public void onDatabaseObject(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_FILE, file.getAbsolutePath());
		values.put(MainDatabase.FIELD_TRANSFER_ACCESSPORT, port);
		values.put(MainDatabase.FIELD_TRANSFER_TYPE, MainDatabase.TYPE_TRANSFER_TYPE_OUTGOING);
	}

	@Override
	public void onCreate(CursorItem item)
	{
		this.file = new File(item.getString(MainDatabase.FIELD_TRANSFER_FILE));
		this.port = item.getInt(MainDatabase.FIELD_TRANSFER_ACCESSPORT);
	}
}
