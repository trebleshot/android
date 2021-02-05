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
import com.genonbeta.TrebleShot.dataobject.Identity
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.StringBuilder

abstract class AsyncTask : StoppableJob(), Stoppable, Identifiable {
    private val mProgressListener: ProgressListener = ProgressListener()
    private var mKuick: Kuick? = null
    protected var app: App? = null
        private set
    private var mStoppable: Stoppable? = null
    private var mActivityIntent: PendingIntent? = null
    private var mCustomNotification // The notification that is not part of the default notification.
            : DynamicNotification? = null
    var ongoingContent: String? = null
    var isFinished = false
        private set
    var isStarted = false
        private set
    var startTime: Long = 0
        private set
    private var mHash = 0
    @Throws(TaskStoppedException::class)
    protected abstract fun onRun()
    protected open fun onProgressChange(progress: Progress) {
        publishStatus()
    }

    open fun onPublishStatus() {}
    override fun addCloser(closer: Stoppable.Closer): Boolean {
        return stoppable.addCloser(closer)
    }

    open fun forceQuit() {
        if (!isInterrupted) interrupt()
    }

    val context: Context
        get() = app.getApplicationContext()
    val closers: List<Any>
        get() = stoppable.getClosers()
    var contentIntent: PendingIntent?
        get() = mActivityIntent
        set(intent) {
            mActivityIntent = intent
        }
    var customNotification: DynamicNotification?
        get() = mCustomNotification
        set(notification) {
            mCustomNotification = notification
        }
    val identity: Identity
        get() = withORs(from(Id.HashCode, hashCode()))
    protected val mediaScanner: MediaScannerConnection?
        protected get() = app!!.mediaScanner
    protected val name: String?
        protected get() = getName(context)

    abstract fun getName(context: Context?): String?
    val notificationHelper: NotificationHelper?
        get() = app!!.notificationHelper
    val state: State
        get() {
            if (!isStarted) return State.Starting else if (!isFinished) return State.Running
            return State.Finished
        }
    private var stoppable: Stoppable?
        private get() {
            if (mStoppable == null) mStoppable = StoppableImpl()
            return mStoppable
        }
        set(stoppable) {
            mStoppable = stoppable
        }

    override fun hasCloser(closer: Stoppable.Closer): Boolean {
        return stoppable.hasCloser(closer)
    }

    override fun hashCode(): Int {
        return if (mHash != 0) mHash else super.hashCode()
    }

    override fun interrupt(): Boolean {
        return stoppable.interrupt()
    }

    override fun interrupt(userAction: Boolean): Boolean {
        return stoppable.interrupt(userAction)
    }

    val isInterrupted: Boolean
        get() = stoppable.isInterrupted()
    val isInterruptedByUser: Boolean
        get() = stoppable.isInterruptedByUser()

    fun kuick(): Kuick? {
        if (mKuick == null) mKuick = AppUtils.getKuick(context)
        return mKuick
    }

    @Throws(TaskStoppedException::class)
    open fun post(message: TaskMessage) {
        throwIfStopped()
        val notification: DynamicNotification = message.toNotification(this).show()
        customNotification = notification
    }

    fun progress(): Progress {
        return Progress.dissect(mProgressListener)
    }

    fun progressListener(): Progress.Listener {
        // This ensures when Progress.Listener.getProgress() is called, it doesn't return a null object.
        // Of course, if the user needs the progress itself, then, he or she should use #progress() method.
        progress()
        return mProgressListener
    }

    fun publishStatus(): Boolean {
        return publishStatus(false)
    }

    protected open fun publishStatus(force: Boolean): Boolean {
        return app != null && app!!.publishTaskNotifications(force)
    }

    override fun removeCloser(closer: Stoppable.Closer): Boolean {
        return stoppable.removeCloser(closer)
    }

    override fun reset() {
        resetInternal()
        stoppable.reset()
    }

    override fun reset(resetClosers: Boolean) {
        resetInternal()
        stoppable.reset(resetClosers)
    }

    private fun resetInternal() {
        check(!(isStarted && !isFinished)) { "Can't reset when the task is running" }
        isStarted = false
        isFinished = false
        progressListener().setProgress(null)
    }

    override fun removeClosers() {
        stoppable.removeClosers()
    }

    fun run(app: App?) {
        check(!(isStarted || isFinished || isInterrupted)) { javaClass.name + " isStarted" }
        startTime = System.currentTimeMillis()
        this.app = app
        publishStatus(true)
        isStarted = true
        try {
            run(stoppable)
        } catch (ignored: TaskStoppedException) {
        } finally {
            isFinished = true
            publishStatus(true)
            this.app = null
        }
    }

    fun setContentIntent(context: Context?, intent: Intent) {
        mHash = hashIntent(intent)
        contentIntent = PendingIntent.getActivity(context, 0, intent, 0)
    }

    @Throws(TaskStoppedException::class)
    fun throwIfStopped() {
        if (isInterrupted) throw TaskStoppedException("This task been interrupted", isInterruptedByUser)
    }

    private inner class ProgressListener : SimpleListener() {
        override fun onProgressChange(progress: Progress): Boolean {
            this@AsyncTask.onProgressChange(progress)
            publishStatus()
            return !isInterrupted
        }
    }

    enum class Id {
        HashCode
    }

    enum class State {
        Starting, Running, Finished
    }

    companion object {
        val taskGroup = "TASK_GROUP_DEFAULT"
            get() = Companion.field

        fun hashIntent(intent: Intent): Int {
            val builder = StringBuilder()
                .append(intent.component)
                .append(intent.data)
                .append(intent.getPackage())
                .append(intent.action)
                .append(intent.flags)
                .append(intent.type)
            if (intent.extras != null) builder.append(intent.extras.toString())
            return builder.toString().hashCode()
        }
    }
}