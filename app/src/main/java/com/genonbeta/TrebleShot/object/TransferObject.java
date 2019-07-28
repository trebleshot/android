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

import android.annotation.SuppressLint;
import android.content.ContentValues;

import com.genonbeta.TrebleShot.adapter.TransferListAdapter;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.framework.io.DocumentFile;

import org.json.JSONException;
import org.json.JSONObject;

import java.security.InvalidParameterException;
import java.util.Iterator;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */

public class TransferObject implements DatabaseObject<TransferGroup>, Editable
{
    public String name;
    public String file;
    public String mimeType;
    public String directory;
    public long id;
    public long groupId;
    public long size = 0;
    public long lastChangeDate;
    public Type type = Type.INCOMING;

    // When the type is outgoing, the sender gets to have device id : flag list
    protected final Map<String, Flag> mSenderFlagList = new ArrayMap<>();

    // When the type is incoming, the receiver will only have a flag for its status.
    private Flag mReceiverFlag = Flag.PENDING;

    private boolean mDeleteOnRemoval = false;
    private boolean mIsSelected = false;

    public TransferObject()
    {
    }

    public TransferObject(long id, long groupId, String name, String file,
                          String mimeType, long size, Type type)
    {
        this.name = name;
        this.file = file;
        this.size = size;
        this.mimeType = mimeType;
        this.id = id;
        this.groupId = groupId;
        this.type = type;
    }

    public TransferObject(long groupId, long id, Type type)
    {
        this.groupId = groupId;
        this.id = id;
        this.type = type;
    }

    public TransferObject(CursorItem item)
    {
        reconstruct(item);
    }

    @Override
    public boolean applyFilter(String[] filteringKeywords)
    {
        for (String keyword : filteringKeywords)
            if (name.contains(keyword))
                return true;

        return false;
    }

