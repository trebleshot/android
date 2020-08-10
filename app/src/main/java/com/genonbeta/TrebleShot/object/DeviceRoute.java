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

public class DeviceRoute implements Parcelable
{
    public Device device;
    public DeviceAddress connection;

    public DeviceRoute(Device device, DeviceAddress connection)
    {
        this.device = device;
        this.connection = connection;
    }

    protected DeviceRoute(Parcel in)
    {
        device = in.readParcelable(Device.class.getClassLoader());
        connection = in.readParcelable(DeviceAddress.class.getClassLoader());
    }

    public static final Creator<DeviceRoute> CREATOR = new Creator<DeviceRoute>()
    {
        @Override
        public DeviceRoute createFromParcel(Parcel in)
        {
            return new DeviceRoute(in);
        }

        @Override
        public DeviceRoute[] newArray(int size)
        {
            return new DeviceRoute[size];
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
