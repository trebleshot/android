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
import java.util.ArrayList

/**
 * This class has two modes. One is the AND values and the other is the OR values. The AND values are the first
 * to be tried. If they don't match, which is when they don't have any values or the same value size, or one of the
 * values not matching, the OR values are tried and if any of them matches with something in the list then the match
 * is considered to be true. When using this, try to avoid using default values like 0, false, or "". The values
 * you put should be unique, so that it doesn't match with a random identifier. This class can be transferred with
 * intents.
 */
class Identity : Parcelable {
    private val mValueListOR: MutableList<Identifier>?
    private val mValueListAND: MutableList<Identifier>?

    constructor() {
        mValueListOR = ArrayList()
        mValueListAND = ArrayList()
    }

    protected constructor(`in`: Parcel) {
        mValueListOR = `in`.createTypedArrayList(Identifier.Companion.CREATOR)
        mValueListAND = `in`.createTypedArrayList(Identifier.Companion.CREATOR)
    }

    override fun describeContents(): Int {
        return 0
    }

    override fun equals(obj: Any?): Boolean {
        if (obj is Identity) {
            val identity = obj
            return isANDsTrue(identity) || isORsTrue(identity)
        }
        return super.equals(obj)
    }

    // AND
    @Synchronized
    private fun isANDsTrue(identity: Identity): Boolean {
        if (mValueListAND!!.size <= 0 || identity.mValueListAND!!.size <= 0) return false
        for (identifier in mValueListAND) if (!identity.mValueListAND.contains(identifier)) return false
        return true
    }

    @Synchronized
    private fun isORsTrue(identity: Identity): Boolean {
        for (identifier in mValueListOR!!) if (identity.mValueListOR!!.contains(identifier)) return true
        return false
    }

    fun putAND(identifier: Identifier) {
        synchronized(mValueListAND!!) { mValueListAND.add(identifier) }
    }

    fun putOR(identifier: Identifier) {
        synchronized(mValueListOR!!) { mValueListOR.add(identifier) }
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeTypedList(mValueListOR)
        dest.writeTypedList(mValueListAND)
    }

    companion object {
        val CREATOR: Creator<Identity> = object : Creator<Identity?> {
            override fun createFromParcel(`in`: Parcel): Identity? {
                return Identity(`in`)
            }

            override fun newArray(size: Int): Array<Identity?> {
                return arrayOfNulls(size)
            }
        }

        @JvmStatic
        fun withANDs(vararg ands: Identifier): Identity {
            val identity = Identity()
            for (and in ands) identity.putAND(and)
            return identity
        }

        @JvmStatic
        fun withORs(vararg ors: Identifier): Identity {
            val identity = Identity()
            for (or in ors) identity.putOR(or)
            return identity
        }
    }
}