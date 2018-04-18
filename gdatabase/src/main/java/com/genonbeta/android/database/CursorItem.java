package com.genonbeta.android.database;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by: veli
 * Date: 12/5/16 4:50 PM
 */

public class CursorItem
{
	private Map<String, Object> mList = new HashMap<>();

	public CursorItem clear()
	{
		mList.clear();
		return this;
	}

	public boolean exists(String keyName)
	{
		return mList.containsKey(keyName) && mList.get(keyName) != null;
	}

	public boolean getBoolean(String keyName)
	{
		return Boolean.valueOf(getString(keyName));
	}

	public double getDouble(String keyName)
	{
		try {
			return Double.valueOf(getString(keyName));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public float getFloat(String keyName)
	{
		try {
			return Float.valueOf(getString(keyName));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public int getInt(String keyName)
	{
		try {
			return Integer.valueOf(getString(keyName));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public long getLong(String keyName)
	{
		try {
			return Long.valueOf(getString(keyName));
		} catch (NumberFormatException e) {
			e.printStackTrace();
		}

		return 0;
	}

	public String getString(String keyName)
	{
		return mList.get(keyName) == null ? null : String.valueOf(mList.get(keyName));
	}

	public int length()
	{
		return mList.size();
	}

	public Map<String, Object> list()
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
