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
import android.content.*
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.os.Bundle
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.os.PowerManager
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.database.SQLType
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

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

    fun <T, V : DatabaseObject<T>?> removeAsynchronous(activity: Activity?, `object`: V, parent: T) {
        removeAsynchronous(App.Companion.from(activity), `object`, parent)
    }

    fun <T, V : DatabaseObject<T>?> removeAsynchronous(app: App, `object`: V, parent: T) {
        app.run(SingleRemovalTask(app.applicationContext, getWritableDatabase(), `object`, parent))
    }

    fun <T, V : DatabaseObject<T>?> removeAsynchronous(activity: Activity?, objects: List<V>?, parent: T) {
        removeAsynchronous(App.Companion.from(activity), objects, parent)
    }

    fun <T, V : DatabaseObject<T>?> removeAsynchronous(app: App, objects: List<V>?, parent: T) {
        app.run(MultipleRemovalTask(app.applicationContext, getWritableDatabase(), objects, parent))
    }

    private abstract class BgTaskImpl internal constructor(context: Context, titleRes: Int, db: SQLiteDatabase) :
        AsyncTask() {
        private val mDb: SQLiteDatabase
        private val mTitle: String
        override fun onProgressChange(progress: Progress) {
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
            mTitle = context.getString(titleRes)
            mDb = db
        }
    }

    private class SingleRemovalTask<T, V : DatabaseObject<T>?> internal constructor(
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

    private class MultipleRemovalTask<T, V : DatabaseObject<T>?> internal constructor(
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
                .define(Column(FIELD_CLIPBOARD_ID, SQLType.INTEGER, false))
                .define(Column(FIELD_CLIPBOARD_TEXT, SQLType.TEXT, false))
                .define(Column(FIELD_CLIPBOARD_TIME, SQLType.LONG, false))
            values.defineTable(TABLE_DEVICES)
                .define(Column(FIELD_DEVICES_ID, SQLType.TEXT, false))
                .define(Column(FIELD_DEVICES_USER, SQLType.TEXT, false))
                .define(Column(FIELD_DEVICES_BRAND, SQLType.TEXT, false))
                .define(Column(FIELD_DEVICES_MODEL, SQLType.TEXT, false))
                .define(Column(FIELD_DEVICES_BUILDNAME, SQLType.TEXT, false))
                .define(Column(FIELD_DEVICES_BUILDNUMBER, SQLType.INTEGER, false))
                .define(Column(FIELD_DEVICES_PROTOCOLVERSION, SQLType.INTEGER, false))
                .define(Column(FIELD_DEVICES_PROTOCOLVERSIONMIN, SQLType.INTEGER, false))
                .define(Column(FIELD_DEVICES_LASTUSAGETIME, SQLType.INTEGER, false))
                .define(Column(FIELD_DEVICES_ISRESTRICTED, SQLType.INTEGER, false))
                .define(Column(FIELD_DEVICES_ISTRUSTED, SQLType.INTEGER, false))
                .define(Column(FIELD_DEVICES_ISLOCALADDRESS, SQLType.INTEGER, false))
                .define(Column(FIELD_DEVICES_SENDKEY, SQLType.INTEGER, true))
                .define(Column(FIELD_DEVICES_RECEIVEKEY, SQLType.INTEGER, true))
                .define(Column(FIELD_DEVICES_TYPE, SQLType.TEXT, false))
            values.defineTable(TABLE_DEVICEADDRESS)
                .define(Column(FIELD_DEVICEADDRESS_IPADDRESS, SQLType.BLOB, false))
                .define(Column(FIELD_DEVICEADDRESS_IPADDRESSTEXT, SQLType.TEXT, false))
                .define(Column(FIELD_DEVICEADDRESS_DEVICEID, SQLType.TEXT, false))
                .define(Column(FIELD_DEVICEADDRESS_LASTCHECKEDDATE, SQLType.INTEGER, false))
            values.defineTable(TABLE_FILEBOOKMARK)
                .define(Column(FIELD_FILEBOOKMARK_TITLE, SQLType.TEXT, false))
                .define(Column(FIELD_FILEBOOKMARK_PATH, SQLType.TEXT, false))
            values.defineTable(TABLE_TRANSFERITEM)
                .define(Column(FIELD_TRANSFERITEM_ID, SQLType.LONG, false))
                .define(Column(FIELD_TRANSFERITEM_TRANSFERID, SQLType.LONG, false))
                .define(Column(FIELD_TRANSFERITEM_DIRECTORY, SQLType.TEXT, true))
                .define(Column(FIELD_TRANSFERITEM_FILE, SQLType.TEXT, false))
                .define(Column(FIELD_TRANSFERITEM_NAME, SQLType.TEXT, false))
                .define(Column(FIELD_TRANSFERITEM_SIZE, SQLType.INTEGER, false))
                .define(Column(FIELD_TRANSFERITEM_MIME, SQLType.TEXT, false))
                .define(Column(FIELD_TRANSFERITEM_TYPE, SQLType.TEXT, false))
                .define(Column(FIELD_TRANSFERITEM_FLAG, SQLType.TEXT, false))
                .define(Column(FIELD_TRANSFERITEM_LASTCHANGETIME, SQLType.LONG, false))
            values.defineTable(TABLE_TRANSFERMEMBER)
                .define(Column(FIELD_TRANSFERMEMBER_TRANSFERID, SQLType.LONG, false))
                .define(Column(FIELD_TRANSFERMEMBER_DEVICEID, SQLType.TEXT, false))
                .define(Column(FIELD_TRANSFERMEMBER_TYPE, SQLType.TEXT, false))
            values.defineTable(TABLE_TRANSFER)
                .define(Column(FIELD_TRANSFER_ID, SQLType.LONG, false))
                .define(Column(FIELD_TRANSFER_DATECREATED, SQLType.LONG, false))
                .define(Column(FIELD_TRANSFER_SAVEPATH, SQLType.TEXT, true))
                .define(Column(FIELD_TRANSFER_ISSHAREDONWEB, SQLType.INTEGER, true))
                .define(Column(FIELD_TRANSFER_ISPAUSED, SQLType.INTEGER, false))
            return values
        }
    }
}