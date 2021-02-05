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
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.android.framework.``object`

/**
 * created by: veli
 * date: 8/3/19 1:35 PM
 */
open class TransferMember : DatabaseObject<Transfer?> {
    @JvmField
    var transferId: Long = 0
    @JvmField
    var deviceId: String? = null
    @JvmField
    var type: TransferItem.Type? = null

    constructor() {}
    constructor(transferId: Long, deviceId: String?, type: TransferItem.Type?) {
        this.transferId = transferId
        this.deviceId = deviceId
        this.type = type
    }

    constructor(transfer: Transfer, device: Device, type: TransferItem.Type) : this(transfer.id, device.uid, type) {}

    override fun equals(obj: Any?): Boolean {
        if (obj is TransferMember) {
            val otherMember = obj
            return otherMember.transferId == transferId && deviceId == otherMember.deviceId && type == otherMember.type
        }
        return super.equals(obj)
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER).setWhere(
            Kuick.FIELD_TRANSFERMEMBER_DEVICEID + "=? AND "
                    + Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=? AND "
                    + Kuick.FIELD_TRANSFERMEMBER_TYPE + "=?", deviceId, transferId.toString(), type.toString()
        )
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.FIELD_TRANSFERMEMBER_DEVICEID, deviceId)
        values.put(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID, transferId)
        values.put(Kuick.FIELD_TRANSFERMEMBER_TYPE, type.toString())
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        deviceId = item.getAsString(Kuick.FIELD_TRANSFERMEMBER_DEVICEID)
        transferId = item.getAsLong(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID)

        // Added in DB version 13 and might be null and may throw an error since ContentValues doesn't like it when
        // when the requested column name doesn't exist or has type different than requested.
        if (item.containsKey(Kuick.FIELD_TRANSFERMEMBER_TYPE)) type =
            TransferItem.Type.valueOf(item.getAsString(Kuick.FIELD_TRANSFERMEMBER_TYPE))
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener) {}
    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener) {}
    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener) {
        var parent = parent
        if (TransferItem.Type.INCOMING != type) return
        try {
            if (parent == null) {
                parent = Transfer(transferId)
                kuick.reconstruct<Device, Transfer>(db, parent)
            }
            val selection = Transfers.createIncomingSelection(
                transferId, TransferItem.Flag.INTERRUPTED,
                true
            )
            kuick.removeAsObject(db, selection, TransferItem::class.java, parent, listener, null)
        } catch (e: ReconstructionFailedException) {
            e.printStackTrace()
        }
    }
}