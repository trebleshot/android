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
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.SQLQuery;

import java.io.Serializable;
import java.util.List;

public class NetworkDevice implements DatabaseObject<Void>, Serializable
{
    public String brand;
    public String model;
    public String nickname;
    public String id;
    public String versionName;
    public int versionCode;
    public int clientVersion;
    public int secureKey = -1;
    public long lastUsageTime;
    public boolean isTrusted = false;
    public boolean isRestricted = false;
    public boolean isLocal = false;
    public Type type = Type.NORMAL;

    public NetworkDevice()
    {
    }

    public NetworkDevice(String id)
    {
        this.id = id;
    }

    private void checkSecureKey()
    {
        if (secureKey < 0)
            throw new RuntimeException("Secure key for " + nickname + " cannot be invalid when the device is saved");
    }

    public String generatePictureId()
    {
        return String.format("picture_%s", id);
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(Kuick.TABLE_DEVICES)
                .setWhere(Kuick.FIELD_DEVICES_ID + "=?", id);
    }

    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(Kuick.FIELD_DEVICES_ID, id);
        values.put(Kuick.FIELD_DEVICES_USER, nickname);
        values.put(Kuick.FIELD_DEVICES_BRAND, brand);
        values.put(Kuick.FIELD_DEVICES_MODEL, model);
        values.put(Kuick.FIELD_DEVICES_BUILDNAME, versionName);
        values.put(Kuick.FIELD_DEVICES_BUILDNUMBER, versionCode);
        values.put(Kuick.FIELD_DEVICES_CLIENTVERSION, clientVersion);
        values.put(Kuick.FIELD_DEVICES_LASTUSAGETIME, lastUsageTime);
        values.put(Kuick.FIELD_DEVICES_ISRESTRICTED, isRestricted ? 1 : 0);
        values.put(Kuick.FIELD_DEVICES_ISTRUSTED, isTrusted ? 1 : 0);
        values.put(Kuick.FIELD_DEVICES_ISLOCALADDRESS, isLocal ? 1 : 0);
        values.put(Kuick.FIELD_DEVICES_SECUREKEY, secureKey);
        values.put(Kuick.FIELD_DEVICES_TYPE, type.toString());

        return values;
    }

    @Override
    public void reconstruct(ContentValues item)
    {
        this.id = item.getAsString(Kuick.FIELD_DEVICES_ID);
        this.nickname = item.getAsString(Kuick.FIELD_DEVICES_USER);
        this.brand = item.getAsString(Kuick.FIELD_DEVICES_BRAND);
        this.model = item.getAsString(Kuick.FIELD_DEVICES_MODEL);
        this.versionName = item.getAsString(Kuick.FIELD_DEVICES_BUILDNAME);
        this.versionCode = item.getAsInteger(Kuick.FIELD_DEVICES_BUILDNUMBER);
        this.lastUsageTime = item.getAsLong(Kuick.FIELD_DEVICES_LASTUSAGETIME);
        this.isTrusted = item.getAsInteger(Kuick.FIELD_DEVICES_ISTRUSTED) == 1;
        this.isRestricted = item.getAsInteger(Kuick.FIELD_DEVICES_ISRESTRICTED) == 1;
        this.isLocal = item.getAsInteger(Kuick.FIELD_DEVICES_ISLOCALADDRESS) == 1;
        this.secureKey = item.getAsInteger(Kuick.FIELD_DEVICES_SECUREKEY);

        if (item.containsKey(Kuick.FIELD_DEVICES_CLIENTVERSION))
            this.clientVersion = item.getAsInteger(Kuick.FIELD_DEVICES_CLIENTVERSION);

        try {
            this.type = Type.valueOf(item.getAsString(Kuick.FIELD_DEVICES_TYPE));
        } catch (Exception e) {
            this.type = Type.NORMAL;
        }
    }

    public void applyPreferences(NetworkDevice otherDevice)
    {
        isLocal = otherDevice.isLocal;
        isRestricted = otherDevice.isRestricted;
        isTrusted = otherDevice.isTrusted;
    }

    @Override
    public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Void parent)
    {
        checkSecureKey();
    }

    @Override
    public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Void parent)
    {
        checkSecureKey();
    }

    @Override
    public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Void parent)
    {
        kuick.getContext().deleteFile(generatePictureId());

        kuick.remove(db, new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?", id));

        List<TransferAssignee> assignees = kuick.castQuery(db, new SQLQuery.Select(
                Kuick.TABLE_TRANSFERASSIGNEE).setWhere(Kuick.FIELD_TRANSFERASSIGNEE_DEVICEID
                + "=?", id), TransferAssignee.class, null);

        for (TransferAssignee assignee : assignees)
            kuick.remove(db, assignee, null);
    }

    public enum Type
    {
        NORMAL,
        WEB
    }
}
