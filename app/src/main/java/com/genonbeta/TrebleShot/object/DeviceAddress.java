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

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * created by: veli
 * date: 8/3/19 1:22 PM
 */
public final class DeviceAddress implements DatabaseObject<Device>, Parcelable
{
    public static final Creator<DeviceAddress> CREATOR = new Creator<DeviceAddress>()
    {
        @Override
        public DeviceAddress createFromParcel(Parcel in)
        {
            return new DeviceAddress(in);
        }

        @Override
        public DeviceAddress[] newArray(int size)
        {
            return new DeviceAddress[size];
        }
    };

    public InetAddress inetAddress;
    public String deviceId;
    public long lastCheckedDate;

    public DeviceAddress()
    {
    }

    public DeviceAddress(InetAddress inetAddress)
    {
        this.inetAddress = inetAddress;
    }

    public DeviceAddress(String deviceId, InetAddress inetAddress, long lastCheckedDate)
    {
        this(inetAddress);
        this.deviceId = deviceId;
        this.lastCheckedDate = lastCheckedDate;
    }

    protected DeviceAddress(Parcel in)
    {
        deviceId = in.readString();
        inetAddress = (InetAddress) in.readSerializable();
        lastCheckedDate = in.readLong();
    }

    public String getHostAddress()
    {
        return inetAddress.getHostAddress();
    }

    @Override
    public SQLQuery.Select getWhere()
    {
        return new SQLQuery.Select(Kuick.TABLE_DEVICEADDRESS)
                .setWhere(Kuick.FIELD_DEVICEADDRESS_IPADDRESSTEXT + "=?", getHostAddress());
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(Kuick.FIELD_DEVICEADDRESS_DEVICEID, deviceId);
        values.put(Kuick.FIELD_DEVICEADDRESS_IPADDRESS, inetAddress.getAddress());
        values.put(Kuick.FIELD_DEVICEADDRESS_IPADDRESSTEXT, inetAddress.getHostAddress());
        values.put(Kuick.FIELD_DEVICEADDRESS_LASTCHECKEDDATE, lastCheckedDate);

        return values;
    }

    @Override
    public void reconstruct(SQLiteDatabase db, KuickDb kuick, ContentValues item)
    {
        try {
            this.inetAddress = InetAddress.getByAddress(item.getAsByteArray(Kuick.FIELD_DEVICEADDRESS_IPADDRESS));
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        this.deviceId = item.getAsString(Kuick.FIELD_DEVICEADDRESS_DEVICEID);
        this.lastCheckedDate = item.getAsLong(Kuick.FIELD_DEVICEADDRESS_LASTCHECKEDDATE);
    }

    @Override
    public void onCreateObject(SQLiteDatabase db, KuickDb kuick, Device parent, Progress.Listener listener)
    {

    }

    @Override
    public void onUpdateObject(SQLiteDatabase db, KuickDb kuick, Device parent, Progress.Listener listener)
    {

    }

    @Override
    public void onRemoveObject(SQLiteDatabase db, KuickDb kuick, Device parent, Progress.Listener listener)
    {

    }

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(deviceId);
        dest.writeSerializable(inetAddress);
        dest.writeLong(lastCheckedDate);
    }
}
