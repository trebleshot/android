package com.genonbeta.android.database

import kotlin.Throws
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteOpenHelper
import android.database.sqlite.SQLiteStatement
import android.database.sqlite.SQLiteDatabase
import android.content.ContentValues
import kotlin.jvm.Synchronized
import android.content.Intent
import org.junit.Assert
import org.junit.Test
import java.lang.Exception

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see [Testing documentation](http://d.android.com/tools/testing)
 */
class ExampleUnitTest {
    @Test
    @Throws(Exception::class)
    fun addition_isCorrect() {
        Assert.assertEquals(4, (2 + 2).toLong())
    }
}