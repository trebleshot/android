/*
 * Copyright (C) 2019 Veli Tasalı
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
package org.monora.uprotocol.client.android.database

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.android.database.*
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */
class Kuick(context: Context) : KuickDb(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {

    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, current: Int) {

    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(activity: Activity, item: V, parent: T?) {
        removeAsynchronous(App.from(activity), item, parent)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(app: App, item: V, parent: T?) {
        app.run(SingleRemovalTask(app.applicationContext, writableDatabase, item, parent))
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(activity: Activity, objects: List<V>, parent: T?) {
        removeAsynchronous(App.from(activity), objects, parent)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(app: App, objects: List<V>, parent: T?) {
        app.run(MultipleRemovalTask(app.applicationContext, writableDatabase, objects, parent))
    }

    private abstract class BgTaskImpl(context: Context, titleRes: Int, val db: SQLiteDatabase) : AsyncTask() {
        private val title = context.getString(titleRes)

        override fun onProgressChange(progress: Progress) {
            super.onProgressChange(progress)
            ongoingContent = context.getString(R.string.text_transferStatusFiles, progress.progress, progress.total)
        }

        override fun getName(context: Context): String {
            return title
        }
    }

    private class SingleRemovalTask<T, V : DatabaseObject<T>>(
        context: Context,
        db: SQLiteDatabase,
        private val targetObject: V,
        private val parent: T?,
    ) : BgTaskImpl(context, R.string.mesg_removing, db) {
        override fun onRun() {
            // TODO: 2/25/21 Remove Kuick altogether
            //kuick.remove(db, targetObject, parent, progress)
            //kuick.broadcast()
        }
    }

    private class MultipleRemovalTask<T, V : DatabaseObject<T>>(
        context: Context,
        db: SQLiteDatabase,
        private val targetObjectList: List<V>,
        private val parent: T?,
    ) : BgTaskImpl(context, R.string.mesg_removing, db) {
        override fun onRun() {
            // TODO: 2/25/21 Remove Kuick altogether
            //kuick.remove(db, targetObjectList, parent, progress)
            //kuick.broadcast()
        }
    }

    companion object {
        const val DATABASE_VERSION = 13
        val TAG = Kuick::class.java.simpleName
        val DATABASE_NAME = Kuick::class.java.simpleName + ".db"
    }
}