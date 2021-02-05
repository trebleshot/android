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
import android.net.Uri
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.Progress
import com.genonbeta.TrebleShot.dataobject.TransferMember
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.dataobject.DeviceRoute
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.android.framework.``object`

/**
 * created by: Veli
 * date: 19.11.2017 16:50
 */
abstract class Shareable : Editable {
    @JvmField
    override var id: Long = 0
    @JvmField
    var friendlyName: String? = null
    @JvmField
    var fileName: String? = null
    @JvmField
    var mimeType: String? = null
    @JvmField
    var uri: Uri? = null
    override var comparableDate: Long = 0
    override var comparableSize: Long = 0
    var isSelectableSelected = false
        private set

    override fun applyFilter(filteringKeywords: Array<String>): Boolean {
        for (keyword in filteringKeywords) if (TextUtils.searchWord(friendlyName, keyword)) return true
        return false
    }

    override fun comparisonSupported(): Boolean {
        return true
    }

    protected fun initialize(
        id: Long, friendlyName: String?, fileName: String?, mimeType: String?, date: Long, size: Long,
        uri: Uri?
    ) {
        this.id = id
        this.friendlyName = friendlyName
        this.fileName = fileName
        this.mimeType = mimeType
        comparableDate = date
        comparableSize = size
        this.uri = uri
    }

    override val comparableName: String?
        get() = selectableTitle
    val selectableTitle: String
        get() = friendlyName!!

    override fun equals(obj: Any?): Boolean {
        return if (obj is Shareable) obj.uri == uri else super.equals(obj)
    }

    override fun setSelectableSelected(selected: Boolean): Boolean {
        isSelectableSelected = selected
        return true
    }
}