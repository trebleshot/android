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
 * date: 7/31/19 11:00 AM
 */
class TransferObjectV12 : DatabaseObject<TransferGroupV12?> {
    var friendlyName: String? = null
    var file: String? = null
    var fileMimeType: String? = null
    var directory: String? = null
    var deviceId: String? = null
    var requestId: Long = 0
    var groupId: Long = 0
    var skippedBytes: Long = 0
    var fileSize: Long = 0
    var accessPort = 0
    var type = Type.INCOMING
    var flag = Flag.PENDING

    constructor() {}
    constructor(
        requestId: Long, groupId: Long, friendlyName: String?, file: String?, fileMime: String?,
        fileSize: Long, type: Type
    ) : this(requestId, groupId, null, friendlyName, file, fileMime, fileSize, type) {
    }

    constructor(
        requestId: Long, groupId: Long, deviceId: String?, friendlyName: String?, file: String?,
        fileMime: String?, fileSize: Long, type: Type
    ) {
        this.friendlyName = friendlyName
        this.file = file
        this.fileSize = fileSize
        fileMimeType = fileMime
        this.deviceId = deviceId
        this.requestId = requestId
        this.groupId = groupId
        this.type = type
    }

    constructor(requestId: Long, deviceId: String?, type: Type) {
        this.requestId = requestId
        this.deviceId = deviceId
        this.type = type
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is TransferObjectV12) return super.equals(obj)
        val otherObject = obj
        return otherObject.requestId == requestId && type == otherObject.type && ((deviceId == null
                && otherObject.deviceId == null) || (deviceId != null
                && deviceId == otherObject.deviceId))
    }

    fun isDivisionObject(): Boolean {
        return deviceId == null
    }

    override fun getWhere(): SQLQuery.Select {
        val whereClause = if (isDivisionObject()) String.format(
            "%s = ? AND %s = ?",
            Kuick.Companion.FIELD_TRANSFERITEM_ID,
            Kuick.Companion.FIELD_TRANSFERITEM_TYPE
        ) else String.format(
            "%s = ? AND %s = ? AND %s = ?", Kuick.Companion.FIELD_TRANSFERITEM_ID,
            Kuick.Companion.FIELD_TRANSFERITEM_TYPE, v12.Companion.FIELD_TRANSFER_DEVICEID
        )
        return if (isDivisionObject()) SQLQuery.Select(v12.Companion.TABLE_DIVISTRANSFER).setWhere(
            whereClause,
            requestId.toString(),
            type.toString()
        ) else SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM).setWhere(
            whereClause, requestId.toString(), type.toString(), deviceId
        )
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_ID, requestId)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID, groupId)
        values.put(v12.Companion.FIELD_TRANSFER_DEVICEID, deviceId)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_NAME, friendlyName)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_SIZE, fileSize)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_MIME, fileMimeType)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_FLAG, flag.toString())
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_TYPE, type.toString())
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_FILE, file)
        values.put(v12.Companion.FIELD_TRANSFER_ACCESSPORT, accessPort)
        values.put(v12.Companion.FIELD_TRANSFER_SKIPPEDBYTES, skippedBytes)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_DIRECTORY, directory)
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        friendlyName = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_NAME)
        file = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_FILE)
        fileSize = item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_SIZE)
        fileMimeType = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_MIME)
        requestId = item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_ID)
        groupId = item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID)
        deviceId = item.getAsString(v12.Companion.FIELD_TRANSFER_DEVICEID)
        type = Type.valueOf(item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_TYPE))

        // We may have put long in that field indicating that the file was / is in progress so generate
        try {
            flag = Flag.valueOf(item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_FLAG))
        } catch (e: Exception) {
            flag = Flag.IN_PROGRESS
            flag.setBytesValue(item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_FLAG))
        }
        accessPort = item.getAsInteger(v12.Companion.FIELD_TRANSFER_ACCESSPORT)
        skippedBytes = item.getAsLong(v12.Companion.FIELD_TRANSFER_SKIPPEDBYTES)
        directory = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_DIRECTORY)
    }

    override fun onCreateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: TransferGroupV12,
        listener: Progress.Listener
    ) {
    }

    override fun onUpdateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: TransferGroupV12,
        listener: Progress.Listener
    ) {
    }

    override fun onRemoveObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: TransferGroupV12,
        listener: Progress.Listener
    ) {
    }

    enum class Type {
        INCOMING, OUTGOING
    }

    enum class Flag {
        INTERRUPTED, PENDING, REMOVED, IN_PROGRESS, DONE;

        private var bytesValue: Long = 0
        fun getBytesValue(): Long {
            return bytesValue
        }

        fun setBytesValue(bytesValue: Long) {
            this.bytesValue = bytesValue
        }

        override fun toString(): String {
            return if (getBytesValue() > 0) getBytesValue().toString() else super.toString()
        }
    }
}