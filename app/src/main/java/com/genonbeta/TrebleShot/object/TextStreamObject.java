/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.object;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.SQLQuery;

/**
 * created by: Veli
 * date: 30.12.2017 13:19
 */

public class TextStreamObject extends GroupEditableListAdapter.GroupShareable implements DatabaseObject<Object>
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
        return new SQLQuery.Select(Kuick.TABLE_CLIPBOARD)
                .setWhere(Kuick.FIELD_CLIPBOARD_ID + "=?", String.valueOf(getId()));
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(Kuick.FIELD_CLIPBOARD_ID, id);
        values.put(Kuick.FIELD_CLIPBOARD_TIME, date);
        values.put(Kuick.FIELD_CLIPBOARD_TEXT, text);

        return values;
    }

    @Override
    public void reconstruct(ContentValues item)
    {
        this.id = item.getAsLong(Kuick.FIELD_CLIPBOARD_ID);
        this.text = item.getAsString(Kuick.FIELD_CLIPBOARD_TEXT);
        this.date = item.getAsLong(Kuick.FIELD_CLIPBOARD_TIME);
        this.mimeType = "text/plain";
        this.size = text.length();
        this.friendlyName = text;
        this.fileName = text;
    }

    @Override
    public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Object parent)
    {

    }

    @Override
    public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Object parent)
    {

    }

    @Override
    public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Object parent)
    {

    }
}
