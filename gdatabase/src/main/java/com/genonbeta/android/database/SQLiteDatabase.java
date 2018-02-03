package com.genonbeta.android.database;

import android.content.ContentValues;
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
	public SQLiteDatabase(Context context, String name, android.database.sqlite.SQLiteDatabase.CursorFactory factory, int version)
	{
		super(context, name, factory, version);
	}

	public <T extends FlexibleObject> ArrayList<T> castQuery(SQLQuery.Select select, final Class<T> clazz)
	{
		ArrayList<T> returnedList = new ArrayList<>();
		ArrayList<CursorItem> itemList = getTable(select);

		try {
			for (CursorItem item : itemList) {
				T newClazz = clazz.newInstance();

				newClazz.reconstruct(item);
				returnedList.add(newClazz);
			}
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		}

		return returnedList;
	}

	public int delete(SQLQuery.Select select)
	{
		return getWritableDatabase().delete(select.tableName, select.where, select.whereArgs);
	}

	public CursorItem getFirstFromTable(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = getTable(select.setLimit(1));
		return list.size() > 0 ? list.get(0) : null;
	}

	public ArrayList<CursorItem> getTable(SQLQuery.Select select)
	{
		ArrayList<CursorItem> list = new ArrayList<>();

		Cursor cursor = getReadableDatabase().query(select.tableName,
				select.columns,
				select.where,
				select.whereArgs,
				select.groupBy,
				select.having,
				select.orderBy,
				select.limit);

		if (cursor.moveToFirst()) {
			if (select.loadListener != null)
				select.loadListener.onOpen(this, cursor);

			do {
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


	public long insert(FlexibleObject object)
	{
		object.onCreateObject(this);
		return insert(object.getWhere().tableName, null, object.getValues());
	}

	public long insert(String tableName, String nullColumnHack, ContentValues contentValues)
	{
		return getWritableDatabase().insert(tableName, nullColumnHack, contentValues);
	}

	public void publish(FlexibleObject object)
	{
		if (getFirstFromTable(object.getWhere()) != null)
			update(object);
		else
			insert(object);
	}

	public void remove(FlexibleObject object)
	{
		object.onRemoveObject(this);
		delete(object.getWhere());
	}

	public void reconstruct(FlexibleObject object) throws Exception
	{
		CursorItem item = getFirstFromTable(object.getWhere());

		if (item == null) {
			SQLQuery.Select select = object.getWhere();

			StringBuilder whereArgs = new StringBuilder();

			for (String arg : select.whereArgs) {
				if (whereArgs.length() > 0)
					whereArgs.append(", ");

				whereArgs.append("[] ");
				whereArgs.append(arg);
			}

			throw new Exception("No data was returned from: query"
					+ "; tableName: " + select.tableName
					+ "; where: " + select.where
					+ "; whereArgs: " + whereArgs.toString());
		}

		object.reconstruct(item);
	}

	public int update(FlexibleObject object)
	{
		object.onUpdateObject(this);
		return update(object.getWhere(), object.getValues());
	}

	public int update(SQLQuery.Select select, ContentValues values)
	{
		return getWritableDatabase().update(select.tableName, values, select.where, select.whereArgs);
	}
}
