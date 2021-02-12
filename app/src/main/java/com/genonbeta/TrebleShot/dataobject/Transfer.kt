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

/**
 * created by: veli
 * date: 06.04.2018 09:37
 */
class Transfer : DatabaseObject<Device?>, Parcelable {
    @JvmField
    var id: Long = 0

    @JvmField
    var dateCreated: Long = 0

    @JvmField
    var savePath: String? = null
    var isPaused = false

    @JvmField
    var isServedOnWeb = false

    @JvmField
    var deleteFilesOnRemoval = false

    constructor() {}
    constructor(id: Long) {
        this.id = id
    }

    protected constructor(`in`: Parcel) {
        id = `in`.readLong()
        dateCreated = `in`.readLong()
        savePath = `in`.readString()
        isPaused = `in`.readByte().toInt() != 0
        isServedOnWeb = `in`.readByte().toInt() != 0
        deleteFilesOnRemoval = `in`.readByte().toInt() != 0
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(other: Any?): Boolean {
        return other is Transfer && other.id == id
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.FIELD_TRANSFER_ID, id)
        values.put(Kuick.FIELD_TRANSFER_SAVEPATH, savePath)
        values.put(Kuick.FIELD_TRANSFER_DATECREATED, dateCreated)
        values.put(Kuick.FIELD_TRANSFER_ISSHAREDONWEB, if (isServedOnWeb) 1 else 0)
        values.put(Kuick.FIELD_TRANSFER_ISPAUSED, if (isPaused) 1 else 0)
        return values
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_TRANSFER)
            .setWhere(Kuick.FIELD_TRANSFER_ID + "=?", id.toString())
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, values: ContentValues) {
        id = values.getAsLong(Kuick.FIELD_TRANSFER_ID)
        savePath = values.getAsString(Kuick.FIELD_TRANSFER_SAVEPATH)
        dateCreated = values.getAsLong(Kuick.FIELD_TRANSFER_DATECREATED)
        isServedOnWeb = values.getAsInteger(Kuick.FIELD_TRANSFER_ISSHAREDONWEB) == 1
        isPaused = values.getAsInteger(Kuick.FIELD_TRANSFER_ISPAUSED) == 1
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeLong(id)
        dest.writeLong(dateCreated)
        dest.writeString(savePath)
        dest.writeByte((if (isPaused) 1 else 0).toByte())
        dest.writeByte((if (isServedOnWeb) 1 else 0).toByte())
        dest.writeByte((if (deleteFilesOnRemoval) 1 else 0).toByte())
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {
        dateCreated = System.currentTimeMillis()
    }

    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {}

    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Device?, progress: Progress.Context?) {
        val objectSelection = SQLQuery.Select(Kuick.TABLE_TRANSFERITEM)
            .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFERITEM_TRANSFERID), id.toString())
        kuick.remove(db,
            SQLQuery.Select(Kuick.TABLE_TRANSFERMEMBER)
                .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFERMEMBER_TRANSFERID), id.toString())
        )
        if (deleteFilesOnRemoval) {
            val itemList = kuick.castQuery(db, objectSelection, TransferItem::class.java, null)
            progress?.increaseTotalBy(itemList.size)
            for (item in itemList) {
                if (progress != null && !progress.increaseBy(1)) break
                item.deleteFile(kuick, this)
            }
        }
        kuick.remove(db, objectSelection)
    }

    companion object CREATOR : Creator<Transfer> {
        override fun createFromParcel(parcel: Parcel): Transfer {
            return Transfer(parcel)
        }

        override fun newArray(size: Int): Array<Transfer?> {
            return arrayOfNulls(size)
        }
    }
}