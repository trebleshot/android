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
package com.genonbeta.TrebleShot.migration.db.dataobject

import com.genonbeta.TrebleShot.database.Kuick

/**
 * created by: veli
 * date: 7/31/19 11:02 AM
 */
class TransferGroupV12 : DatabaseObject<NetworkDeviceV12?> {
    var groupId: Long = 0
    var dateCreated: Long = 0
    var savePath: String? = null
    var isServedOnWeb = false

    constructor() {}
    constructor(groupId: Long) {
        this.groupId = groupId
    }

    override fun equals(obj: Any?): Boolean {
        return obj is TransferGroupV12 && obj.groupId == groupId
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        groupId = item.getAsLong(Kuick.FIELD_TRANSFER_ID)
        savePath = item.getAsString(Kuick.FIELD_TRANSFER_SAVEPATH)
        dateCreated = item.getAsLong(Kuick.FIELD_TRANSFER_DATECREATED)
        isServedOnWeb = item.getAsInteger(Kuick.FIELD_TRANSFER_ISSHAREDONWEB) == 1
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.FIELD_TRANSFER_ID, groupId)
        values.put(Kuick.FIELD_TRANSFER_SAVEPATH, savePath)
        values.put(Kuick.FIELD_TRANSFER_DATECREATED, dateCreated)
        values.put(Kuick.FIELD_TRANSFER_ISSHAREDONWEB, if (isServedOnWeb) 1 else 0)
        return values
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_TRANSFER)
            .setWhere(Kuick.FIELD_TRANSFER_ID + "=?", groupId.toString())
    }

    override fun onCreateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: NetworkDeviceV12,
        listener: Progress.Listener
    ) {
        dateCreated = System.currentTimeMillis()
    }

    override fun onUpdateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: NetworkDeviceV12,
        listener: Progress.Listener
    ) {
    }

    override fun onRemoveObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: NetworkDeviceV12,
        listener: Progress.Listener
    ) {
        kuick.remove(
            db, SQLQuery.Select(v12.TABLE_DIVISTRANSFER)
                .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID), groupId.toString())
        )
        kuick.remove(
            db, SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
                .setWhere(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?", groupId.toString())
        )
        kuick.removeAsObject<TransferGroupV12, TransferObjectV12>(
            db, SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
                .setWhere(Kuick.FIELD_TRANSFERITEM_TRANSFERID + "=?", groupId.toString()),
            TransferObjectV12::class.java, this, listener, null
        )
    }
}