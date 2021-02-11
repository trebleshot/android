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
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Files
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.io.StreamInfo
import org.json.JSONException
import org.json.JSONObject
import java.security.InvalidParameterException

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */
open class TransferItem : DatabaseObject<Transfer?>, Editable {
    lateinit var name: String

    var file: String? = null

    var mimeType: String? = null

    var directory: String? = null

    override var id: Long = 0

    var length: Long = 0

    var date: Long = 0

    var transferId: Long = 0

    var type = Type.INCOMING

    // When the type is outgoing, the sender gets to have device id : flag list

    protected val senderFlagList1: MutableMap<String, Flag> = ArrayMap()

    // When the type is incoming, the receiver will only have a flag for its status.
    private var receiverFlag = Flag.PENDING

    private var deleteOnRemoval = false

    protected val isSelected = false

    constructor() {}

    constructor(id: Long, transferId: Long, name: String, file: String?, mimeType: String?, size: Long, type: Type) {
        this.id = id
        this.transferId = transferId
        this.name = name
        this.file = file
        this.mimeType = mimeType
        this.length = size
        this.type = type
    }

    constructor(transferId: Long, id: Long, type: Type) {
        this.transferId = transferId
        this.id = id
        this.type = type
    }

    override fun applyFilter(filteringKeywords: Array<String>): Boolean {
        for (keyword in filteringKeywords) if (name!!.contains(keyword)) return true
        return false
    }

