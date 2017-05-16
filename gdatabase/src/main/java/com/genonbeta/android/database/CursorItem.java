package com.genonbeta.android.database;

import java.util.HashMap;

/**
 * Created by: veli
 * Date: 12/5/16 4:50 PM
 */

public class CursorItem
{
	private HashMap<String, Object> mList = new HashMap<>();

	public CursorItem clear()
	{
		mList.clear();
		return this;
	}

	public boolean exists(String keyName)
	{
		return mList.containsKey(keyName);
	}

	public boolean getBoolean(String keyName)
	{
		return Boolean.valueOf(getString(keyName));
	}

	public double getDouble(String keyName)
	{
		return Double.valueOf(getString(keyName));
	}

	public float getFloat(String keyName)
	{
		return Float.valueOf(getString(keyName));
	}

	public int getInt(String keyName)
	{
		return Integer.valueOf(getString(keyName));
	}

	public long getLong(String keyName)
	{
		return Long.valueOf(getString(keyName));
	}

	public String getString(String keyName)
	{
		return String.valueOf(mList.get(keyName));
	}

	public int lenght()
	{
		return mList.size();
	}

	public boolean isStatic()
	{
		return exists(SQLiteDatabase.COLUMN_ISSTATIC) && getInt(SQLiteDatabase.COLUMN_ISSTATIC) == 1;
	}

	public HashMap<String, Object> list()
	{
		return mList;
	}

	public CursorItem put(String keyName, Object object)
	{
		mList.put(keyName, object);
		return this;
	}

	public CursorItem putAll(CursorItem item)
	{
		list().putAll(item.list());
		return this;
	}

	public int size()
	{
		return mList.size();
	}
}
