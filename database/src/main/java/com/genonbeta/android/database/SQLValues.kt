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

import java.util.*

/**
 * created by: Veli
 * date: 10.10.2017 17:31
 */
class SQLValues {
    val tables: MutableMap<String, Table> = HashMap()

    fun defineTable(name: String, mayExist: Boolean = false) = Table(name, mayExist).also { tables[name] = it }

    class Column(
        var name: String,
        var value: String? = null,
        var extra: String? = null,
        var type: SQLType? = null,
        var nullable: Boolean = false,
    ) {
        override fun toString(): String {
            return "`" + name + "` " + type.toString() + " " + (if (nullable) "null" else "not null") + (extra ?: "")
        }
    }

    class Table(var name: String, val mayExist: Boolean = false) {
        val columns: MutableMap<String, Column> = HashMap()

        operator fun plusAssign(column: Column) {
            columns[column.name] = column
        }
    }
}