package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.MainDatabase;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.android.database.CursorItem;

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */

abstract public class AwaitedTransaction
{
	public String fileName;
	public String ip;
	public int requestId;
	public int acceptId;
	public DynamicNotification notification;
	public Transaction.Flag flag = Transaction.Flag.PENDING;

	public abstract void onDatabaseObject(ContentValues values);
	public abstract void onCreate(CursorItem item);

	public AwaitedTransaction(String ip, String fileName, int requestId, int acceptId)
	{
		this.ip = ip;
		this.fileName = fileName;
		this.requestId = requestId;
		this.acceptId = acceptId;
	}

	public AwaitedTransaction(CursorItem item)
	{
		this.ip = item.getString(MainDatabase.FIELD_TRANSFER_USERIP);
		this.fileName = item.getString(MainDatabase.FIELD_TRANSFER_NAME);
		this.requestId = item.getInt(MainDatabase.FIELD_TRANSFER_ID);
		this.acceptId = item.getInt(MainDatabase.FIELD_TRANSFER_ACCEPTID);
		this.flag = Transaction.Flag.valueOf(item.getString(MainDatabase.FIELD_TRANSFER_FLAG));

		this.onCreate(item);
	}

	public ContentValues getDatabaseObject()
	{
		ContentValues values = new ContentValues();
		getDatabaseObject(values);

		return values;
	}

	public void getDatabaseObject(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_ID, requestId);
		values.put(MainDatabase.FIELD_TRANSFER_ACCEPTID, acceptId);
		values.put(MainDatabase.FIELD_TRANSFER_NAME, fileName);
		values.put(MainDatabase.FIELD_TRANSFER_USERIP, ip);
		values.put(MainDatabase.FIELD_TRANSFER_FLAG, flag.toString());

		onDatabaseObject(values);
	}
}
