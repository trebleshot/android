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
package com.genonbeta.TrebleShot.dataobject

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery
import java.net.InetAddress
import java.net.UnknownHostException

/**
 * created by: veli
 * date: 8/3/19 1:22 PM
 */
class DeviceAddress() : DatabaseObject<Device>, Parcelable {
    lateinit var inetAddress: InetAddress

    lateinit var deviceId: String

    var lastCheckedDate: Long = 0

    constructor(inetAddress: InetAddress) : this() {
        this.inetAddress = inetAddress
    }

    constructor(deviceId: String, inetAddress: InetAddress, lastCheckedDate: Long) : this(inetAddress) {
        this.deviceId = deviceId
        this.lastCheckedDate = lastCheckedDate
    }

    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readSerializable() as InetAddress? ?: InetAddress.getLocalHost(),
        parcel.readLong()
    )

    val hostAddress: String
        get() = inetAddress.hostAddress

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_DEVICEADDRESS)
            .setWhere(Kuick.FIELD_DEVICEADDRESS_IPADDRESSTEXT + "=?", hostAddress)
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.FIELD_DEVICEADDRESS_DEVICEID, deviceId)
        values.put(Kuick.FIELD_DEVICEADDRESS_IPADDRESS, inetAddress.address)
        values.put(Kuick.FIELD_DEVICEADDRESS_IPADDRESSTEXT, inetAddress.hostAddress)
        values.put(Kuick.FIELD_DEVICEADDRESS_LASTCHECKEDDATE, lastCheckedDate)
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, values: ContentValues) {
        try {
            inetAddress = InetAddress.getByAddress(values.getAsByteArray(Kuick.FIELD_DEVICEADDRESS_IPADDRESS))
        } catch (e: UnknownHostException) {
            e.printStackTrace()
        }
        deviceId = values.getAsString(Kuick.FIELD_DEVICEADDRESS_DEVICEID)
        lastCheckedDate = values.getAsLong(Kuick.FIELD_DEVICEADDRESS_LASTCHECKEDDATE)
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {}

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {}

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {}

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(deviceId)
        dest.writeSerializable(inetAddress)
        dest.writeLong(lastCheckedDate)
    }

    companion object CREATOR : Creator<DeviceAddress> {
        override fun createFromParcel(parcel: Parcel): DeviceAddress {
            return DeviceAddress(parcel)
        }

        override fun newArray(size: Int): Array<DeviceAddress?> {
            return arrayOfNulls(size)
        }
    }
}