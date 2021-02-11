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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.migration.db.Migration
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery

/**
 * created by: veli
 * date: 8/3/19 1:14 PM
 */
class TransferAssigneeV12 : DatabaseObject<NetworkDeviceV12?> {
    var groupId: Long = 0
    var deviceId: String? = null
    var connectionAdapter: String? = null

    constructor() {}
    constructor(groupId: Long, deviceId: String?) {
        this.groupId = groupId
        this.deviceId = deviceId
    }

    constructor(group: TransferGroupV12, device: NetworkDeviceV12) : this(group.groupId, device.deviceId) {}
    constructor(groupId: Long, deviceId: String?, connectionAdapter: String?) : this(groupId, deviceId) {
        this.connectionAdapter = connectionAdapter
    }

    override fun equals(other: Any?): Boolean {
        if (other is TransferAssigneeV12) {
            val otherAssignee = other
            return otherAssignee.groupId == groupId && deviceId == otherAssignee.deviceId
        }
        return super.equals(other)
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
            .setWhere(
                Kuick.FIELD_TRANSFERMEMBER_DEVICEID + "=? AND " +
                        Kuick.FIELD_TRANSFERMEMBER_TRANSFERID + "=?", deviceId, groupId.toString()
            )
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.FIELD_TRANSFERMEMBER_DEVICEID, deviceId)
        values.put(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID, groupId)
        values.put(Migration.v12.FIELD_TRANSFERASSIGNEE_ISCLONE, 1)
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        deviceId = item.getAsString(Kuick.FIELD_TRANSFERMEMBER_DEVICEID)
        groupId = item.getAsLong(Kuick.FIELD_TRANSFERMEMBER_TRANSFERID)
    }

    override fun onCreateObject(
        db: SQLiteDatabase?,
        kuick: KuickDb?,
        parent: NetworkDeviceV12?,
        listener: Progress.Listener?,
    ) {
        TODO("Not yet implemented")
    }

    override fun onCreateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: NetworkDeviceV12,
        listener: Progress.Listener,
    ) {
    }

    override fun onUpdateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: NetworkDeviceV12,
        listener: Progress.Listener,
    ) {
    }

    override fun onRemoveObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: NetworkDeviceV12,
        listener: Progress.Listener,
    ) {
    }
}