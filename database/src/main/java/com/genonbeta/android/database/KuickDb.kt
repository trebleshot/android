package com.genonbeta.android.database

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.util.Log
import com.genonbeta.android.database.exception.ReconstructionFailedException
import java.io.Serializable
import java.util.*

/**
 * Created by: veli
 * Date: 1/31/17 4:51 PM
 */
abstract class KuickDb(
    val context: Context, name: String, factory: CursorFactory?, version: Int,
) : SQLiteOpenHelper(context, name, factory, version) {
    private val broadcastOverhead: MutableList<BroadcastData> = ArrayList()

    fun bindContentValue(statement: SQLiteStatement, iteratorPosition: Int, binding: Any?) {
        binding?.let {
            if (it is Long || it is Int)
                statement.bindLong(iteratorPosition, it as Long)
            else if (it is Double)
                statement.bindDouble(iteratorPosition, it)
            else if (it is ByteArray)
                statement.bindBlob(iteratorPosition, it)
            else
                statement.bindString(iteratorPosition, if (it is String) it else it.toString())
        } ?: statement.bindNull(iteratorPosition)
    }

    fun <T, V : DatabaseObject<T>> castQuery(select: SQLQuery.Select, clazz: Class<V>): MutableList<V> {
        return castQuery(select, clazz, null)
    }

    fun <T, V : DatabaseObject<T>> castQuery(
        select: SQLQuery.Select, clazz: Class<V>,
        listener: CastQueryListener<V>?,
    ): MutableList<V> {
        return castQuery(readableDatabase, select, clazz, listener)
    }

    fun <T, V : DatabaseObject<T>> castQuery(
        db: SQLiteDatabase, select: SQLQuery.Select,
        clazz: Class<V>, listener: CastQueryListener<V>?,
    ): MutableList<V> {
        val returnedList: MutableList<V> = ArrayList()
        val itemList = getTable(db, select)
        try {
            for (item in itemList) {
                val newClazz = clazz.newInstance()
                newClazz.reconstruct(db, this, item)
                listener?.onObjectReconstructed(this, item, newClazz)
                returnedList.add(newClazz)
            }
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InstantiationException) {
            e.printStackTrace()
        }
        return returnedList
    }

    @Synchronized
    fun append(db: SQLiteDatabase, tableName: String, changeType: String) {
        append(db, tableName, changeType, getAffectedRowCount(db))
    }

    @Synchronized
    fun append(db: SQLiteDatabase, tableName: String, changeType: String, affectedRows: Long) {
        // If no row were affected, we shouldn't add the changelog.
        if (affectedRows <= 0) {
            Log.e(TAG, "No changelog to update in: $tableName ; change: $changeType; affected rows: $affectedRows")
            return
        }

        val data = broadcastOverhead.firstOrNull { tableName == it.tableName } ?: run {
            BroadcastData(tableName).also { broadcastOverhead.add(it) }
        }

        when (changeType) {
            TYPE_INSERT -> data.inserted = true
            TYPE_REMOVE -> data.removed = true
            TYPE_UPDATE -> data.updated = true
        }
        data.affectedRowCount += affectedRows.toInt()
    }

    @Synchronized
    fun broadcast() {
        synchronized(broadcastOverhead) {
            for (data in broadcastOverhead)
                context.sendBroadcast(Intent(ACTION_DATABASE_CHANGE).putExtra(EXTRA_BROADCAST_DATA, data))
            broadcastOverhead.clear()
        }
    }

    fun getAffectedRowCount(database: SQLiteDatabase): Long {
        var cursor: Cursor? = null
        var returnCount: Long = 0
        try {
            cursor = database.rawQuery("SELECT changes() AS affected_row_count", null)
            if (cursor != null && cursor.count > 0 && cursor.moveToFirst())
                returnCount = cursor.getLong(cursor.getColumnIndex("affected_row_count"))
        } catch (ignored: SQLException) {
        } finally {
            cursor?.close()
        }
        return returnCount
    }

    fun getFirstFromTable(select: SQLQuery.Select): ContentValues? {
        return getFirstFromTable(readableDatabase, select)
    }

    fun getFirstFromTable(db: SQLiteDatabase, select: SQLQuery.Select): ContentValues? {
        val list = getTable(db, select.setLimit(1))
        return if (list.size > 0) list[0] else null
    }

    fun getTable(select: SQLQuery.Select): MutableList<ContentValues> {
        return getTable(readableDatabase, select)
    }

    fun getTable(db: SQLiteDatabase, select: SQLQuery.Select): MutableList<ContentValues> {
        val list: MutableList<ContentValues> = ArrayList()
        val cursor = db.query(
            select.tableName, select.columns, select.where, select.whereArgs, select.groupBy,
            select.having, select.orderBy, select.limit
        )
        if (cursor.moveToFirst()) {
            select.loadListener?.onOpen(this, cursor)

            val columnCount = cursor.columnCount
            val columns = arrayOfNulls<String?>(columnCount)
            val types = IntArray(columnCount)

            for (i in 0 until columnCount) {
                columns[i] = cursor.getColumnName(i)
                types[i] = cursor.getType(i)
            }

            do {
                val item = ContentValues()
                for (i in 0 until columnCount) {
                    val columnName = columns[i]
                    when (types[i]) {
                        Cursor.FIELD_TYPE_INTEGER -> item.put(columnName, cursor.getLong(i))
                        Cursor.FIELD_TYPE_STRING, Cursor.FIELD_TYPE_NULL -> item.put(columnName, cursor.getString(i))
                        Cursor.FIELD_TYPE_FLOAT -> item.put(columnName, cursor.getFloat(i))
                        Cursor.FIELD_TYPE_BLOB -> item.put(columnName, cursor.getBlob(i))
                    }
                }
                select.loadListener?.onLoad(this, cursor, item)
                list.add(item)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun <T, V : DatabaseObject<T>> insert(`object`: V): Long {
        return insert(writableDatabase, `object`, null, null)
    }

    fun <T, V : DatabaseObject<T>> insert(
        db: SQLiteDatabase, `object`: V, parent: T?, listener: Progress.Listener?,
    ): Long {
        `object`.onCreateObject(db, this, parent, listener)
        return insert(db, `object`.getWhere().tableName, null, `object`.getValues())
    }

    fun insert(db: SQLiteDatabase, tableName: String, nullColumnHack: String?, contentValues: ContentValues): Long {
        val insertedId = db.insert(tableName, nullColumnHack, contentValues)
        append(db, tableName, TYPE_INSERT, if (insertedId > -1) 1 else 0.toLong())
        return insertedId
    }

    fun <T, V : DatabaseObject<T>> insert(objects: MutableList<V>): Boolean {
        return insert(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> insert(
        db: SQLiteDatabase, objects: MutableList<V>, parent: T?, listener: Progress.Listener?,
    ): Boolean {
        db.beginTransaction()
        try {
            Progress.addToTotal(listener, objects.size)
            for (`object` in objects) {
                if (!Progress.call(listener, 1))
                    break
                insert(db, `object`, parent, listener)
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return false
    }

    fun <T, V : DatabaseObject<T>> publish(`object`: V): Int {
        return publish(writableDatabase, `object`, null, null)
    }

    fun <T, V : DatabaseObject<T>> publish(
        database: SQLiteDatabase, `object`: V, parent: T?, listener: Progress.Listener?,
    ): Int {
        var rowsChanged = update(database, `object`, parent, listener)
        if (rowsChanged <= 0)
            rowsChanged = if (insert(database, `object`, parent, listener) >= -1) 1 else 0
        return rowsChanged
    }

    fun <T, V : DatabaseObject<T>> publish(objects: MutableList<V>): Boolean {
        return publish(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> publish(
        db: SQLiteDatabase, objectList: MutableList<V>, parent: T?, listener: Progress.Listener?,
    ): Boolean {
        db.beginTransaction()
        try {
            Progress.addToTotal(listener, objectList.size)
            for (`object` in objectList) {
                if (!Progress.call(listener, 1)) break
                publish(db, `object`, parent, listener)
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return false
    }

    @Throws(ReconstructionFailedException::class)
    fun <T, V : DatabaseObject<T>> reconstruct(`object`: V) {
        reconstruct(readableDatabase, `object`)
    }

    @Throws(ReconstructionFailedException::class)
    fun <T, V : DatabaseObject<T>> reconstruct(db: SQLiteDatabase, `object`: V) {
        val item = getFirstFromTable(db, `object`.getWhere())
        if (item == null) {
            val select = `object`.getWhere()
            val whereArgs = StringBuilder()

            select.whereArgs?.forEach {
                if (whereArgs.isNotEmpty())
                    whereArgs.append(", ")
                whereArgs.append("[] ")
                whereArgs.append(it)
            }

            throw ReconstructionFailedException("No data was returned from: query" + "; tableName: "
                    + select.tableName + "; where: " + select.where + "; whereArgs: " + whereArgs.toString())
        }
        `object`.reconstruct(db, this, item)
    }

    fun <T, V : DatabaseObject<T>> remove(`object`: V) {
        remove(writableDatabase, `object`, null, null)
    }

    fun <T, V : DatabaseObject<T>> remove(
        db: SQLiteDatabase, `object`: V, parent: T?, listener: Progress.Listener?,
    ) {
        `object`.onRemoveObject(db, this, parent, listener)
        remove(db, `object`.getWhere())
    }

    fun remove(select: SQLQuery.Select): Int {
        return remove(writableDatabase, select)
    }

    fun remove(db: SQLiteDatabase, select: SQLQuery.Select): Int {
        val affectedRows = db.delete(select.tableName, select.where, select.whereArgs)
        append(db, select.tableName, TYPE_REMOVE, affectedRows.toLong())
        return affectedRows
    }

    fun <T, V : DatabaseObject<T>> remove(objects: MutableList<V>): Boolean {
        return remove(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> remove(
        db: SQLiteDatabase, objects: MutableList<V>, parent: T?, listener: Progress.Listener?,
    ): Boolean {
        db.beginTransaction()
        try {
            Progress.addToTotal(listener, objects.size)
            for (`object` in objects) {
                if (!Progress.call(listener, 1))
                    break
                remove(db, `object`, parent, listener)
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return false
    }

    fun <T, V : DatabaseObject<T>> removeAsObject(
        db: SQLiteDatabase, select: SQLQuery.Select,
        objectType: Class<V>, parent: T?,
        progressListener: Progress.Listener?,
        queryListener: CastQueryListener<V>?,
    ): Boolean {
        db.beginTransaction()
        try {
            val objects = castQuery(db, select, objectType, queryListener)
            Progress.addToTotal(progressListener, objects.size)
            for (`object` in objects) {
                if (!Progress.call(progressListener, 1))
                    break
                remove(db, `object`, parent, progressListener)
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return false
    }

    fun <T, V : DatabaseObject<T>> update(`object`: V): Int {
        return update(writableDatabase, `object`, null, null)
    }

    fun <T, V : DatabaseObject<T>> update(
        db: SQLiteDatabase, `object`: V, parent: T?, listener: Progress.Listener?,
    ): Int {
        `object`.onUpdateObject(db, this, parent, listener)
        return update(db, `object`.getWhere(), `object`.getValues())
    }

    fun update(select: SQLQuery.Select, values: ContentValues): Int {
        return update(writableDatabase, select, values)
    }

    fun update(database: SQLiteDatabase, select: SQLQuery.Select, values: ContentValues): Int {
        val rowsAffected = database.update(select.tableName, values, select.where, select.whereArgs)
        append(database, select.tableName, TYPE_UPDATE, rowsAffected.toLong())
        return rowsAffected
    }

    fun <T, V : DatabaseObject<T>> update(objects: MutableList<V>): Boolean {
        return update(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> update(
        db: SQLiteDatabase, objects: MutableList<V>, parent: T?, listener: Progress.Listener?,
    ): Boolean {
        db.beginTransaction()
        try {
            Progress.addToTotal(listener, objects.size)
            for (`object` in objects) {
                if (!Progress.call(listener, 1))
                    break
                update(db, `object`, parent, listener)
            }
            db.setTransactionSuccessful()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            db.endTransaction()
        }
        return false
    }

    interface CastQueryListener<T : DatabaseObject<*>> {
        fun onObjectReconstructed(manager: KuickDb, item: ContentValues, `object`: T)
    }

    class BroadcastData internal constructor(var tableName: String) : Serializable {
        var affectedRowCount = 0
        var inserted = false
        var removed = false
        var updated = false
    }

    companion object {
        val TAG = KuickDb::class.java.simpleName
        val ACTION_DATABASE_CHANGE = "com.genonbeta.database.intent.action.DATABASE_CHANGE"
        val EXTRA_BROADCAST_DATA = "extraBroadcastData"
        val TYPE_REMOVE = "typeRemove"
        val TYPE_INSERT = "typeInsert"
        val TYPE_UPDATE = "typeUpdate"
        fun toData(intent: Intent): BroadcastData {
            return intent.getSerializableExtra(EXTRA_BROADCAST_DATA) as BroadcastData
        }
    }
}