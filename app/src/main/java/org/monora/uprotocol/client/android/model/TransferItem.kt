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
package org.monora.uprotocol.client.android.model

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.collection.ArrayMap
import org.monora.uprotocol.client.android.database.Kuick
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.Files
import org.monora.uprotocol.client.android.util.Transfers
import com.genonbeta.android.database.DatabaseObject
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.util.actionperformer.SelectionModel
import org.json.JSONException
import org.json.JSONObject
import java.security.InvalidParameterException

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */
open class TransferItem : DatabaseObject<Transfer>, SelectionModel {
    lateinit var name: String

    lateinit var file: String

    lateinit var mimeType: String

    open var directory: String? = null

    var id: Long = 0

    var length: Long = 0

    var date: Long = 0

    var transferId: Long = 0

    var type = Type.INCOMING

    // When the type is outgoing, the sender gets to have device id : flag list

    protected val senderFlagListInternal: MutableMap<String, Flag> = ArrayMap()

    // When the type is incoming, the receiver will only have a flag for its status.
    private var receiverFlag = Flag.PENDING

    private var deleteOnRemoval = false

    protected var isSelected = false

    constructor()

    constructor(id: Long, transferId: Long, name: String, file: String, mimeType: String, size: Long, type: Type) {
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

    override fun canSelect(): Boolean = true

    override fun equals(other: Any?): Boolean {
        if (other !is TransferItem) return super.equals(other)
        return other.id == id && type == other.type
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
        synchronized(senderFlagListInternal) { flag = senderFlagListInternal[deviceId] }
        return if (flag == null) Flag.PENDING else flag!!
    }

    val flags: Array<Flag>
        get() {
            synchronized(senderFlagListInternal) {
                return senderFlagListInternal.values.toTypedArray()
            }
        }

    val senderFlagList: MutableMap<String, Flag>
        get() {
            synchronized(senderFlagListInternal) {
                return senderFlagListInternal.toMutableMap()
            }
        }

    fun putFlag(deviceId: String, flag: Flag) {
        if (Type.OUTGOING != type) throw InvalidParameterException()
        synchronized(senderFlagListInternal) { senderFlagListInternal.put(deviceId, flag) }
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
            val item = JSONObject()
            synchronized(senderFlagListInternal) {
                for (deviceId in senderFlagListInternal.keys) try {
                    item.put(deviceId, senderFlagListInternal[deviceId])
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
            values.put(Kuick.FIELD_TRANSFERITEM_FLAG, item.toString())
        }
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, values: ContentValues) {
        name = values.getAsString(Kuick.FIELD_TRANSFERITEM_NAME)
        file = values.getAsString(Kuick.FIELD_TRANSFERITEM_FILE)
        length = values.getAsLong(Kuick.FIELD_TRANSFERITEM_SIZE)
        mimeType = values.getAsString(Kuick.FIELD_TRANSFERITEM_MIME)
        id = values.getAsLong(Kuick.FIELD_TRANSFERITEM_ID)
        transferId = values.getAsLong(Kuick.FIELD_TRANSFERITEM_TRANSFERID)
        type = Type.valueOf(values.getAsString(Kuick.FIELD_TRANSFERITEM_TYPE))
        directory = values.getAsString(Kuick.FIELD_TRANSFERITEM_DIRECTORY)

        // Added with DB version 13
        if (values.containsKey(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME)) date =
            values.getAsLong(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME)
        val flagString = values.getAsString(Kuick.FIELD_TRANSFERITEM_FLAG)
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
                synchronized(senderFlagListInternal) {
                    senderFlagListInternal.clear()
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
                        senderFlagListInternal[key] = flag
                    }
                }
            } catch (ignored: JSONException) {
            }
        }
    }

    fun setDeleteOnRemoval(delete: Boolean) {
        deleteOnRemoval = delete
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, progress: Progress.Context?) {
        date = System.currentTimeMillis()
    }

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, progress: Progress.Context?) {
        date = System.currentTimeMillis()
    }

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, progress: Progress.Context?) {
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

    override fun name(): String = name

    override fun selected(): Boolean = isSelected

    override fun select(selected: Boolean) {
        isSelected = selected
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
    }
}