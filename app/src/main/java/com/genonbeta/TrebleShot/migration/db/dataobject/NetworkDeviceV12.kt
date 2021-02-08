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
import java.lang.Exception

/**
 * created by: veli
 * date: 7/31/19 11:03 AM
 */
class NetworkDeviceV12 : DatabaseObject<Any?> {
    var brand: String? = null
    var model: String? = null
    var nickname: String? = null
    var deviceId: String? = null
    var versionName: String? = null
    var versionNumber = 0
    var tmpSecureKey = 0
    var lastUsageTime: Long = 0
    var isTrusted = false
    var isRestricted = false
    var isLocalAddress = false

    constructor() {}
    constructor(deviceId: String?) {
        this.deviceId = deviceId
    }

    fun generatePictureId(): String {
        return String.format("picture_%s", deviceId)
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.Companion.TABLE_DEVICES)
            .setWhere(Kuick.Companion.FIELD_DEVICES_ID + "=?", deviceId)
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.Companion.FIELD_DEVICES_ID, deviceId)
        values.put(Kuick.Companion.FIELD_DEVICES_USER, nickname)
        values.put(Kuick.Companion.FIELD_DEVICES_BRAND, brand)
        values.put(Kuick.Companion.FIELD_DEVICES_MODEL, model)
        values.put(Kuick.Companion.FIELD_DEVICES_BUILDNAME, versionName)
        values.put(Kuick.Companion.FIELD_DEVICES_BUILDNUMBER, versionNumber)
        values.put(Kuick.Companion.FIELD_DEVICES_LASTUSAGETIME, lastUsageTime)
        values.put(Kuick.Companion.FIELD_DEVICES_ISRESTRICTED, if (isRestricted) 1 else 0)
        values.put(Kuick.Companion.FIELD_DEVICES_ISTRUSTED, if (isTrusted) 1 else 0)
        values.put(Kuick.Companion.FIELD_DEVICES_ISLOCALADDRESS, if (isLocalAddress) 1 else 0)
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        deviceId = item.getAsString(Kuick.Companion.FIELD_DEVICES_ID)
        nickname = item.getAsString(Kuick.Companion.FIELD_DEVICES_USER)
        brand = item.getAsString(Kuick.Companion.FIELD_DEVICES_BRAND)
        model = item.getAsString(Kuick.Companion.FIELD_DEVICES_MODEL)
        versionName = item.getAsString(Kuick.Companion.FIELD_DEVICES_BUILDNAME)
        versionNumber = item.getAsInteger(Kuick.Companion.FIELD_DEVICES_BUILDNUMBER)
        lastUsageTime = item.getAsLong(Kuick.Companion.FIELD_DEVICES_LASTUSAGETIME)
        isTrusted = item.getAsInteger(Kuick.Companion.FIELD_DEVICES_ISTRUSTED) == 1
        isRestricted = item.getAsInteger(Kuick.Companion.FIELD_DEVICES_ISRESTRICTED) == 1
        isLocalAddress = item.getAsInteger(Kuick.Companion.FIELD_DEVICES_ISLOCALADDRESS) == 1
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {
        kuick.getContext().deleteFile(generatePictureId())
        kuick.remove(
            db, SQLQuery.Select(Kuick.Companion.TABLE_DEVICEADDRESS)
                .setWhere(Kuick.Companion.FIELD_DEVICEADDRESS_DEVICEID + "=?", deviceId)
        )
        val assignees: List<TransferAssigneeV12> = kuick.castQuery<NetworkDeviceV12, TransferAssigneeV12>(
            db, SQLQuery.Select(
                Kuick.Companion.TABLE_TRANSFERMEMBER
            ).setWhere(
                Kuick.Companion.FIELD_TRANSFERMEMBER_DEVICEID + "=?",
                deviceId
            ), TransferAssigneeV12::class.java, null
        )

        // We are ensuring that the transfer group is still valid for other devices
        for (assignee in assignees) {
            kuick.remove<NetworkDeviceV12, TransferAssigneeV12>(db, assignee, this, listener)
            try {
                val transferGroup = TransferGroupV12(assignee.groupId)
                kuick.reconstruct<NetworkDeviceV12, TransferGroupV12>(db, transferGroup)
                val relatedAssignees: List<TransferAssigneeV12> =
                    kuick.castQuery<NetworkDeviceV12, TransferAssigneeV12>(
                        SQLQuery.Select(
                            Kuick.Companion.TABLE_TRANSFERMEMBER
                        ).setWhere(
                            Kuick.Companion.FIELD_TRANSFERMEMBER_TRANSFERID + "=?",
                            transferGroup.groupId.toString()
                        ), TransferAssigneeV12::class.java
                    )
                if (relatedAssignees.size == 0) kuick.remove<NetworkDeviceV12, TransferGroupV12>(
                    db,
                    transferGroup,
                    this,
                    listener
                )
            } catch (ignored: Exception) {
            }
        }
    }
}