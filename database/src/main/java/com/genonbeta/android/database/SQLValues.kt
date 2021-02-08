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
            return "`" + name + "` " + type.toString() + " " + (if (nullable) "null" else "not null") + extra ?: ""
        }
    }

    class Table(var name: String, val mayExist: Boolean = false) {
        val columns: MutableMap<String, Column> = HashMap()

        operator fun plusAssign(column: Column) {
            columns[column.name] = column
        }
    }
}