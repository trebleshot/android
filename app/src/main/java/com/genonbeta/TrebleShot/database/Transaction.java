package com.genonbeta.TrebleShot.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.util.Log;

import com.genonbeta.TrebleShot.helper.AwaitedFileSender;

/**
 * Created by: veli
 * Date: 4/15/17 1:16 AM
 */

public class Transaction extends MainDatabase
{
	public static final String TAG = Transaction.class.getSimpleName();

	public Transaction(Context context)
	{
		super(context);
	}

	public boolean registerSender(AwaitedFileSender sender)
	{
		ContentValues values = new ContentValues();

		values.put(FIELD_TRANSFER_ID, sender.requestId);
		values.put(FIELD_TRANSFER_GROUPID, sender.requestId);
		values.put(FIELD_TRANSFER_FILE, sender.file.getAbsolutePath());
		values.put(FIELD_TRANSFER_TYPE, TYPE_TRANSFER_TYPE_OUTGOING);

		getWritableDatabase().insert(TABLE_TRANSFER, null, values);

		return getAffectedRowCount() > 0;
	}

	public boolean removeSender(AwaitedFileSender sender)
	{
		getWritableDatabase().delete(TABLE_TRANSFER, FIELD_TRANSFER_ID + "=?", new String[] {String.valueOf(sender.requestId)});

		return getAffectedRowCount() > 0;
	}

	public long getAffectedRowCount()
	{
		Cursor cursor = null;
		long returnCount = 0;

		try
		{
			cursor = getReadableDatabase().rawQuery("SELECT changes() AS affected_row_count", null);

			if(cursor != null && cursor.getCount() > 0 && cursor.moveToFirst())
			{
				returnCount = cursor.getLong(cursor.getColumnIndex("affected_row_count"));
				Log.d(TAG, "affectedRowCount = " + returnCount);
			}
			else
			{
				// Some error occurred?
			}
		}
		catch(SQLException e)
		{
			// Handle exception here.
		}
		finally
		{
			if(cursor != null)
				cursor.close();
		}

		return returnCount;
	}
}
