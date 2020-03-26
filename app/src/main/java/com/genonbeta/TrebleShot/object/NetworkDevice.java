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
import android.os.Parcel;
import android.os.Parcelable;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.android.database.DatabaseObject;
import com.genonbeta.android.database.KuickDb;
import com.genonbeta.android.database.Progress;
import com.genonbeta.android.database.SQLQuery;

import java.io.Serializable;
import java.util.List;

public final class NetworkDevice implements DatabaseObject<Void>, Parcelable
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

    private boolean mIsSelected = false;

    public NetworkDevice()
    {
    }

    public NetworkDevice(String id)
    {
        this.id = id;
    }

    protected NetworkDevice(Parcel in)
    {
        brand = in.readString();
        model = in.readString();
        nickname = in.readString();
        id = in.readString();
        versionName = in.readString();
        versionCode = in.readInt();
        clientVersion = in.readInt();
        secureKey = in.readInt();
        lastUsageTime = in.readLong();
        isTrusted = in.readByte() != 0;
        isRestricted = in.readByte() != 0;
        isLocal = in.readByte() != 0;
        mIsSelected = in.readByte() != 0;
    }

    public static final Creator<NetworkDevice> CREATOR = new Creator<NetworkDevice>()
    {
        @Override
        public NetworkDevice createFromParcel(Parcel in)
        {
            return new NetworkDevice(in);
        }

        @Override
        public NetworkDevice[] newArray(int size)
        {
            return new NetworkDevice[size];
        }
    };

    public void applyPreferences(NetworkDevice otherDevice)
    {
        isLocal = otherDevice.isLocal;
        isRestricted = otherDevice.isRestricted;
        isTrusted = otherDevice.isTrusted;
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
    public void reconstruct(SQLiteDatabase db, KuickDb kuick, ContentValues item)
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

    @Override
    public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Void parent, Progress.Listener listener)
    {
        checkSecureKey();
    }

    @Override
    public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Void parent, Progress.Listener listener)
    {
        checkSecureKey();
    }

    @Override
    public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Void parent, Progress.Listener listener)
    {
        kuick.getContext().deleteFile(generatePictureId());

        kuick.remove(db, new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION)
                .setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=?", id));

        List<TransferAssignee> assignees = kuick.castQuery(db, new SQLQuery.Select(
                Kuick.TABLE_TRANSFERASSIGNEE).setWhere(Kuick.FIELD_TRANSFERASSIGNEE_DEVICEID
                + "=?", id), TransferAssignee.class, null);

        for (TransferAssignee assignee : assignees)
            kuick.remove(db, assignee, null, listener);
    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(brand);
        dest.writeString(model);
        dest.writeString(nickname);
        dest.writeString(id);
        dest.writeString(versionName);
        dest.writeInt(versionCode);
        dest.writeInt(clientVersion);
        dest.writeInt(secureKey);
        dest.writeLong(lastUsageTime);
        dest.writeByte((byte) (isTrusted ? 1 : 0));
        dest.writeByte((byte) (isRestricted ? 1 : 0));
        dest.writeByte((byte) (isLocal ? 1 : 0));
        dest.writeByte((byte) (mIsSelected ? 1 : 0));
    }

    public enum Type
    {
        NORMAL,
        WEB
    }
}
