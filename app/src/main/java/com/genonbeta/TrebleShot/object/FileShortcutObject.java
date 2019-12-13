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
import android.net.Uri;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;

public class FileShortcutObject implements DatabaseObject<Object>
{
    public String title;
    public Uri path;

    public FileShortcutObject()
    {
    }

    public FileShortcutObject(Uri path)
    {
        this.path = path;
    }

    public FileShortcutObject(String title, Uri path)
    {
        this(path);
        this.title = title;
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_FILEBOOKMARK).setWhere(
                String.format("%s = ?", AccessDatabase.FIELD_FILEBOOKMARK_PATH), path.toString());
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues contentValues = new ContentValues();

        contentValues.put(AccessDatabase.FIELD_FILEBOOKMARK_TITLE, title);
        contentValues.put(AccessDatabase.FIELD_FILEBOOKMARK_PATH, path.toString());
        ;

        return contentValues;
    }

    @Override
    public void reconstruct(ContentValues item)
    {
        this.title = item.getAsString(AccessDatabase.FIELD_FILEBOOKMARK_TITLE);
        this.path = Uri.parse(item.getAsString(AccessDatabase.FIELD_FILEBOOKMARK_PATH));
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

