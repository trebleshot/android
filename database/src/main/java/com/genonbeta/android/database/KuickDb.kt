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

    fun <T, V : DatabaseObject<T>> castQuery(select: SQLQuery.Select, clazz: Class<V>): List<V> {
        return castQuery(select, clazz, null)
    }

    fun <T, V : DatabaseObject<T>> castQuery(
        select: SQLQuery.Select, clazz: Class<V>,
        listener: CastQueryListener<V>?,
    ): List<V> {
        return castQuery(readableDatabase, select, clazz, listener)
    }

    fun <T, V : DatabaseObject<T>> castQuery(
        db: SQLiteDatabase, select: SQLQuery.Select,
        clazz: Class<V>, listener: CastQueryListener<V>?,
    ): List<V> {
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
            Log.e(TAG, "No changelog to update on='$tableName' change='$changeType' affected rows=$affectedRows")
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

    fun getTable(select: SQLQuery.Select): List<ContentValues> {
        return getTable(readableDatabase, select)
    }

    fun getTable(db: SQLiteDatabase, select: SQLQuery.Select): List<ContentValues> {
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

    fun <T, V : DatabaseObject<T>> insert(item: V): Long {
        return insert(writableDatabase, item, null, null)
    }

    fun <T, V : DatabaseObject<T>> insert(
        db: SQLiteDatabase, item: V, parent: T?, progress: Progress.Context?,
    ): Long {
        item.onCreateObject(db, this, parent, progress)
        return insert(db, item.getWhere().tableName, null, item.getValues())
    }

    fun insert(db: SQLiteDatabase, tableName: String, nullColumnHack: String?, contentValues: ContentValues): Long {
        val insertedId = db.insert(tableName, nullColumnHack, contentValues)
        append(db, tableName, TYPE_INSERT, if (insertedId > -1) 1 else 0.toLong())
        return insertedId
    }

    fun <T, V : DatabaseObject<T>> insert(objects: List<V>): Boolean {
        return insert(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> insert(
        db: SQLiteDatabase, objects: List<V>, parent: T?, progress: Progress.Context?,
    ): Boolean {
        db.beginTransaction()
        try {
            progress?.increaseTotalBy(objects.size)
            for (item in objects) {
                if (progress != null && !progress.increaseBy(1))
                    break
                insert(db, item, parent, progress)
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

    fun <T, V : DatabaseObject<T>> publish(item: V): Int {
        return publish(writableDatabase, item, null, null)
    }

    fun <T, V : DatabaseObject<T>> publish(
        database: SQLiteDatabase, item: V, parent: T?, progress: Progress.Context?,
    ): Int {
        var rowsChanged = update(database, item, parent, progress)
        if (rowsChanged <= 0)
            rowsChanged = if (insert(database, item, parent, progress) >= -1) 1 else 0
        return rowsChanged
    }

    fun <T, V : DatabaseObject<T>> publish(objects: List<V>): Boolean {
        return publish(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> publish(
        db: SQLiteDatabase, objectList: List<V>, parent: T?, progress: Progress.Context?,
    ): Boolean {
        db.beginTransaction()
        try {
            progress?.increaseTotalBy(objectList.size)
            for (item in objectList) {
                if (progress != null && progress.increaseBy(1)) break
                publish(db, item, parent, progress)
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
    fun <T, V : DatabaseObject<T>> reconstruct(item: V) {
        reconstruct(readableDatabase, item)
    }

    @Throws(ReconstructionFailedException::class)
    fun <T, V : DatabaseObject<T>> reconstruct(db: SQLiteDatabase, item: V) {
        val values = getFirstFromTable(db, item.getWhere())
        if (values == null) {
            val select = item.getWhere()
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
        item.reconstruct(db, this, values)
    }

    fun <T, V : DatabaseObject<T>> remove(item: V) {
        remove(writableDatabase, item, null, null)
    }

    fun <T, V : DatabaseObject<T>> remove(
        db: SQLiteDatabase, item: V, parent: T?, progress: Progress.Context?,
    ) {
        item.onRemoveObject(db, this, parent, progress)
        remove(db, item.getWhere())
    }

    fun remove(select: SQLQuery.Select): Int {
        return remove(writableDatabase, select)
    }

    fun remove(db: SQLiteDatabase, select: SQLQuery.Select): Int {
        val affectedRows = db.delete(select.tableName, select.where, select.whereArgs)
        append(db, select.tableName, TYPE_REMOVE, affectedRows.toLong())
        return affectedRows
    }

    fun <T, V : DatabaseObject<T>> remove(objects: List<V>): Boolean {
        return remove(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> remove(
        db: SQLiteDatabase, objects: List<V>, parent: T?, progress: Progress.Context?,
    ): Boolean {
        db.beginTransaction()
        try {
            progress?.increaseTotalBy(objects.size)
            for (item in objects) {
                if (progress != null && !progress.increaseBy(1))
                    break
                remove(db, item, parent, progress)
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
        progress: Progress.Context?,
        queryListener: CastQueryListener<V>?,
    ): Boolean {
        db.beginTransaction()
        try {
            val objects = castQuery(db, select, objectType, queryListener)
            progress?.increaseTotalBy(objects.size)
            for (item in objects) {
                if (progress != null && !progress.increaseBy(1)) break
                remove(db, item, parent, progress)
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

    fun <T, V : DatabaseObject<T>> update(item: V): Int {
        return update(writableDatabase, item, null, null)
    }

    fun <T, V : DatabaseObject<T>> update(
        db: SQLiteDatabase, item: V, parent: T?, progress: Progress.Context?,
    ): Int {
        item.onUpdateObject(db, this, parent, progress)
        return update(db, item.getWhere(), item.getValues())
    }

    fun update(select: SQLQuery.Select, values: ContentValues): Int {
        return update(writableDatabase, select, values)
    }

    fun update(database: SQLiteDatabase, select: SQLQuery.Select, values: ContentValues): Int {
        val rowsAffected = database.update(select.tableName, values, select.where, select.whereArgs)
        append(database, select.tableName, TYPE_UPDATE, rowsAffected.toLong())
        return rowsAffected
    }

    fun <T, V : DatabaseObject<T>> update(objects: List<V>): Boolean {
        return update(writableDatabase, objects, null, null)
    }

    fun <T, V : DatabaseObject<T>> update(
        db: SQLiteDatabase, objects: List<V>, parent: T?, progress: Progress.Context?,
    ): Boolean {
        db.beginTransaction()
        try {
            progress?.increaseTotalBy(objects.size)
            for (item in objects) {
                if (progress?.increaseBy(1) == false)
                    break
                update(db, item, parent, progress)
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
        fun onObjectReconstructed(manager: KuickDb, values: ContentValues, item: T)
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