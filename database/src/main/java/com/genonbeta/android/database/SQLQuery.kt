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
            fun onLoad(db: KuickDb, cursor: Cursor, item: ContentValues)
        }
    }
}