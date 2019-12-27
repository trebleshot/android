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
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.object.Selectable;

import java.util.List;

/**
 * created by: veli
 * date: 06.04.2018 09:37
 */
public class TransferGroup implements DatabaseObject<NetworkDevice>, Selectable
{
    public long id;
    public long dateCreated;
    public String savePath;
    public boolean isPaused;
    public boolean isServedOnWeb;

    private boolean mIsSelected = false;
    private boolean mDeleteFilesOnRemoval = false;

    public TransferGroup()
    {
    }

    public TransferGroup(long id)
    {
        this.id = id;
    }

    public TransferGroup(ContentValues item)
    {
        reconstruct(item);
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof TransferGroup && ((TransferGroup) obj).id == id;
    }

    @Override
    public void reconstruct(ContentValues item)
    {
        this.id = item.getAsLong(AccessDatabase.FIELD_TRANSFERGROUP_ID);
        this.savePath = item.getAsString(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH);
        this.dateCreated = item.getAsLong(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED);
        this.isServedOnWeb = item.getAsInteger(AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB) == 1;
        this.isPaused = item.getAsInteger(AccessDatabase.FIELD_TRANSFERGROUP_ISPAUSED) == 1;
    }

    @Override
    public boolean isSelectableSelected()
    {
        return mIsSelected;
    }

    @Override
    public String getSelectableTitle()
    {
        return String.valueOf(id);
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(AccessDatabase.FIELD_TRANSFERGROUP_ID, id);
        values.put(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH, savePath);
        values.put(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED, dateCreated);
        values.put(AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB, isServedOnWeb ? 1 : 0);
        values.put(AccessDatabase.FIELD_TRANSFERGROUP_ISPAUSED, isPaused ? 1 : 0);

        return values;
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
                .setWhere(AccessDatabase.FIELD_TRANSFERGROUP_ID + "=?", String.valueOf(id));
    }

    public void setDeleteFilesOnRemoval(boolean delete)
    {
        mDeleteFilesOnRemoval = delete;
    }

    @Override
    public boolean setSelectableSelected(boolean selected)
    {
        mIsSelected = selected;
        return true;
    }

    @Override
    public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
    {
        this.dateCreated = System.currentTimeMillis();
    }

    @Override
    public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
    {

    }

    @Override
    public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
    {
        SQLQuery.Select objectSelection = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER).setWhere(
                String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID), String.valueOf(id));

        database.remove(dbInstance, new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE).setWhere(
                String.format("%s = ?", AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID), String.valueOf(id)));

        if (mDeleteFilesOnRemoval) {
            List<TransferObject> objects = database.castQuery(dbInstance, objectSelection,
                    TransferObject.class, null);

            for (TransferObject object : objects)
                object.setDeleteOnRemoval(true);

            database.remove(dbInstance, objects, null, this);
        } else
            database.removeAsObject(dbInstance, objectSelection, TransferObject.class, null, null,
                    this);
    }
}
