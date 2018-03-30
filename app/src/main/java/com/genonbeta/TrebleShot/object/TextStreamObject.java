package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.widget.GroupShareableListAdapter;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

/**
 * created by: Veli
 * date: 30.12.2017 13:19
 */

public class TextStreamObject
		extends GroupShareableListAdapter.GroupShareable
		implements DatabaseObject, Editable
{
	public int id;
	public String text;

	public TextStreamObject()
	{
	}

	public TextStreamObject(String representativeText)
	{
		super(GroupShareableListAdapter.VIEW_TYPE_REPRESENTATIVE, representativeText);
	}

	public TextStreamObject(int id)
	{
		this.id = id;
	}

	public TextStreamObject(int id, String index)
	{
		super(index, index, "text/plain", System.currentTimeMillis(), index.length(), null);

		this.id = id;
		this.text = index;
	}

	@Override
	public boolean equals(Object obj)
	{
		return obj instanceof TextStreamObject && ((TextStreamObject) obj).id == id;
	}

	@Override
	public SQLQuery.Select getWhere()
	{
		return new SQLQuery.Select(AccessDatabase.TABLE_CLIPBOARD)
				.setWhere(AccessDatabase.FIELD_CLIPBOARD_ID + "=?", String.valueOf(id));
	}

	@Override
	public ContentValues getValues()
	{
		ContentValues values = new ContentValues();

		values.put(AccessDatabase.FIELD_CLIPBOARD_ID, id);
		values.put(AccessDatabase.FIELD_CLIPBOARD_TIME, date);
		values.put(AccessDatabase.FIELD_CLIPBOARD_TEXT, text);

		return values;
	}

	@Override
	public void reconstruct(CursorItem item)
	{
		this.id = item.getInt(AccessDatabase.FIELD_CLIPBOARD_ID);
		this.text = item.getString(AccessDatabase.FIELD_CLIPBOARD_TEXT);
		this.date = item.getLong(AccessDatabase.FIELD_CLIPBOARD_TIME);
		this.mimeType = "text/plain";
		this.size = text.length();
		this.friendlyName = text;
		this.fileName = text;
	}

	@Override
	public void onCreateObject(SQLiteDatabase database)
	{

	}

	@Override
	public void onUpdateObject(SQLiteDatabase database)
	{

	}

	@Override
	public void onRemoveObject(SQLiteDatabase database)
	{

	}
}
