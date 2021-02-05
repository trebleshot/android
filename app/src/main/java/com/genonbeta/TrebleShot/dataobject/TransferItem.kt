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
import android.util.Log
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.dataobject.DeviceRoute
import com.genonbeta.TrebleShot.util.FileUtils
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import org.json.JSONException
import org.json.JSONObject
import java.lang.Exception
import java.lang.NumberFormatException
import java.security.InvalidParameterException

/**
 * Created by: veli
 * Date: 4/24/17 11:50 PM
 */
open class TransferItem : DatabaseObject<Transfer?>, Editable {
    @JvmField
    var name: String? = null
    @JvmField
    var file: String? = null
    @JvmField
    var mimeType: String? = null
    @JvmField
    var directory: String? = null
    @JvmField
    override var id: Long = 0
    @JvmField
    var transferId: Long = 0
    override var comparableSize: Long = 0
    override var comparableDate: Long = 0
    @JvmField
    var type = Type.INCOMING

    // When the type is outgoing, the sender gets to have device id : flag list
    @JvmField
    protected val mSenderFlagList: MutableMap<String?, Flag> = ArrayMap()

    // When the type is incoming, the receiver will only have a flag for its status.
    private var mReceiverFlag = Flag.PENDING
    private var mDeleteOnRemoval = false
    var isSelectableSelected = false
        private set

    constructor() {}
    constructor(id: Long, transferId: Long, name: String?, file: String?, mimeType: String?, size: Long, type: Type) {
        this.id = id
        this.transferId = transferId
        this.name = name
        this.file = file
        this.mimeType = mimeType
        comparableSize = size
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
            return mReceiverFlag
        }
        set(flag) {
            if (Type.INCOMING != type) throw InvalidParameterException()
            mReceiverFlag = flag
        }

    fun getFlag(deviceId: String?): Flag {
        if (Type.OUTGOING != type) throw InvalidParameterException()
        var flag: Flag?
        synchronized(mSenderFlagList) { flag = mSenderFlagList[deviceId] }
        return if (flag == null) Flag.PENDING else flag!!
    }

    val flags: Array<Flag?>
        get() {
            synchronized(mSenderFlagList) {
                val flags = arrayOfNulls<Flag>(mSenderFlagList.size)
                mSenderFlagList.values.toArray(flags)
                return flags
            }
        }
    val senderFlagList: Map<String?, Flag>
        get() {
            synchronized(mSenderFlagList) {
                val map: MutableMap<String?, Flag> = ArrayMap()
                map.putAll(mSenderFlagList)
                return map
            }
        }

    override fun setId(id: Long) {
        this.id = id
    }

    fun putFlag(deviceId: String?, flag: Flag) {
        if (Type.OUTGOING != type) throw InvalidParameterException()
        synchronized(mSenderFlagList) { mSenderFlagList.put(deviceId, flag) }
    }

