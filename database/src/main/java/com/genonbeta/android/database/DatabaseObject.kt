package com.genonbeta.android.database

import android.database.sqlite.SQLiteDatabase

/**
 * created by: Veli
 * date: 2.11.2017 21:31
 */
interface DatabaseObject<T> : BaseDatabaseObject {
    fun onCreateObject(db: SQLiteDatabase, kuick: KuickDb, parent: T?, progress: Progress.Context?)

    fun onUpdateObject(db: SQLiteDatabase, kuick: KuickDb, parent: T?, progress: Progress.Context?)

    fun onRemoveObject(db: SQLiteDatabase, kuick: KuickDb, parent: T?, progress: Progress.Context?)
}