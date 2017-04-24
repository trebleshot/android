package com.genonbeta.TrebleShot.database;

import android.content.Context;

import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */

public class MainDatabase extends SQLiteDatabase
{
	public static final String DATABASE_NAME = MainDatabase.class.getSimpleName() + ".db";

	public static final String TABLE_TRANSFER = "transfer";
	public static final String FIELD_TRANSFER_ID = "id";
	public static final String FIELD_TRANSFER_FILE = "file";
	public static final String FIELD_TRANSFER_NAME = "name";
	public static final String FIELD_TRANSFER_SIZE = "size";
	public static final String FIELD_TRANSFER_MIME = "mime";
	public static final String FIELD_TRANSFER_TYPE = "type";
	public static final String FIELD_TRANSFER_ACCEPTID = "acceptId";
	public static final int TYPE_TRANSFER_TYPE_INCOMING = 0;
	public static final int TYPE_TRANSFER_TYPE_OUTGOING = 1;

	public MainDatabase(Context context)
	{
		super(context, DATABASE_NAME, null, 1);
	}

	@Override
	public void onCreate(android.database.sqlite.SQLiteDatabase db)
	{
		new SQLQuery.CreateTable(TABLE_TRANSFER)
				.addColumn(FIELD_TRANSFER_ID, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_TRANSFER_ACCEPTID, SQLQuery.Type.INTEGER.toString(), true)
				.addColumn(FIELD_TRANSFER_FILE, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_TRANSFER_NAME, SQLQuery.Type.TEXT.toString(), false)
				.addColumn(FIELD_TRANSFER_SIZE, SQLQuery.Type.INTEGER.toString(), true)
				.addColumn(FIELD_TRANSFER_MIME, SQLQuery.Type.TEXT.toString(), true)
				.addColumn(FIELD_TRANSFER_TYPE, SQLQuery.Type.TEXT.toString(), false)
				.exec(db);
	}

	@Override
	public void onUpgrade(android.database.sqlite.SQLiteDatabase sqLiteDatabase, int i, int i1)
	{

	}
}
