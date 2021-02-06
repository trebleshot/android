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
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.android.framework.`object`

/**
 * created by: Veli
 * date: 30.12.2017 13:19
 */
class TextStreamObject(var text: String?) : GroupEditableListAdapter.GroupShareable, DatabaseObject<Any?> {

    constructor() : this(null) {}
    constructor(representativeText: String?) : super(GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE, representativeText) {}

    constructor(id: Long) {
        setId(id)
    }

    constructor(id: Long, index: String) {
        initialize(id, index, index, "text/plain", System.currentTimeMillis(), index.length.toLong(), null)
        text = index
    }

    override fun applyFilter(filteringKeywords: Array<String>): Boolean {
        if (super.applyFilter(filteringKeywords)) return true
        for (keyword in filteringKeywords)
            if (text.toLowerCase().contains(keyword.toLowerCase()))
                return true
        return false
    }

    override fun equals(obj: Any?): Boolean {
        return obj is TextStreamObject && obj.id == id
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(Kuick.TABLE_CLIPBOARD)
            .setWhere(Kuick.FIELD_CLIPBOARD_ID + "=?", getId().toString())
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.FIELD_CLIPBOARD_ID, id)
        values.put(Kuick.FIELD_CLIPBOARD_TIME, date)
        values.put(Kuick.FIELD_CLIPBOARD_TEXT, text)
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        this.id = item.getAsLong(Kuick.FIELD_CLIPBOARD_ID)
        text = item.getAsString(Kuick.FIELD_CLIPBOARD_TEXT)
        this.date = item.getAsLong(Kuick.FIELD_CLIPBOARD_TIME)
        this.mimeType = "text/plain"
        this.size = text.length.toLong()
        this.friendlyName = text
        this.fileName = text
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any?, listener: Progress.Listener) {}
    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any?, listener: Progress.Listener) {}
    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any?, listener: Progress.Listener) {}
}