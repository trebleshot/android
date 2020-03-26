/*
 * Copyright (C) 2020 Veli Tasalı
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

import android.os.Parcel;
import android.os.Parcelable;

public class DeviceAddress implements Parcelable
{
    public NetworkDevice device;
    public DeviceConnection connection;

    public DeviceAddress(NetworkDevice device, DeviceConnection connection)
    {
        this.device = device;
        this.connection = connection;
    }

    protected DeviceAddress(Parcel in)
    {
        device = in.readParcelable(NetworkDevice.class.getClassLoader());
        connection = in.readParcelable(DeviceConnection.class.getClassLoader());
    }

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

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeParcelable(device, flags);
        dest.writeParcelable(connection, flags);
    }
}
