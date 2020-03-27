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

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * created by: veli
 * date: 8/3/19 1:22 PM
 */
public final class DeviceConnection implements DatabaseObject<Device>, Parcelable
{
    public String adapterName;
    public String ipAddress;
    public String deviceId;
    public long lastCheckedDate;

    public DeviceConnection()
    {
    }

    public DeviceConnection(String adapterName, String ipAddress, String deviceId, long lastCheckedDate)
    {
        this.adapterName = adapterName;
        this.ipAddress = ipAddress;
        this.deviceId = deviceId;
        this.lastCheckedDate = lastCheckedDate;
    }

    public DeviceConnection(String deviceId, String adapterName)
    {
        this.deviceId = deviceId;
        this.adapterName = adapterName;
    }

    public DeviceConnection(TransferAssignee assignee)
    {
        this(assignee.deviceId, assignee.connectionAdapter);
    }

    public DeviceConnection(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    protected DeviceConnection(Parcel in)
    {
        adapterName = in.readString();
        ipAddress = in.readString();
        deviceId = in.readString();
        lastCheckedDate = in.readLong();
    }

    public static final Creator<DeviceConnection> CREATOR = new Creator<DeviceConnection>()
    {
        @Override
        public DeviceConnection createFromParcel(Parcel in)
        {
            return new DeviceConnection(in);
        }

        @Override
        public DeviceConnection[] newArray(int size)
        {
            return new DeviceConnection[size];
        }
    };

    @Override
    public SQLQuery.Select getWhere()
    {
        SQLQuery.Select select = new SQLQuery.Select(Kuick.TABLE_DEVICECONNECTION);

        return ipAddress == null ? select.setWhere(Kuick.FIELD_DEVICECONNECTION_DEVICEID + "=? AND "
                + Kuick.FIELD_DEVICECONNECTION_ADAPTERNAME + "=?", deviceId, adapterName)
                : select.setWhere(Kuick.FIELD_DEVICECONNECTION_IPADDRESS + "=?", ipAddress);
    }

    @Override
    public ContentValues getValues()
    {
        ContentValues values = new ContentValues();

        values.put(Kuick.FIELD_DEVICECONNECTION_DEVICEID, deviceId);
        values.put(Kuick.FIELD_DEVICECONNECTION_ADAPTERNAME, adapterName);
        values.put(Kuick.FIELD_DEVICECONNECTION_IPADDRESS, ipAddress);
        values.put(Kuick.FIELD_DEVICECONNECTION_LASTCHECKEDDATE, lastCheckedDate);

        return values;
    }

    @Override
    public void reconstruct(SQLiteDatabase db, KuickDb kuick, ContentValues item)
    {
        this.adapterName = item.getAsString(Kuick.FIELD_DEVICECONNECTION_ADAPTERNAME);
        this.ipAddress = item.getAsString(Kuick.FIELD_DEVICECONNECTION_IPADDRESS);
        this.deviceId = item.getAsString(Kuick.FIELD_DEVICECONNECTION_DEVICEID);
        this.lastCheckedDate = item.getAsLong(Kuick.FIELD_DEVICECONNECTION_LASTCHECKEDDATE);
    }

    public Inet4Address toInet4Address() throws UnknownHostException
    {
        return (Inet4Address) InetAddress.getByName(ipAddress);
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
        dest.writeString(adapterName);
        dest.writeString(ipAddress);
        dest.writeString(deviceId);
        dest.writeLong(lastCheckedDate);
    }
}
