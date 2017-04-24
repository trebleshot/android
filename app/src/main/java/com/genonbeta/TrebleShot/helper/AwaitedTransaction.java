package com.genonbeta.TrebleShot.helper;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.MainDatabase;

import java.io.File;

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */

abstract public class AwaitedTransaction
{
	public String fileName;
	public int requestId;
	public int acceptId;
	public boolean isCancelled = false;

	public abstract void onAddDatabase(ContentValues values);

	public AwaitedTransaction(String fileName, int requestId, int acceptId)
	{
		this.fileName = fileName;
		this.requestId = requestId;
	}

	public void addDatabase(ContentValues values)
	{
		values.put(MainDatabase.FIELD_TRANSFER_ID, requestId);
		values.put(MainDatabase.FIELD_TRANSFER_NAME, fileName);

		onAddDatabase(values);
	}
}
