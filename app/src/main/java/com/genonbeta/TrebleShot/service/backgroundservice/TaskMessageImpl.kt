/*
 * Copyright (C) 2020 Veli Tasalı
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
package com.genonbeta.TrebleShot.service.backgroundservice

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
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.app.AlertDialog
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.StringBuilder
import java.util.ArrayList

class TaskMessageImpl : TaskMessage {
    private var mTitle: String? = null
    private var mMessage: String? = null
    private var mTone: Tone = Tone.Neutral
    private val mActionList: MutableList<TaskMessage.Action> = ArrayList()
    override fun addAction(action: TaskMessage.Action): TaskMessage {
        synchronized(mActionList) { mActionList.add(action) }
        return this
    }

    override fun addAction(context: Context, nameRes: Int, callback: TaskMessage.Callback?): TaskMessage {
        return addAction(context.getString(nameRes), callback)
    }

    override fun addAction(name: String?, callback: TaskMessage.Callback?): TaskMessage {
        return addAction(name, Tone.Neutral, callback)
    }

    override fun addAction(context: Context, nameRes: Int, tone: Tone?, callback: TaskMessage.Callback?): TaskMessage {
        return addAction(context.getString(nameRes), tone, callback)
    }

    override fun addAction(name: String?, tone: Tone?, callback: TaskMessage.Callback?): TaskMessage {
        val action = TaskMessage.Action()
        action.name = name
        action.tone = tone
        action.callback = callback
        return addAction(action)
    }

    override fun getActionList(): List<TaskMessage.Action> {
        synchronized(mActionList) { return ArrayList(mActionList) }
    }

    override fun getMessage(): String? {
        return mMessage
    }

    override fun getTitle(): String? {
        return mTitle
    }

    override fun getTone(): Tone {
        return mTone
    }

    override fun removeAction(action: TaskMessage.Action): TaskMessage {
        synchronized(mActionList) { mActionList.remove(action) }
        return this
    }

    override fun setMessage(context: Context, msgRes: Int): TaskMessage {
        return setMessage(context.getString(msgRes))
    }

    override fun setMessage(msg: String?): TaskMessage {
        mMessage = msg
        return this
    }

    override fun setTitle(context: Context, titleRes: Int): TaskMessage {
        return setTitle(context.getString(titleRes))
    }

    override fun setTitle(title: String?): TaskMessage {
        mTitle = title
        return this
    }

    override fun setTone(tone: Tone): TaskMessage {
        mTone = tone
        return this
    }

    override fun sizeOfActions(): Int {
        synchronized(mActionList) { return mActionList.size }
    }

    override fun toDialogBuilder(activity: Activity?): AlertDialog.Builder {
        val builder = AlertDialog.Builder(activity!!)
            .setTitle(title)
            .setMessage(message)
        synchronized(mActionList) {
            val appliedTones = BooleanArray(TaskMessage.Tone.values().size)
            for (action in mActionList) {
                if (appliedTones[action.tone.ordinal]) continue
                when (action.tone) {
                    Tone.Positive -> builder.setPositiveButton(action.name) { dialog: DialogInterface?, which: Int ->
                        action.callback.call(
                            activity
                        )
                    }
                    Tone.Negative -> builder.setNegativeButton(action.name) { dialog: DialogInterface?, which: Int ->
                        action.callback.call(
                            activity
                        )
                    }
                    Tone.Neutral -> builder.setNeutralButton(action.name) { dialog: DialogInterface?, which: Int ->
                        action.callback.call(
                            activity
                        )
                    }
                }
                appliedTones[action.tone.ordinal] = true
            }
            if (appliedTones.size < 1 || !appliedTones[Tone.Negative.ordinal]) builder.setNegativeButton(
                R.string.butn_close,
                null
            )
        }
        return builder
    }

    override fun toNotification(task: AsyncTask): DynamicNotification? {
        val context = task.context.applicationContext
        val utils = task.notificationHelper.utils
        val notification: DynamicNotification? = utils.buildDynamicNotification(
            task.hashCode().toLong(),
            NotificationUtils.Companion.NOTIFICATION_CHANNEL_HIGH
        )
        val intent: PendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0
        )
        notification.setSmallIcon(iconFor(mTone))
            .setGroup(task.taskGroup)
            .setContentTitle(mTitle)
            .setContentText(mMessage)
            .setContentIntent(intent)
            .setAutoCancel(true)
        for (action in mActionList) notification.addAction(
            iconFor(action.tone), action.name, PendingIntent.getActivity(
                context,
                0, Intent(context, HomeActivity::class.java), 0
            )
        )
        return notification
    }

    override fun toSnackbar(view: View?): Snackbar {
        val snackbar: Snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        if (sizeOfActions() > 0) {
            synchronized(mActionList) {
                val action = mActionList[0]
                snackbar.setAction(action.name, View.OnClickListener { v: View -> action.callback.call(v.context) })
            }
        }
        return snackbar
    }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Title=")
            .append(title)
            .append(" Msg=")
            .append(message)
            .append(" Tone=")
            .append(tone)
        for (action in mActionList) stringBuilder.append(action)
        return stringBuilder.toString()
    }

    companion object {
        @DrawableRes
        fun iconFor(tone: Tone?): Int {
            return when (tone) {
                Tone.Confused -> R.drawable.ic_help_white_24_static
                Tone.Positive -> R.drawable.ic_check_white_24dp_static
                Tone.Negative -> R.drawable.ic_close_white_24dp_static
                Tone.Neutral -> R.drawable.ic_trebleshot_white_24dp_static
                else -> R.drawable.ic_trebleshot_white_24dp_static
            }
        }
    }
}