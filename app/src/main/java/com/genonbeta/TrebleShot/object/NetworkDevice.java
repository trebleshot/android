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

import java.io.Serializable;
import java.util.List;

public class NetworkDevice implements DatabaseObject<Object>, Serializable
{
    public String brand;
    public String model;
    public String nickname;
    public String id;
    public String versionName;
    public int versionCode;
    public int clientVersion;
    public int tmpSecureKey;
    public long lastUsageTime;
    public boolean isTrusted = false;
    public boolean isRestricted = false;
    public boolean isLocalAddress = false;
    public Type type = Type.NORMAL;

    public NetworkDevice()
    {
    }

    public NetworkDevice(String id)
    {
        this.id = id;
    }

    public NetworkDevice(ContentValues item)
    {
        reconstruct(item);
    }

    public String generatePictureId()
    {
        return String.format("picture_%s", id);
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(AccessDatabase.TABLE_DEVICES)
                .setWhere(AccessDatabase.FIELD_DEVICES_ID + "=?", id);
    }

    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(AccessDatabase.FIELD_DEVICES_ID, id);
        values.put(AccessDatabase.FIELD_DEVICES_USER, nickname);
        values.put(AccessDatabase.FIELD_DEVICES_BRAND, brand);
        values.put(AccessDatabase.FIELD_DEVICES_MODEL, model);
        values.put(AccessDatabase.FIELD_DEVICES_BUILDNAME, versionName);
        values.put(AccessDatabase.FIELD_DEVICES_BUILDNUMBER, versionCode);
        values.put(AccessDatabase.FIELD_DEVICES_CLIENTVERSION, clientVersion);
        values.put(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME, lastUsageTime);
        values.put(AccessDatabase.FIELD_DEVICES_ISRESTRICTED, isRestricted ? 1 : 0);
        values.put(AccessDatabase.FIELD_DEVICES_ISTRUSTED, isTrusted ? 1 : 0);
        values.put(AccessDatabase.FIELD_DEVICES_ISLOCALADDRESS, isLocalAddress ? 1 : 0);
        values.put(AccessDatabase.FIELD_DEVICES_TMPSECUREKEY, tmpSecureKey);
        values.put(AccessDatabase.FIELD_DEVICES_TYPE, type.toString());

        return values;
    }

    @Override
    public void reconstruct(ContentValues item)
    {
        this.id = item.getAsString(AccessDatabase.FIELD_DEVICES_ID);
        this.nickname = item.getAsString(AccessDatabase.FIELD_DEVICES_USER);
        this.brand = item.getAsString(AccessDatabase.FIELD_DEVICES_BRAND);
        this.model = item.getAsString(AccessDatabase.FIELD_DEVICES_MODEL);
        this.versionName = item.getAsString(AccessDatabase.FIELD_DEVICES_BUILDNAME);
        this.versionCode = item.getAsInteger(AccessDatabase.FIELD_DEVICES_BUILDNUMBER);
        this.lastUsageTime = item.getAsLong(AccessDatabase.FIELD_DEVICES_LASTUSAGETIME);
        this.isTrusted = item.getAsInteger(AccessDatabase.FIELD_DEVICES_ISTRUSTED) == 1;
        this.isRestricted = item.getAsInteger(AccessDatabase.FIELD_DEVICES_ISRESTRICTED) == 1;
        this.isLocalAddress = item.getAsInteger(AccessDatabase.FIELD_DEVICES_ISLOCALADDRESS) == 1;
        this.tmpSecureKey = item.getAsInteger(AccessDatabase.FIELD_DEVICES_TMPSECUREKEY);

        if (item.containsKey(AccessDatabase.FIELD_DEVICES_CLIENTVERSION))
            this.clientVersion = item.getAsInteger(AccessDatabase.FIELD_DEVICES_CLIENTVERSION);

        try {
            this.type = Type.valueOf(item.getAsString(AccessDatabase.FIELD_DEVICES_TYPE));
        } catch (Exception e) {
            this.type = Type.NORMAL;
        }
    }

    public void applyPreferences(NetworkDevice otherDevice)
    {
        isLocalAddress = otherDevice.isLocalAddress;
        isRestricted = otherDevice.isRestricted;
        isTrusted = otherDevice.isTrusted;
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
        database.getContext().deleteFile(generatePictureId());

        database.remove(dbInstance, new SQLQuery.Select(AccessDatabase.TABLE_DEVICECONNECTION)
                .setWhere(AccessDatabase.FIELD_DEVICECONNECTION_DEVICEID + "=?", id));

        List<TransferAssignee> assignees = database.castQuery(dbInstance, new SQLQuery.Select(
                AccessDatabase.TABLE_TRANSFERASSIGNEE).setWhere(AccessDatabase.FIELD_TRANSFERASSIGNEE_DEVICEID
                + "=?", id), TransferAssignee.class, null);

        for (TransferAssignee assignee : assignees)
            database.remove(dbInstance, assignee, null);
    }

    public enum Type
    {
        NORMAL,
        WEB
    }
}
