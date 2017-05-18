package com.genonbeta.TrebleShot.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;

import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */

public class MainDatabase extends SQLiteDatabase
{
	public static final String TAG = MainDatabase.class.getSimpleName();

	public static final String DATABASE_NAME = MainDatabase.class.getSimpleName() + ".db";

	public static final String TABLE_TRANSFER = "transfer";
	public static final String FIELD_TRANSFER_ID = "id";
	public static final String FIELD_TRANSFER_DEVICEID = "deviceId";
	public static final String FIELD_TRANSFER_FILE = "file";
	public static final String FIELD_TRANSFER_NAME = "name";
	public static final String FIELD_TRANSFER_SIZE = "size";
	public static final String FIELD_TRANSFER_MIME = "mime";
	public static final String FIELD_TRANSFER_TYPE = "type";
	public static final String FIELD_TRANSFER_USERIP = "ip";
	public static final String FIELD_TRANSFER_ACCESSPORT = "accessPort";
	public static final String FIELD_TRANSFER_GROUPID = "groupId";
	public static final String FIELD_TRANSFER_FLAG = "flag";
	public static final int TYPE_TRANSFER_TYPE_INCOMING = 0;
	public static final int TYPE_TRANSFER_TYPE_OUTGOING = 1;

	public static final String TABLE_DEVICES = "devices";
	public static final String FIELD_DEVICES_IP = "ip";
	public static final String FIELD_DEVICES_USER = "user";
	public static final String FIELD_DEVICES_BRAND = "brand";
	public static final String FIELD_DEVICES_MODEL = "model";
	public static final String FIELD_DEVICES_ID = "deviceId";
	public static final String FIELD_DEVICES_ISRESTRICTED = "isRestricted";
	public static final String FIELD_DEVICES_ISLOCALADDRESS = "isLocalAddress";

	private Context mContext;

	public MainDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, 1);
		mContext = context;
	}

	@Override
	public void onCreate(android.database.sqlite.SQLiteDatabase db)
	{
		new SQLQuery.CreateTable(TABLE_TRANSFER)
				.addColumn(FIELD_TRANSFER_ID, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_TRANSFER_DEVICEID, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_TRANSFER_GROUPID, SQLQuery.Type.INTEGER.toString(), true)
				.addColumn(FIELD_TRANSFER_FILE, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_TRANSFER_NAME, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_TRANSFER_SIZE, SQLQuery.Type.INTEGER.toString(), true)
				.addColumn(FIELD_TRANSFER_MIME, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_TRANSFER_TYPE, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_TRANSFER_USERIP, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_TRANSFER_FLAG, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_TRANSFER_ACCESSPORT, SQLQuery.Type.INTEGER.toString(), true)
				.exec(db);

		new SQLQuery.CreateTable(TABLE_DEVICES)
				.addColumn(FIELD_DEVICES_IP, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_DEVICES_USER, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_DEVICES_BRAND, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_DEVICES_MODEL, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_DEVICES_ID, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_DEVICES_ISRESTRICTED, SQLQuery.Type.INTEGER.toString(), false)
				.addColumn(FIELD_DEVICES_ISLOCALADDRESS, SQLQuery.Type.INTEGER.toString(), false)
				.exec(db);
	}

	@Override
	public void onUpgrade(android.database.sqlite.SQLiteDatabase sqLiteDatabase, int i, int i1)
	{

	}

	public long getAffectedRowCount()
	{
		Cursor cursor = null;
		long returnCount = 0;

		try
		{
			cursor = getReadableDatabase().rawQuery("SELECT changes() AS affected_row_count", null);

			if (cursor != null && cursor.getCount() > 0 && cursor.moveToFirst())
				returnCount = cursor.getLong(cursor.getColumnIndex("affected_row_count"));
		} catch (SQLException e)
		{
			// Handle exception here.
		}
		finally
		{
			if (cursor != null)
				cursor.close();
		}

		return returnCount;
	}

	public Context getContext()
	{
		return mContext;
	}
}
