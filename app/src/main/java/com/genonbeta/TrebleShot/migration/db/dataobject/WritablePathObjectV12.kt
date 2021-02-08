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
package com.genonbeta.TrebleShot.migration.db.dataobject

import android.net.Uri

/**
 * created by: Veli
 * date: 16.02.2018 12:56
 */
class WritablePathObjectV12 : DatabaseObject<Any?> {
    var title: String? = null
    var path: Uri? = null

    constructor() {}
    constructor(path: Uri?) {
        this.path = path
    }

    constructor(title: String?, path: Uri?) : this(path) {
        this.title = title
    }

    override fun getWhere(): SQLQuery.Select {
        return SQLQuery.Select(v12.Companion.TABLE_WRITABLEPATH)
            .setWhere(v12.Companion.FIELD_WRITABLEPATH_PATH + "=?", path.toString())
    }

    override fun getValues(): ContentValues {
        val contentValues = ContentValues()
        contentValues.put(v12.Companion.FIELD_WRITABLEPATH_TITLE, title)
        contentValues.put(v12.Companion.FIELD_WRITABLEPATH_PATH, path.toString())
        return contentValues
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        title = item.getAsString(v12.Companion.FIELD_WRITABLEPATH_TITLE)
        path = Uri.parse(item.getAsString(v12.Companion.FIELD_WRITABLEPATH_PATH))
    }

    override fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
    override fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
    override fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: Any, listener: Progress.Listener) {}
}