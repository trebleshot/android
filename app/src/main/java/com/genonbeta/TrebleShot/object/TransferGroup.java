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

import androidx.annotation.NonNull;

import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.object.Selectable;

import java.lang.reflect.Field;
import java.util.ArrayList;
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

    public TransferGroup(CursorItem item)
    {
        reconstruct(item);
    }

    @Override
    public boolean equals(Object obj)
    {
        return obj instanceof TransferGroup && ((TransferGroup) obj).id == id;
    }

    @Override
    public void reconstruct(CursorItem item)
    {
        this.id = item.getLong(AccessDatabase.FIELD_TRANSFERGROUP_ID);
        this.savePath = item.getString(AccessDatabase.FIELD_TRANSFERGROUP_SAVEPATH);
        this.dateCreated = item.getLong(AccessDatabase.FIELD_TRANSFERGROUP_DATECREATED);
        this.isServedOnWeb = item.getInt(AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB) == 1;
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
        database.remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID), String.valueOf(id)));

        database.remove(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE)
                .setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=?", String.valueOf(id)));

        SQLQuery.Select objectSelection = new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
                .setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(id));

        if (mDeleteFilesOnRemoval) {
            List<TransferObject> objects = database.castQuery(dbInstance, objectSelection,
                    TransferObject.class, null);

            for (TransferObject object : objects)
                object.setDeleteOnRemoval(true);

            database.remove(dbInstance, objects, null, this);
        } else
            database.removeAsObject(dbInstance, objectSelection, TransferObject.class, null, this);
    }

    public static class Index
    {
        public boolean calculated = false;
        public boolean hasIssues = false;
        public long incoming = 0;
        public long incomingCompleted = 0;
        public long outgoing = 0;
        public long outgoingCompleted = 0;
        public int incomingCount = 0;
        public int outgoingCount = 0;
        public int incomingCountCompleted;
        public int outgoingCountCompleted;
        public List<ShowingAssignee> assignees = new ArrayList<>();

        public void reset()
        {
            calculated = false;
            hasIssues = false;

            incoming = 0;
            outgoing = 0;
            incomingCount = 0;
            outgoingCount = 0;
            assignees.clear();
        }
    }

    public static class Assignee implements DatabaseObject<NetworkDevice>
    {
        public long groupId;
        public String deviceId;
        public String connectionAdapter;
        public TransferObject.Type type;

        public Assignee()
        {

        }

        public Assignee(long groupId, String deviceId, TransferObject.Type type)
        {
            this.groupId = groupId;
            this.deviceId = deviceId;
            this.type = type;
        }

        public Assignee(@NonNull TransferGroup group, @NonNull NetworkDevice device,
                        @NonNull TransferObject.Type type)
        {
            this(group.id, device.id, type);
        }

        public Assignee(long groupId, String deviceId, TransferObject.Type type, String connectionAdapter)
        {
            this(groupId, deviceId, type);
            this.connectionAdapter = connectionAdapter;
        }

        public Assignee(@NonNull TransferGroup group, @NonNull NetworkDevice device,
                        @NonNull TransferObject.Type type,
                        @NonNull NetworkDevice.Connection connection)
        {
            this(group.id, device.id, type, connection.adapterName);
        }

        @Override
        public SQLQuery.Select getWhere()
        {
            return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERASSIGNEE).setWhere(
                    AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID + "=? AND "
                            + AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID + "=? AND "
                            + AccessDatabase.FIELD_TRANSFERASSIGNEE_TYPE + "=?", deviceId,
                    String.valueOf(groupId), type.toString());
        }

        @Override
        public ContentValues getValues()
        {
            ContentValues values = new ContentValues();

            values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID, deviceId);
            values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID, groupId);
            values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER, connectionAdapter);
            values.put(AccessDatabase.FIELD_TRANSFERASSIGNEE_TYPE, type.toString());

            return values;
        }

        @Override
        public void reconstruct(CursorItem item)
        {
            this.deviceId = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID);
            this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFERASSIGNEE_GROUPID);
            this.connectionAdapter = item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_CONNECTIONADAPTER);
            this.type = TransferObject.Type.valueOf(item.getString(AccessDatabase.FIELD_TRANSFERASSIGNEE_TYPE));
        }

        @Override
        public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
        {

        }

        @Override
        public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
        {

        }

        @Override
        public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, NetworkDevice parent)
        {
            // Removing an assignee would also remove the files belonging to it, however,
            // new design makes that action obsolete because the files are always held
            // in one place in the database.
        }
    }
}
