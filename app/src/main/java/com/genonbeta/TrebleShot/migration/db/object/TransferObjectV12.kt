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
package com.genonbeta.TrebleShot.migration.db.`object`

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
import android.content.DialogInterface
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import android.content.Intent
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
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.content.IntentFilter
import android.content.BroadcastReceiver
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
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.Exception

/**
 * created by: veli
 * date: 7/31/19 11:00 AM
 */
class TransferObjectV12 : DatabaseObject<TransferGroupV12?> {
    var friendlyName: String? = null
    var file: String? = null
    var fileMimeType: String? = null
    var directory: String? = null
    var deviceId: String? = null
    var requestId: Long = 0
    var groupId: Long = 0
    var skippedBytes: Long = 0
    var fileSize: Long = 0
    var accessPort = 0
    var type = Type.INCOMING
    var flag = Flag.PENDING

    constructor() {}
    constructor(
        requestId: Long, groupId: Long, friendlyName: String?, file: String?, fileMime: String?,
        fileSize: Long, type: Type
    ) : this(requestId, groupId, null, friendlyName, file, fileMime, fileSize, type) {
    }

    constructor(
        requestId: Long, groupId: Long, deviceId: String?, friendlyName: String?, file: String?,
        fileMime: String?, fileSize: Long, type: Type
    ) {
        this.friendlyName = friendlyName
        this.file = file
        this.fileSize = fileSize
        fileMimeType = fileMime
        this.deviceId = deviceId
        this.requestId = requestId
        this.groupId = groupId
        this.type = type
    }

    constructor(requestId: Long, deviceId: String?, type: Type) {
        this.requestId = requestId
        this.deviceId = deviceId
        this.type = type
    }

    override fun equals(obj: Any?): Boolean {
        if (obj !is TransferObjectV12) return super.equals(obj)
        val otherObject = obj
        return otherObject.requestId == requestId && type == otherObject.type && ((deviceId == null
                && otherObject.deviceId == null) || (deviceId != null
                && deviceId == otherObject.deviceId))
    }

    fun isDivisionObject(): Boolean {
        return deviceId == null
    }

    override fun getWhere(): SQLQuery.Select {
        val whereClause = if (isDivisionObject()) String.format(
            "%s = ? AND %s = ?",
            Kuick.Companion.FIELD_TRANSFERITEM_ID,
            Kuick.Companion.FIELD_TRANSFERITEM_TYPE
        ) else String.format(
            "%s = ? AND %s = ? AND %s = ?", Kuick.Companion.FIELD_TRANSFERITEM_ID,
            Kuick.Companion.FIELD_TRANSFERITEM_TYPE, v12.Companion.FIELD_TRANSFER_DEVICEID
        )
        return if (isDivisionObject()) SQLQuery.Select(v12.Companion.TABLE_DIVISTRANSFER).setWhere(
            whereClause,
            requestId.toString(),
            type.toString()
        ) else SQLQuery.Select(Kuick.Companion.TABLE_TRANSFERITEM).setWhere(
            whereClause, requestId.toString(), type.toString(), deviceId
        )
    }

    override fun getValues(): ContentValues {
        val values = ContentValues()
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_ID, requestId)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID, groupId)
        values.put(v12.Companion.FIELD_TRANSFER_DEVICEID, deviceId)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_NAME, friendlyName)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_SIZE, fileSize)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_MIME, fileMimeType)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_FLAG, flag.toString())
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_TYPE, type.toString())
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_FILE, file)
        values.put(v12.Companion.FIELD_TRANSFER_ACCESSPORT, accessPort)
        values.put(v12.Companion.FIELD_TRANSFER_SKIPPEDBYTES, skippedBytes)
        values.put(Kuick.Companion.FIELD_TRANSFERITEM_DIRECTORY, directory)
        return values
    }

    override fun reconstruct(db: SQLiteDatabase, kuick: KuickDb, item: ContentValues) {
        friendlyName = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_NAME)
        file = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_FILE)
        fileSize = item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_SIZE)
        fileMimeType = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_MIME)
        requestId = item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_ID)
        groupId = item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_TRANSFERID)
        deviceId = item.getAsString(v12.Companion.FIELD_TRANSFER_DEVICEID)
        type = Type.valueOf(item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_TYPE))

        // We may have put long in that field indicating that the file was / is in progress so generate
        try {
            flag = Flag.valueOf(item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_FLAG))
        } catch (e: Exception) {
            flag = Flag.IN_PROGRESS
            flag.setBytesValue(item.getAsLong(Kuick.Companion.FIELD_TRANSFERITEM_FLAG))
        }
        accessPort = item.getAsInteger(v12.Companion.FIELD_TRANSFER_ACCESSPORT)
        skippedBytes = item.getAsLong(v12.Companion.FIELD_TRANSFER_SKIPPEDBYTES)
        directory = item.getAsString(Kuick.Companion.FIELD_TRANSFERITEM_DIRECTORY)
    }

    override fun onCreateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: TransferGroupV12,
        listener: Progress.Listener
    ) {
    }

    override fun onUpdateObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: TransferGroupV12,
        listener: Progress.Listener
    ) {
    }

    override fun onRemoveObject(
        db: SQLiteDatabase,
        kuick: KuickDb,
        parent: TransferGroupV12,
        listener: Progress.Listener
    ) {
    }

    enum class Type {
        INCOMING, OUTGOING
    }

    enum class Flag {
        INTERRUPTED, PENDING, REMOVED, IN_PROGRESS, DONE;

        private var bytesValue: Long = 0
        fun getBytesValue(): Long {
            return bytesValue
        }

        fun setBytesValue(bytesValue: Long) {
            this.bytesValue = bytesValue
        }

        override fun toString(): String {
            return if (getBytesValue() > 0) getBytesValue().toString() else super.toString()
        }
    }
}