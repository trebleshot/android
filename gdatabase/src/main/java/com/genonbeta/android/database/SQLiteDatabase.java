package com.genonbeta.android.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by: veli
 * Date: 1/31/17 4:51 PM
 */

abstract public class SQLiteDatabase extends SQLiteOpenHelper
{
	public static final String COLUMN_ID = "_id";
	public static final String COLUMN_NAME = "_name";
	public static final String COLUMN_TEXT = "_text";
	public static final String COLUMN_ISDELETED = "_isDeleted";
	public static final String COLUMN_ISSTATIC = "_isStatic";
	public static final String COLUMN_UPDATETIME = "_updateTime";

	public SQLiteDatabase(Context context, String name, android.database.sqlite.SQLiteDatabase.CursorFactory factory, int version)
	{
		super(context, name, factory, version);
	}

	public CursorItem getFirstFromTable(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = getTable(select);
		return list.size() > 0 ? list.get(0) : null;
	}

	public ArrayList<CursorItem> getTable(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = new ArrayList<>();

		Cursor cursor = getReadableDatabase().query(select.tableName, select.columns, select.where, select.whereArgs, select.groupBy, select.having, select.orderBy);

		if (cursor.moveToFirst())
		{
			if (select.loadListener != null)
				select.loadListener.onOpen(this, cursor);

			do
			{
				CursorItem item = new CursorItem();

				for (int i = 0; i < cursor.getColumnCount(); i++)
					item.put(cursor.getColumnName(i), cursor.getString(i));

				if (select.loadListener != null)
					select.loadListener.onLoad(this, cursor, item);

				list.add(item);
			} while (cursor.moveToNext());
		}

		cursor.close();

		return list;
	}

	public HashMap<Long, Long> getIdentities(String tableName)
	{
		HashMap<Long, Long> identityList = new HashMap<>();
		ArrayList<CursorItem> identities = getTable(new SQLQuery.Select(tableName, COLUMN_ID, COLUMN_UPDATETIME, COLUMN_ISSTATIC));

		for (CursorItem item : identities)
		{
			if (item.isStatic())
				continue;

			identityList.put(item.getLong(COLUMN_ID), item.getLong(COLUMN_UPDATETIME));
		}

		return identityList;
	}

	public void removeOldItems(HashMap<Long, Long> ids, String tableName)
	{
		StringBuilder idsToValues = new StringBuilder();

		for (Long id : ids.keySet())
		{
			if (idsToValues.length() > 0)
				idsToValues.append(",");

			idsToValues.append(id);
		}

		getWritableDatabase().execSQL("DELETE FROM " + tableName + " WHERE " + COLUMN_ID + " IN (" + idsToValues.toString() + ")");
	}
}