    @Override
    public boolean comparisonSupported()
    {
        return true;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof TransferObject))
            return super.equals(obj);

        TransferObject otherObject = (TransferObject) obj;
        return otherObject.id == id && type.equals(otherObject.type);
    }

    public Flag getFlag() {
        if (!Type.INCOMING.equals(type))
            throw new InvalidParameterException();

        return mReceiverFlag;
    }

    public Flag getFlag(String deviceId) {
        if (!Type.OUTGOING.equals(type))
            throw new InvalidParameterException();

        Flag flag;

        synchronized (mSenderFlagList) {
            flag = mSenderFlagList.get(deviceId);
        }

        return flag == null ? Flag.PENDING : flag;
    }

    public Flag[] getFlags() {
        synchronized (mSenderFlagList) {
            Flag[] flags = new Flag[mSenderFlagList.size()];
            mSenderFlagList.values().toArray(flags);
            return flags;
        }
    }

    public Map<String, Flag> getSenderFlagList() {
        synchronized (mSenderFlagList) {
            Map<String, Flag> map = new ArrayMap<>();
            map.putAll(mSenderFlagList);
            return map;
        }
    }

    public void setFlag(Flag flag) {
        if (!Type.INCOMING.equals(type))
            throw new InvalidParameterException();

        mReceiverFlag = flag;
    }

    public void putFlag(String deviceId, Flag flag) {
        if (!Type.OUTGOING.equals(type))
            throw new InvalidParameterException();

        synchronized (mSenderFlagList) {
            mSenderFlagList.put(deviceId, flag);
        }
    }

    public double getPercentage(ShowingAssignee[] assignees, @Nullable String deviceId)
    {
        if (assignees.length == 0)
            return 0;

        if (Type.INCOMING.equals(type))
            return TransferUtils.getPercentageByFlag(getFlag(), size);
        else if (deviceId != null)
            return TransferUtils.getPercentageByFlag(getFlag(deviceId), size);

        double percentageIndex = 0;
        int senderAssignees = 0;
        for (ShowingAssignee assignee : assignees) {
            if (!Type.OUTGOING.equals(assignee.type))
                continue;

            senderAssignees++;
            percentageIndex += TransferUtils.getPercentageByFlag(getFlag(
                    assignee.deviceId), size);
        }

        return percentageIndex > 0 ? percentageIndex / senderAssignees : 0;
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER).setWhere(
                String.format("%s = ? AND %s = ? AND %s = ?", AccessDatabase.FIELD_TRANSFER_GROUPID,
                        AccessDatabase.FIELD_TRANSFER_ID, AccessDatabase.FIELD_TRANSFER_TYPE),
                String.valueOf(groupId), String.valueOf(id), type.toString());
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(AccessDatabase.FIELD_TRANSFER_ID, id);
        values.put(AccessDatabase.FIELD_TRANSFER_GROUPID, groupId);
        values.put(AccessDatabase.FIELD_TRANSFER_NAME, name);
        values.put(AccessDatabase.FIELD_TRANSFER_SIZE, size);
        values.put(AccessDatabase.FIELD_TRANSFER_MIME, mimeType);
        values.put(AccessDatabase.FIELD_TRANSFER_TYPE, type.toString());
        values.put(AccessDatabase.FIELD_TRANSFER_FILE, file);
        values.put(AccessDatabase.FIELD_TRANSFER_DIRECTORY, directory);
        values.put(AccessDatabase.FIELD_TRANSFER_LASTCHANGETIME, lastChangeDate);

        if (Type.INCOMING.equals(type)) {
            values.put(AccessDatabase.FIELD_TRANSFER_FLAG, mReceiverFlag.toString());
        } else {
            JSONObject object = new JSONObject();

            synchronized (mSenderFlagList) {
                for (String deviceId : mSenderFlagList.keySet())
                    try {
                        object.put(deviceId, mSenderFlagList.get(deviceId));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
            }

            values.put(AccessDatabase.FIELD_TRANSFER_FLAG, object.toString());
        }

        return values;
    }

    @Override
    public void reconstruct(CursorItem item)
    {
        this.name = item.getString(AccessDatabase.FIELD_TRANSFER_NAME);
        this.file = item.getString(AccessDatabase.FIELD_TRANSFER_FILE);
        this.size = item.getLong(AccessDatabase.FIELD_TRANSFER_SIZE);
        this.mimeType = item.getString(AccessDatabase.FIELD_TRANSFER_MIME);
        this.id = item.getLong(AccessDatabase.FIELD_TRANSFER_ID);
        this.groupId = item.getLong(AccessDatabase.FIELD_TRANSFER_GROUPID);
        this.type = Type.valueOf(item.getString(AccessDatabase.FIELD_TRANSFER_TYPE));
        this.directory = item.getString(AccessDatabase.FIELD_TRANSFER_DIRECTORY);
        this.lastChangeDate = item.getLong(AccessDatabase.FIELD_TRANSFER_LASTCHANGETIME);
        String flagString = item.getString(AccessDatabase.FIELD_TRANSFER_FLAG);

        if (Type.INCOMING.equals(this.type)) {
            try {
                mReceiverFlag = Flag.valueOf(flagString);
            } catch (Exception e) {
                try {
                    mReceiverFlag = Flag.IN_PROGRESS;
                    mReceiverFlag.setBytesValue(Long.parseLong(flagString));
                } catch (NumberFormatException e1) {
                    mReceiverFlag = Flag.PENDING;
                }
            }
        } else {
            try {
                JSONObject jsonObject = new JSONObject(flagString);
                Iterator<String> iterator = jsonObject.keys();

                synchronized (mSenderFlagList) {
                    mSenderFlagList.clear();
                    while (iterator.hasNext()) {
                        String key = iterator.next();
                        String value = jsonObject.getString(key);
                        Flag flag;

                        try {
                            flag = Flag.valueOf(value);
                        } catch (Exception e) {
                            try {
                                flag = Flag.IN_PROGRESS;
                                flag.setBytesValue(Long.parseLong(value));
                            } catch (NumberFormatException e1) {
                                flag = Flag.PENDING;
                            }
                        }

                        mSenderFlagList.put(key, flag);
                    }
                }
            } catch (JSONException e) {
                // do nothing
            }
        }
    }

    public void setDeleteOnRemoval(boolean delete)
    {
        mDeleteOnRemoval = delete;
    }

    @Override
    public void onCreateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
    {
        lastChangeDate = System.currentTimeMillis();
    }

    @Override
    public void onUpdateObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
    {
        lastChangeDate = System.currentTimeMillis();
    }

    @Override
    public void onRemoveObject(android.database.sqlite.SQLiteDatabase dbInstance, SQLiteDatabase database, TransferGroup parent)
    {
        // Normally we'd like to check every file, but it may take a while.
        if (!Type.INCOMING.equals(type) || (!Flag.INTERRUPTED.equals(getFlag())
                && (!Flag.DONE.equals(getFlag()) || !mDeleteOnRemoval)))
            return;

        try {
            if (parent == null) {
                parent = new TransferGroup(groupId);
                database.reconstruct(parent);
            }

            DocumentFile file = FileUtils.getIncomingPseudoFile(database.getContext(),
                    this, parent, false);

            if (file != null && file.isFile())
                file.delete();
        } catch (Exception e) {
            // do nothing
        }
    }

    @Override
    public String getComparableName()
    {
        return getSelectableTitle();
    }

    @Override
    public long getComparableDate()
    {
        return lastChangeDate;
    }

    @Override
    public long getComparableSize()
    {
        return size;
    }

    @SuppressLint("DefaultLocale")
    @Override
    public long getId()
    {
        return String.format("%d_%d", id, type.ordinal()).hashCode();
    }

    @Override
    public void setId(long id)
    {
        // it will && should be effective on representative text items
        this.id = id;
    }

    @Override
    public String getSelectableTitle()
    {
        return name;
    }

    @Override
    public boolean isSelectableSelected()
    {
        return mIsSelected;
    }

    @Override
    public boolean setSelectableSelected(boolean selected)
    {
        mIsSelected = selected;
        return true;
    }

    public enum Type
    {
        INCOMING,
        OUTGOING
    }

    public enum Flag
    {
        INTERRUPTED,
        PENDING,
        REMOVED,
        IN_PROGRESS,
        DONE;

        private long bytesValue;

        public long getBytesValue()
        {
            return bytesValue;
        }

        public void setBytesValue(long bytesValue)
        {
            this.bytesValue = bytesValue;
        }

        @NonNull
        @Override
        public String toString()
        {
            return getBytesValue() > 0 ? String.valueOf(getBytesValue()) : super.toString();
        }
    }
}