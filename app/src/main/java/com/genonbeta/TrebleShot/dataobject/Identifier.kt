/*
 * Copyright (C) 2020 Veli Tasalı
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

class Identifier : Parcelable {
    var key: String? = null
    var value: String? = null
    var isNull = false

    constructor() {}
    protected constructor(`in`: Parcel) {
        key = `in`.readString()
        value = `in`.readString()
        isNull = `in`.readByte().toInt() != 0
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is Identifier) {
            val other = obj
            return key == other.key && isNull == other.isNull && (isNull || value == other.value)
        }
        return super.equals(obj)
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(key)
        dest.writeString(value)
        dest.writeByte((if (isNull) 1 else 0).toByte())
    }

    companion object {
        val CREATOR: Creator<Identifier> = object : Creator<Identifier?> {
            override fun createFromParcel(`in`: Parcel): Identifier? {
                return Identifier(`in`)
            }

            override fun newArray(size: Int): Array<Identifier?> {
                return arrayOfNulls(size)
            }
        }

        fun from(key: Enum<*>, value: Any?): Identifier {
            return from(key.toString(), value)
        }

        @JvmStatic
        fun from(key: String?, value: Any?): Identifier {
            val identifier = Identifier()
            identifier.key = key
            identifier.isNull = value == null
            identifier.value = if (identifier.isNull) "" else value.toString()
            return identifier
        }
    }
}