    override fun comparisonSupported(): Boolean {
        return true
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is TransferItem) return super.equals(obj)
        val otherObject = obj
        return otherObject.id == id && type == otherObject.type
    }

    var flag: Flag
        get() {
            if (Type.INCOMING != type) throw InvalidParameterException()
            return receiverFlag
        }
        set(flag) {
            if (Type.INCOMING != type) throw InvalidParameterException()
            receiverFlag = flag
        }

    fun getFlag(deviceId: String?): Flag {
        if (Type.OUTGOING != type) throw InvalidParameterException()
        var flag: Flag?
        synchronized(senderFlagList1) { flag = senderFlagList1[deviceId] }
        return if (flag == null) Flag.PENDING else flag!!
    }

    val flags: Array<Flag?>
        get() {
            synchronized(senderFlagList1) {
                val flags = arrayOfNulls<Flag>(senderFlagList1.size)
                senderFlagList1.values.toArray(flags)
                return flags
            }
        }
    val senderFlagList: Map<String?, Flag>
        get() {
            synchronized(senderFlagList1) {
                val map: MutableMap<String?, Flag> = ArrayMap()
                map.putAll(senderFlagList1)
                return map
            }
        }

    fun putFlag(deviceId: String?, flag: Flag) {
        if (Type.OUTGOING != type) throw InvalidParameterException()
        synchronized(senderFlagList1) { senderFlagList1.put(deviceId, flag) }
    }

    fun getPercentage(members: Array<LoadedMember>, deviceId: String?): Double {
        if (members.isEmpty()) return 0.0
        if (Type.INCOMING == type) return Transfers.getPercentageByFlag(
            flag, length
        ) else if (deviceId != null) return Transfers.getPercentageByFlag(getFlag(deviceId), length)
        var percentageIndex = 0.0
        var senderMembers = 0
        for (member in members) {
            if (Type.OUTGOING != member.type) continue
            senderMembers++
            percentageIndex += Transfers.getPercentageByFlag(getFlag(member.deviceId), length)
        }
        return if (percentageIndex > 0) percentageIndex / senderMembers else 0.0
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_TRANSFERITEM).setWhere(
            String.format(
                "%s = ? AND %s = ? AND %s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID,
                Kuick.FIELD_TRANSFERITEM_ID, Kuick.FIELD_TRANSFERITEM_TYPE
            ), transferId.toString(), id.toString(), type.toString()
        )
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.FIELD_TRANSFERITEM_ID, id)
        values.put(Kuick.FIELD_TRANSFERITEM_TRANSFERID, transferId)
        values.put(Kuick.FIELD_TRANSFERITEM_NAME, name)
        values.put(Kuick.FIELD_TRANSFERITEM_SIZE, length)
        values.put(Kuick.FIELD_TRANSFERITEM_MIME, mimeType)
        values.put(Kuick.FIELD_TRANSFERITEM_TYPE, type.toString())
        values.put(Kuick.FIELD_TRANSFERITEM_FILE, file)
        values.put(Kuick.FIELD_TRANSFERITEM_DIRECTORY, directory)
        values.put(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME, date)
        if (Type.INCOMING == type) {
            values.put(Kuick.FIELD_TRANSFERITEM_FLAG, receiverFlag.toString())
        } else {
            val `object` = JSONObject()
            synchronized(senderFlagList1) {
                for (deviceId in senderFlagList1.keys) try {
                    `object`.put(deviceId, senderFlagList1[deviceId])
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            values.put(Kuick.FIELD_TRANSFERITEM_FLAG, `object`.toString())
        }
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        name = item.getAsString(Kuick.FIELD_TRANSFERITEM_NAME)
        file = item.getAsString(Kuick.FIELD_TRANSFERITEM_FILE)
        size = item.getAsLong(Kuick.FIELD_TRANSFERITEM_SIZE)
        mimeType = item.getAsString(Kuick.FIELD_TRANSFERITEM_MIME)
        id = item.getAsLong(Kuick.FIELD_TRANSFERITEM_ID)
        transferId = item.getAsLong(Kuick.FIELD_TRANSFERITEM_TRANSFERID)
        type = Type.valueOf(item.getAsString(Kuick.FIELD_TRANSFERITEM_TYPE))
        directory = item.getAsString(Kuick.FIELD_TRANSFERITEM_DIRECTORY)

        // Added with DB version 13
        if (item.containsKey(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME)) date =
            item.getAsLong(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME)
        val flagString = item.getAsString(Kuick.FIELD_TRANSFERITEM_FLAG)
        if (Type.INCOMING == type) {
            try {
                receiverFlag = Flag.valueOf(flagString)
            } catch (e: Exception) {
                try {
                    receiverFlag = Flag.IN_PROGRESS
                    receiverFlag.bytesValue = flagString.toLong()
                } catch (e1: NumberFormatException) {
                    receiverFlag = Flag.PENDING
                }
            }
        } else {
            try {
                val jsonObject = JSONObject(flagString)
                val iterator = jsonObject.keys()
                synchronized(senderFlagList1) {
                    senderFlagList1.clear()
                    while (iterator.hasNext()) {
                        val key = iterator.next()
                        val value = jsonObject.getString(key)
                        var flag: Flag
                        try {
                            flag = Flag.valueOf(value)
                        } catch (e: Exception) {
                            try {
                                flag = Flag.IN_PROGRESS
                                flag.bytesValue = value.toLong()
                            } catch (e1: NumberFormatException) {
                                flag = Flag.PENDING
                            }
                        }
                        senderFlagList1[key] = flag
                    }
                }
            } catch (ignored: JSONException) {
            }
        }
    }

    fun setDeleteOnRemoval(delete: Boolean) {
        deleteOnRemoval = delete
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener?) {
        date = System.currentTimeMillis()
    }

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener?) {
        date = System.currentTimeMillis()
    }

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener?) {
        // Normally we'd like to check every file, but it may take a while.
        if (deleteOnRemoval) deleteFile(kuick, parent)
    }

    fun deleteFile(kuick: KuickDb, parent: Transfer?) {
        if (Type.INCOMING != type || (Flag.INTERRUPTED != flag && Flag.DONE != flag)) return
        val actualParent = parent ?: Transfer(transferId).also {
            kuick.reconstruct(it)
        }

        try {
            val file = Files.getIncomingPseudoFile(kuick.context, this, actualParent, false)
            if (file.isFile()) file.delete()
        } catch (ignored: Exception) {
            // do nothing
        }
    }

    override fun getComparableName(): String = name

    override fun getComparableDate(): Long = date

    override fun getComparableSize(): Long = length

    override fun getSelectableTitle(): String = name

    override fun isSelectableSelected(): Boolean = isSelected

    override fun setSelectableSelected(selected: Boolean): Boolean {
        isSelected = selected
        return true
    }

    enum class Type {
        INCOMING, OUTGOING
    }

    enum class Flag {
        INTERRUPTED, PENDING, REMOVED, IN_PROGRESS, DONE;

        var bytesValue: Long = 0
        override fun toString(): String {
            return if (bytesValue > 0) bytesValue.toString() else super.toString()
        }
    }

    companion object {
        fun from(streamInfo: StreamInfo, transferId: Long): TransferItem {
            return TransferItem(
                AppUtils.uniqueNumber.toLong(), transferId, streamInfo.friendlyName,
                streamInfo.uri.toString(), streamInfo.mimeType, streamInfo.size, Type.OUTGOING
            )
        }

        fun from(file: DocumentFile, transferId: Long, directory: String?): TransferItem {
            val transferItem = TransferItem(
                AppUtils.uniqueNumber.toLong(), transferId, file.getName(),
                file.getUri().toString(), file.getType(), file.getLength(), Type.OUTGOING
            )
            if (directory.isNullOrEmpty()) transferItem.directory = directory
            return transferItem
        }

        @JvmStatic
        fun from(shareable: Shareable, transferId: Long, directory: String?): TransferItem {
            val transferItem = TransferItem(
                AppUtils.uniqueNumber.toLong(), transferId, shareable.fileName,
                shareable.uri.toString(), shareable.mimeType, shareable.getComparableSize(), Type.OUTGOING
            )
            if (directory != null) transferItem.directory = directory
            return transferItem
        }
    }
}