    fun getPercentage(members: Array<LoadedMember>, deviceId: String?): Double {
        if (members.size == 0) return 0
        if (Type.INCOMING == type) return Transfers.getPercentageByFlag(
            flag, comparableSize
        ) else if (deviceId != null) return Transfers.getPercentageByFlag(getFlag(deviceId), comparableSize)
        var percentageIndex = 0.0
        var senderMembers = 0
        for (member in members) {
            if (Type.OUTGOING != member.type) continue
            senderMembers++
            percentageIndex += Transfers.getPercentageByFlag(getFlag(member.deviceId), comparableSize)
        }
        return if (percentageIndex > 0) percentageIndex / senderMembers else 0
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
        values.put(Kuick.FIELD_TRANSFERITEM_SIZE, comparableSize)
        values.put(Kuick.FIELD_TRANSFERITEM_MIME, mimeType)
        values.put(Kuick.FIELD_TRANSFERITEM_TYPE, type.toString())
        values.put(Kuick.FIELD_TRANSFERITEM_FILE, file)
        values.put(Kuick.FIELD_TRANSFERITEM_DIRECTORY, directory)
        values.put(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME, comparableDate)
        if (Type.INCOMING == type) {
            values.put(Kuick.FIELD_TRANSFERITEM_FLAG, mReceiverFlag.toString())
        } else {
            val `object` = JSONObject()
            synchronized(mSenderFlagList) {
                for (deviceId in mSenderFlagList.keys) try {
                    `object`.put(deviceId, mSenderFlagList[deviceId])
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
        comparableSize = item.getAsLong(Kuick.FIELD_TRANSFERITEM_SIZE)
        mimeType = item.getAsString(Kuick.FIELD_TRANSFERITEM_MIME)
        id = item.getAsLong(Kuick.FIELD_TRANSFERITEM_ID)
        transferId = item.getAsLong(Kuick.FIELD_TRANSFERITEM_TRANSFERID)
        type = Type.valueOf(item.getAsString(Kuick.FIELD_TRANSFERITEM_TYPE))
        directory = item.getAsString(Kuick.FIELD_TRANSFERITEM_DIRECTORY)

        // Added with DB version 13
        if (item.containsKey(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME)) comparableDate =
            item.getAsLong(Kuick.FIELD_TRANSFERITEM_LASTCHANGETIME)
        val flagString = item.getAsString(Kuick.FIELD_TRANSFERITEM_FLAG)
        if (Type.INCOMING == type) {
            try {
                mReceiverFlag = Flag.valueOf(flagString)
            } catch (e: Exception) {
                try {
                    mReceiverFlag = Flag.IN_PROGRESS
                    mReceiverFlag.bytesValue = flagString.toLong()
                } catch (e1: NumberFormatException) {
                    mReceiverFlag = Flag.PENDING
                }
            }
        } else {
            try {
                val jsonObject = JSONObject(flagString)
                val iterator = jsonObject.keys()
                synchronized(mSenderFlagList) {
                    mSenderFlagList.clear()
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
                        mSenderFlagList[key] = flag
                    }
                }
            } catch (ignored: JSONException) {
            }
        }
    }

    fun setDeleteOnRemoval(delete: Boolean) {
        mDeleteOnRemoval = delete
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener) {
        comparableDate = System.currentTimeMillis()
    }

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener) {
        comparableDate = System.currentTimeMillis()
    }

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Transfer?, listener: Progress.Listener) {
        // Normally we'd like to check every file, but it may take a while.
        if (mDeleteOnRemoval) deleteFile(kuick, parent)
    }

    fun deleteFile(kuick: KuickDb, parent: Transfer?) {
        var parent = parent
        if (Type.INCOMING != type || (Flag.INTERRUPTED != flag
                    && Flag.DONE != flag)
        ) return
        try {
            if (parent == null) {
                Log.d(TransferItem::class.java.simpleName, "onRemoveObject: Had to recreate the group")
                parent = Transfer(transferId)
                kuick.reconstruct<Device, Transfer>(parent)
            }
            val file = FileUtils.getIncomingPseudoFile(
                kuick.context, this, parent,
                false
            )
            if (file != null && file.isFile) file.delete()
        } catch (ignored: Exception) {
            // do nothing
        }
    }

    override val comparableName: String?
        get() = selectableTitle

    @SuppressLint("DefaultLocale")
    override fun getId(): Long {
        return String.format("%d_%d", id, type.ordinal).hashCode().toLong()
    }

    val selectableTitle: String
        get() = name!!

    override fun setSelectableSelected(selected: Boolean): Boolean {
        isSelectableSelected = selected
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
                AppUtils.getUniqueNumber(), transferId, streamInfo.friendlyName,
                streamInfo.uri.toString(), streamInfo.mimeType, streamInfo.size, Type.OUTGOING
            )
        }

        fun from(file: DocumentFile, transferId: Long, directory: String?): TransferItem {
            val `object` = TransferItem(
                AppUtils.getUniqueNumber(), transferId, file.name,
                file.uri.toString(), file.type, file.length(), Type.OUTGOING
            )
            if (directory != null) `object`.directory = directory
            return `object`
        }

        @JvmStatic
        fun from(shareable: Shareable, transferId: Long, directory: String?): TransferItem {
            val `object` = TransferItem(
                AppUtils.getUniqueNumber(), transferId, shareable.fileName,
                shareable.uri.toString(), shareable.mimeType, shareable.size, Type.OUTGOING
            )
            if (directory != null) `object`.directory = directory
            return `object`
        }
    }
}