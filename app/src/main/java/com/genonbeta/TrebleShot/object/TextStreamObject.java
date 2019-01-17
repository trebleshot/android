package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

/**
 * created by: Veli
 * date: 30.12.2017 13:19
 */

public class TextStreamObject
        extends GroupEditableListAdapter.GroupShareable
        implements DatabaseObject<Object>, Editable
{
    public String text;

    public TextStreamObject()
    {
    }

    public TextStreamObject(String representativeText)
    {
        super(GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE, representativeText);
    }

    public TextStreamObject(long id)
    {
        setId(id);
    }

    public TextStreamObject(long id, String index)
    {
        super(id, index, index, "text/plain", System.currentTimeMillis(), index.length(), null);
        this.text = index;
    }

    @Override
    public boolean applyFilter(String[] filteringKeywords)
    {
        if (super.applyFilter(filteringKeywords))
            return true;

        for (String keyword : filteringKeywords)
            if (text.toLowerCase().contains(keyword.toLowerCase()))
                return true;

        return false;
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
                .setWhere(AccessDatabase.FIELD_CLIPBOARD_ID + "=?", String.valueOf(getId()));
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
        this.id = item.getLong(AccessDatabase.FIELD_CLIPBOARD_ID);
        this.text = item.getString(AccessDatabase.FIELD_CLIPBOARD_TEXT);
        this.date = item.getLong(AccessDatabase.FIELD_CLIPBOARD_TIME);
        this.mimeType = "text/plain";
        this.size = text.length();
        this.friendlyName = text;
        this.fileName = text;
    }

    @Override
    public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, Object parent)
    {

    }

    @Override
    public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, Object parent)
    {

    }

    @Override
    public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, Object parent)
    {

    }
}
