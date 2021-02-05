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
package com.genonbeta.TrebleShot.dataobject

import com.genonbeta.TrebleShot.io.Containable
import com.genonbeta.android.database.DatabaseObject
import android.os.Parcelable
import android.os.Parcel
import androidx.core.util.ObjectsCompat
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.TrebleShot.database.Kuick
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.TrebleShot.dataobject.TransferMember
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.dataobject.DeviceRoute
import com.genonbeta.android.framework.``object`

class DeviceRoute : Parcelable {
    @JvmField
    var device: Device?
    @JvmField
    var address: DeviceAddress?

    constructor(device: Device?, address: DeviceAddress?) {
        this.device = device
        this.address = address
    }

    protected constructor(`in`: Parcel) {
        device = `in`.readParcelable(Device::class.java.classLoader)
        address = `in`.readParcelable(DeviceAddress::class.java.classLoader)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(device, flags)
        dest.writeParcelable(address, flags)
    }

    companion object CREATOR : Creator<DeviceRoute> {
        override fun createFromParcel(parcel: Parcel): DeviceRoute {
            return DeviceRoute(parcel)
        }

        override fun newArray(size: Int): Array<DeviceRoute?> {
            return arrayOfNulls(size)
        }
    }
}