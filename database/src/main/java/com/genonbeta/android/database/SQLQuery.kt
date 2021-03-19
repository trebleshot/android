/*
 * Copyright (C) 2021 Veli Tasalı
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

package com.genonbeta.android.database

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**
 * Created by: veli
 * Date: 12/1/16 2:41 PM
 */
object SQLQuery {
    fun createTable(db: SQLiteDatabase, table: SQLValues.Table) {
        val stringBuilder = StringBuilder()
        stringBuilder.append("CREATE TABLE ")

        if (table.mayExist)
            stringBuilder.append("IF NOT EXISTS ")

        stringBuilder.append("`")
        stringBuilder.append(table.name)
        stringBuilder.append("` (")

        for ((count, columnString) in table.columns.values.withIndex()) {
            if (count > 0) stringBuilder.append(", ")
            stringBuilder.append(columnString.toString())
        }

        stringBuilder.append(")")
        db.execSQL(stringBuilder.toString())
    }

    fun createTables(db: SQLiteDatabase, values: SQLValues) {
        for (table in values.tables.values)
            createTable(db, table)
    }

    class Select(var tableName: String, vararg var columns: String?) {
        val items: ContentValues = ContentValues()
        var tag: String? = null
        var where: String? = null
        var whereArgs: Array<out String>? = null
        var groupBy: String? = null
        var having: String? = null
        var orderBy: String? = null
        var limit: String? = null
        var loadListener: LoadListener? = null

        fun setHaving(having: String?): Select {
            this.having = having
            return this
        }

        fun setOrderBy(orderBy: String?): Select {
            this.orderBy = orderBy
            return this
        }

        fun setGroupBy(groupBy: String?): Select {
            this.groupBy = groupBy
            return this
        }

        fun setLimit(limit: Int): Select {
            return setLimit(limit.toString())
        }

        fun setLimit(limit: String?): Select {
            this.limit = limit.toString()
            return this
        }

        fun setLoadListener(listener: LoadListener?): Select {
            loadListener = listener
            return this
        }

        fun setTag(tag: String?): Select {
            this.tag = tag
            return this
        }

        fun setWhere(where: String?, vararg args: String): Select {
            this.where = where
            this.whereArgs = args
            return this
        }

        interface LoadListener {
            fun onOpen(db: KuickDb, cursor: Cursor)
            fun onLoad(db: KuickDb, cursor: Cursor, values: ContentValues)
        }
    }
}