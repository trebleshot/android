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
import com.genonbeta.android.framework.``object`
import java.lang.Exception

class Device : DatabaseObject<Void?>, Parcelable {
    @JvmField
    var brand: String? = null
    @JvmField
    var model: String? = null
    @JvmField
    var username: String? = null
    @JvmField
    var uid: String? = null
    @JvmField
    var versionName: String? = null
    @JvmField
    var versionCode = 0
    @JvmField
    var protocolVersion = 0
    @JvmField
    var protocolVersionMin = 0
    @JvmField
    var sendKey = 0
    @JvmField
    var receiveKey = 0
    @JvmField
    var lastUsageTime: Long = 0
    @JvmField
    var isTrusted = false
    @JvmField
    var isBlocked = false
    @JvmField
    var isLocal = false
    @JvmField
    var type = Type.NORMAL
    private var mIsSelected = false

    constructor() {}
    constructor(uid: String?) {
        this.uid = uid
    }

    protected constructor(`in`: Parcel) {
        brand = `in`.readString()
        model = `in`.readString()
        username = `in`.readString()
        uid = `in`.readString()
        versionName = `in`.readString()
        versionCode = `in`.readInt()
        protocolVersion = `in`.readInt()
        protocolVersionMin = `in`.readInt()
        sendKey = `in`.readInt()
        receiveKey = `in`.readInt()
        lastUsageTime = `in`.readLong()
        isTrusted = `in`.readByte().toInt() != 0
        isBlocked = `in`.readByte().toInt() != 0
        isLocal = `in`.readByte().toInt() != 0
        mIsSelected = `in`.readByte().toInt() != 0
    }

    private fun checkFields() {
        check(!(Type.NORMAL == type && (sendKey == 0 || receiveKey == 0))) { "Keys for $username cannot be invalid when the device is saved" }
        check(Type.NORMAL_ONLINE != type) { "Online state should not be assigned even when the device is online." }
    }

    override fun equals(obj: Any?): Boolean {
        return if (obj is Device && uid != null) uid == obj.uid else super.equals(
            obj
        )
    }

    override fun hashCode(): Int {
        return ObjectsCompat.hash(uid, type)
    }

    fun generatePictureId(): String {
        return String.format("picture_%s", uid)
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_DEVICES)
            .setWhere(Kuick.FIELD_DEVICES_ID + "=?", uid)
    }

    override fun getValues(): ContentValues {
        if (Type.NORMAL_ONLINE == type) type = Type.NORMAL
        val values = ContentValues()
        values.put(Kuick.FIELD_DEVICES_ID, uid)
        values.put(Kuick.FIELD_DEVICES_USER, username)
        values.put(Kuick.FIELD_DEVICES_BRAND, brand)
        values.put(Kuick.FIELD_DEVICES_MODEL, model)
        values.put(Kuick.FIELD_DEVICES_BUILDNAME, versionName)
        values.put(Kuick.FIELD_DEVICES_BUILDNUMBER, versionCode)
        values.put(Kuick.FIELD_DEVICES_PROTOCOLVERSION, protocolVersion)
        values.put(Kuick.FIELD_DEVICES_PROTOCOLVERSIONMIN, protocolVersionMin)
        values.put(Kuick.FIELD_DEVICES_LASTUSAGETIME, lastUsageTime)
        values.put(Kuick.FIELD_DEVICES_ISRESTRICTED, if (isBlocked) 1 else 0)
        values.put(Kuick.FIELD_DEVICES_ISTRUSTED, if (isTrusted) 1 else 0)
        values.put(Kuick.FIELD_DEVICES_ISLOCALADDRESS, if (isLocal) 1 else 0)
        values.put(Kuick.FIELD_DEVICES_SENDKEY, sendKey)
        values.put(Kuick.FIELD_DEVICES_RECEIVEKEY, receiveKey)
        values.put(Kuick.FIELD_DEVICES_TYPE, type.toString())
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        uid = item.getAsString(Kuick.FIELD_DEVICES_ID)
        username = item.getAsString(Kuick.FIELD_DEVICES_USER)
        brand = item.getAsString(Kuick.FIELD_DEVICES_BRAND)
        model = item.getAsString(Kuick.FIELD_DEVICES_MODEL)
        versionName = item.getAsString(Kuick.FIELD_DEVICES_BUILDNAME)
        versionCode = item.getAsInteger(Kuick.FIELD_DEVICES_BUILDNUMBER)
        lastUsageTime = item.getAsLong(Kuick.FIELD_DEVICES_LASTUSAGETIME)
        isTrusted = item.getAsInteger(Kuick.FIELD_DEVICES_ISTRUSTED) == 1
        isBlocked = item.getAsInteger(Kuick.FIELD_DEVICES_ISRESTRICTED) == 1
        isLocal = item.getAsInteger(Kuick.FIELD_DEVICES_ISLOCALADDRESS) == 1
        sendKey = item.getAsInteger(Kuick.FIELD_DEVICES_SENDKEY)
        receiveKey = item.getAsInteger(Kuick.FIELD_DEVICES_RECEIVEKEY)
        protocolVersion = item.getAsInteger(Kuick.FIELD_DEVICES_PROTOCOLVERSION)
        protocolVersionMin = item.getAsInteger(Kuick.FIELD_DEVICES_PROTOCOLVERSIONMIN)
        try {
            type = Type.valueOf(item.getAsString(Kuick.FIELD_DEVICES_TYPE))
        } catch (e: Exception) {
            type = Type.NORMAL
        }
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Void?, listener: Progress.Listener) {
        checkFields()
    }

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Void?, listener: Progress.Listener) {
        checkFields()
    }

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Void?, listener: Progress.Listener) {
        kuick.context.deleteFile(generatePictureId())
        kuick.remove(
            db, SQLQuery.Select(Kuick.TABLE_DEVICEADDRESS)
                .setWhere(Kuick.FIELD_DEVICEADDRESS_DEVICEID + "=?", uid)
        )
        val members = kuick.castQuery(
            db, SQLQuery.Select(
                Kuick.TABLE_TRANSFERMEMBER
            ).setWhere(
                Kuick.FIELD_TRANSFERMEMBER_DEVICEID
                        + "=?", uid
            ), TransferMember::class.java, null
        )
        for (member in members) kuick.remove(db, member, null, listener)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(brand)
        dest.writeString(model)
        dest.writeString(username)
        dest.writeString(uid)
        dest.writeString(versionName)
        dest.writeInt(versionCode)
        dest.writeInt(protocolVersion)
        dest.writeInt(protocolVersionMin)
        dest.writeInt(sendKey)
        dest.writeInt(receiveKey)
        dest.writeLong(lastUsageTime)
        dest.writeByte((if (isTrusted) 1 else 0).toByte())
        dest.writeByte((if (isBlocked) 1 else 0).toByte())
        dest.writeByte((if (isLocal) 1 else 0).toByte())
        dest.writeByte((if (mIsSelected) 1 else 0).toByte())
    }

    enum class Type {
        NORMAL, NORMAL_ONLINE, WEB
    }

    companion object {
        val CREATOR: Creator<Device> = object : Creator<Device?> {
            override fun createFromParcel(`in`: Parcel): Device? {
                return Device(`in`)
            }

            override fun newArray(size: Int): Array<Device?> {
                return arrayOfNulls(size)
            }
        }
    }
}