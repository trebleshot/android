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
package com.genonbeta.TrebleShot.database

import android.app.Activity
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.migration.db.Migration
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.database.*

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */
class Kuick(context: Context?) : KuickDb(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        SQLQuery.createTables(db, tables())
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, current: Int) {
        Migration.migrate(this, db, old, current)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(activity: Activity, `object`: V, parent: T) {
        removeAsynchronous(App.from(activity), `object`, parent)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(app: App, `object`: V, parent: T) {
        app.run(SingleRemovalTask(app.applicationContext, writableDatabase, `object`, parent))
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(activity: Activity?, objects: List<V>, parent: T) {
        removeAsynchronous(App.from(activity), objects, parent)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(app: App, objects: List<V>, parent: T) {
        app.run(MultipleRemovalTask(app.applicationContext, writableDatabase, objects, parent))
    }

    private abstract class BgTaskImpl(context: Context, titleRes: Int, db: SQLiteDatabase) : AsyncTask() {
        private val mTitle = context.getString(titleRes)

        override fun onProgressChange(progress: Progress) : onProg {
            super.onProgressChange(progress)
            ongoingContent = context.getString(
                R.string.text_transferStatusFiles, progress.getCurrent(),
                progress.getTotal()
            )
        }

        fun getDb(): SQLiteDatabase {
            return mDb
        }

        override fun getName(context: Context?): String? {
            return mTitle
        }

        init {
            mDb = db
        }
    }

    private class SingleRemovalTask<T, V : DatabaseObject<T>>(
        context: Context,
        db: SQLiteDatabase,
        private val mObject: V,
        private val mParent: T
    ) : BgTaskImpl(context, R.string.mesg_removing, db) {
        override fun onRun() {
            val kuick = AppUtils.getKuick(context)
            kuick.remove(getDb(), mObject, mParent, progressListener())
            kuick.broadcast()
        }
    }

    private class MultipleRemovalTask<T, V : DatabaseObject<T>>(
        context: Context,
        db: SQLiteDatabase,
        private val mObjectList: List<V>?,
        private val mParent: T
    ) : BgTaskImpl(context, R.string.mesg_removing, db) {
        override fun onRun() {
            val kuick = AppUtils.getKuick(context)
            kuick.remove(getDb(), mObjectList, mParent, progressListener())
            kuick.broadcast()
        }
    }

    companion object {
        const val DATABASE_VERSION = 13
        val TAG = Kuick::class.java.simpleName
        val DATABASE_NAME = Kuick::class.java.simpleName + ".db"
        const val TABLE_CLIPBOARD = "clipboard"
        const val FIELD_CLIPBOARD_ID = "id"
        const val FIELD_CLIPBOARD_TEXT = "text"
        const val FIELD_CLIPBOARD_TIME = "time"
        const val TABLE_DEVICES = "devices"
        const val FIELD_DEVICES_ID = "deviceId"
        const val FIELD_DEVICES_USER = "user"
        const val FIELD_DEVICES_BRAND = "brand"
        const val FIELD_DEVICES_MODEL = "model"
        const val FIELD_DEVICES_BUILDNAME = "buildName"
        const val FIELD_DEVICES_BUILDNUMBER = "buildNumber"
        const val FIELD_DEVICES_PROTOCOLVERSION = "clientVersion"
        const val FIELD_DEVICES_PROTOCOLVERSIONMIN = "protocolVersionMin"
        const val FIELD_DEVICES_LASTUSAGETIME = "lastUsedTime"
        const val FIELD_DEVICES_ISRESTRICTED = "isRestricted"
        const val FIELD_DEVICES_ISTRUSTED = "isTrusted"
        const val FIELD_DEVICES_ISLOCALADDRESS = "isLocalAddress"
        const val FIELD_DEVICES_SENDKEY = "sendKey"
        const val FIELD_DEVICES_RECEIVEKEY = "receiveKey"
        const val FIELD_DEVICES_TYPE = "type"
        const val TABLE_DEVICEADDRESS = "deviceAddress"
        const val FIELD_DEVICEADDRESS_IPADDRESSTEXT = "ipAddressText"
        const val FIELD_DEVICEADDRESS_IPADDRESS = "ipAddress"
        const val FIELD_DEVICEADDRESS_DEVICEID = "deviceId"
        const val FIELD_DEVICEADDRESS_LASTCHECKEDDATE = "lastCheckedDate"
        const val TABLE_FILEBOOKMARK = "fileBookmark"
        const val FIELD_FILEBOOKMARK_TITLE = "title"
        const val FIELD_FILEBOOKMARK_PATH = "path"
        const val TABLE_TRANSFERMEMBER = "transferMember"
        const val FIELD_TRANSFERMEMBER_TRANSFERID = "transferId"
        const val FIELD_TRANSFERMEMBER_DEVICEID = "deviceId"
        const val FIELD_TRANSFERMEMBER_TYPE = "type"
        const val TABLE_TRANSFERITEM = "transferItem"
        const val FIELD_TRANSFERITEM_ID = "id"
        const val FIELD_TRANSFERITEM_NAME = "name"
        const val FIELD_TRANSFERITEM_SIZE = "size"
        const val FIELD_TRANSFERITEM_MIME = "mime"
        const val FIELD_TRANSFERITEM_TYPE = "type"
        const val FIELD_TRANSFERITEM_TRANSFERID = "groupId"
        const val FIELD_TRANSFERITEM_FILE = "file"
        const val FIELD_TRANSFERITEM_DIRECTORY = "directory"
        const val FIELD_TRANSFERITEM_LASTCHANGETIME = "lastChangeTime"
        const val FIELD_TRANSFERITEM_FLAG = "flag"
        const val TABLE_TRANSFER = "transfer"
        const val FIELD_TRANSFER_ID = "id"
        const val FIELD_TRANSFER_SAVEPATH = "savePath"
        const val FIELD_TRANSFER_DATECREATED = "dateCreated"
        const val FIELD_TRANSFER_ISSHAREDONWEB = "isSharedOnWeb"
        const val FIELD_TRANSFER_ISPAUSED = "isPaused"
        fun tables(): SQLValues {
            val values = SQLValues()
            values.defineTable(TABLE_CLIPBOARD)
                .define(SQLValues.Column(FIELD_CLIPBOARD_ID, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_CLIPBOARD_TEXT, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_CLIPBOARD_TIME, SQLType.Long, false))
            values.defineTable(TABLE_DEVICES)
                .define(SQLValues.Column(FIELD_DEVICES_ID, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_DEVICES_USER, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_DEVICES_BRAND, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_DEVICES_MODEL, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_DEVICES_BUILDNAME, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_DEVICES_BUILDNUMBER, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_DEVICES_PROTOCOLVERSION, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_DEVICES_PROTOCOLVERSIONMIN, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_DEVICES_LASTUSAGETIME, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_DEVICES_ISRESTRICTED, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_DEVICES_ISTRUSTED, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_DEVICES_ISLOCALADDRESS, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_DEVICES_SENDKEY, SQLType.Integer, true))
                .define(SQLValues.Column(FIELD_DEVICES_RECEIVEKEY, SQLType.Integer, true))
                .define(SQLValues.Column(FIELD_DEVICES_TYPE, SQLType.Text, false))
            values.defineTable(TABLE_DEVICEADDRESS)
                .define(SQLValues.Column(FIELD_DEVICEADDRESS_IPADDRESS, SQLType.Blob, false))
                .define(SQLValues.Column(FIELD_DEVICEADDRESS_IPADDRESSTEXT, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_DEVICEADDRESS_DEVICEID, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_DEVICEADDRESS_LASTCHECKEDDATE, SQLType.Integer, false))
            values.defineTable(TABLE_FILEBOOKMARK)
                .define(SQLValues.Column(FIELD_FILEBOOKMARK_TITLE, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_FILEBOOKMARK_PATH, SQLType.Text, false))
            values.defineTable(TABLE_TRANSFERITEM)
                .define(SQLValues.Column(FIELD_TRANSFERITEM_ID, SQLType.Long, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_TRANSFERID, SQLType.Long, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_DIRECTORY, SQLType.Text, true))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_FILE, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_NAME, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_SIZE, SQLType.Integer, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_MIME, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_TYPE, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_FLAG, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_TRANSFERITEM_LASTCHANGETIME, SQLType.Long, false))
            values.defineTable(TABLE_TRANSFERMEMBER)
                .define(SQLValues.Column(FIELD_TRANSFERMEMBER_TRANSFERID, SQLType.Long, false))
                .define(SQLValues.Column(FIELD_TRANSFERMEMBER_DEVICEID, SQLType.Text, false))
                .define(SQLValues.Column(FIELD_TRANSFERMEMBER_TYPE, SQLType.Text, false))
            values.defineTable(TABLE_TRANSFER)
                .define(SQLValues.Column(FIELD_TRANSFER_ID, SQLType.Long, false))
                .define(SQLValues.Column(FIELD_TRANSFER_DATECREATED, SQLType.Long, false))
                .define(SQLValues.Column(FIELD_TRANSFER_SAVEPATH, SQLType.Text, true))
                .define(SQLValues.Column(FIELD_TRANSFER_ISSHAREDONWEB, SQLType.Integer, true))
                .define(SQLValues.Column(FIELD_TRANSFER_ISPAUSED, SQLType.Integer, false))
            return values
        }
    }
}