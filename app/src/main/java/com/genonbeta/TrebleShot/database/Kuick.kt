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
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.database.*

/**
 * Created by: veli
 * Date: 4/14/17 11:47 PM
 */
class Kuick(context: Context) : KuickDb(context, DATABASE_NAME, null, DATABASE_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        SQLQuery.createTables(db, tables())
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, current: Int) {
        Migration.migrate(this, db, old, current)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(activity: Activity, item: V, parent: T) {
        removeAsynchronous(App.from(activity), item, parent)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(app: App, item: V, parent: T) {
        app.run(SingleRemovalTask(app.applicationContext, writableDatabase, item, parent))
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(activity: Activity, objects: List<V>, parent: T) {
        removeAsynchronous(App.from(activity), objects, parent)
    }

    fun <T, V : DatabaseObject<T>> removeAsynchronous(app: App, objects: List<V>, parent: T) {
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
            kuick.remove(db, targetObject, parent, progress)
            kuick.broadcast()
        }
    }

    private class MultipleRemovalTask<T, V : DatabaseObject<T>>(
        context: Context,
        db: SQLiteDatabase,
        private val targetObjectList: List<V>,
        private val parent: T?,
    ) : BgTaskImpl(context, R.string.mesg_removing, db) {
        override fun onRun() {
            kuick.remove(db, targetObjectList, parent, progress)
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
            values.defineTable(TABLE_CLIPBOARD).also {
                it += SQLValues.Column(FIELD_CLIPBOARD_ID, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_CLIPBOARD_TEXT, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_CLIPBOARD_TIME, type = SQLType.Long, nullable = false)
            }

            values.defineTable(TABLE_DEVICES).also {
                it += SQLValues.Column(FIELD_DEVICES_ID, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_USER, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_BRAND, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_MODEL, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_BUILDNAME, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_BUILDNUMBER, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_PROTOCOLVERSION, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_PROTOCOLVERSIONMIN, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_LASTUSAGETIME, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_ISRESTRICTED, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_ISTRUSTED, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_ISLOCALADDRESS, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_DEVICES_SENDKEY, type = SQLType.Integer, nullable = true)
                it += SQLValues.Column(FIELD_DEVICES_RECEIVEKEY, type = SQLType.Integer, nullable = true)
                it += SQLValues.Column(FIELD_DEVICES_TYPE, type = SQLType.Text, nullable = false)
            }

            values.defineTable(TABLE_DEVICEADDRESS).also {
                it += SQLValues.Column(FIELD_DEVICEADDRESS_IPADDRESS, type = SQLType.Blob, nullable = false)
                it += SQLValues.Column(FIELD_DEVICEADDRESS_IPADDRESSTEXT, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_DEVICEADDRESS_DEVICEID, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_DEVICEADDRESS_LASTCHECKEDDATE, type = SQLType.Integer, nullable = false)
            }

            values.defineTable(TABLE_FILEBOOKMARK).also {
                it += SQLValues.Column(FIELD_FILEBOOKMARK_TITLE, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_FILEBOOKMARK_PATH, type = SQLType.Text, nullable = false)
            }

            values.defineTable(TABLE_TRANSFERITEM).also {
                it += SQLValues.Column(FIELD_TRANSFERITEM_ID, type = SQLType.Long, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_TRANSFERID, type = SQLType.Long, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_DIRECTORY, type = SQLType.Text, nullable = true)
                it += SQLValues.Column(FIELD_TRANSFERITEM_FILE, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_NAME, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_SIZE, type = SQLType.Integer, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_MIME, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_TYPE, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_FLAG, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERITEM_LASTCHANGETIME, type = SQLType.Long, nullable = false)
            }

            values.defineTable(TABLE_TRANSFERMEMBER).also {
                it += SQLValues.Column(FIELD_TRANSFERMEMBER_TRANSFERID, type = SQLType.Long, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERMEMBER_DEVICEID, type = SQLType.Text, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFERMEMBER_TYPE, type = SQLType.Text, nullable = false)
            }

            values.defineTable(TABLE_TRANSFER).also {
                it += SQLValues.Column(FIELD_TRANSFER_ID, type = SQLType.Long, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFER_DATECREATED, type = SQLType.Long, nullable = false)
                it += SQLValues.Column(FIELD_TRANSFER_SAVEPATH, type = SQLType.Text, nullable = true)
                it += SQLValues.Column(FIELD_TRANSFER_ISSHAREDONWEB, type = SQLType.Integer, nullable = true)
                it += SQLValues.Column(FIELD_TRANSFER_ISPAUSED, type = SQLType.Integer, nullable = false)
            }
            return values
        }
    }
}