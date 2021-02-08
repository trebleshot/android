package com.genonbeta.android.database

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase

interface BaseDatabaseObject {
    fun getValues(): ContentValues

    fun getWhere(): SQLQuery.Select

    fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues)
}