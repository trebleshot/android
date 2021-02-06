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

import android.app.PendingIntent
import android.content.*
import android.media.MediaScannerConnection
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Identifiable
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.Identity
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.service.backgroundserviceimport.TaskStoppedException
import com.genonbeta.TrebleShot.util.DynamicNotification
import com.genonbeta.TrebleShot.util.NotificationHelper
import com.genonbeta.TrebleShot.util.StoppableJob
import com.genonbeta.android.database.Progress
import com.genonbeta.android.framework.util.Stoppable
import com.genonbeta.android.framework.util.StoppableImpl

abstract class AsyncTask : StoppableJob(), Stoppable, Identifiable {
    private var activityIntent: PendingIntent? = null

    protected var app: App? = null
        private set

    val closers: List<Any>
        get() = stoppable.closers

    protected var customNotification // The notification that is not part of the default notification.
            : DynamicNotification? = null

    private var kuick: Kuick? = null

    private val progressListener: ProgressListener = ProgressListener()
        get() {
            // This ensures when Progress.Listener.getProgress() is called, it doesn't return a null object.
            // Of course, if the user needs the progress itself, then, he or she should use #progress() method.
            progress()
            return this
        }

    private var stoppable = StoppableImpl()

    var isFinished = false
        private set

    var isStarted = false
        private set

    var startTime: Long = 0
        private set

    var ongoingContent: String? = null

    private var mHash = 0

    protected open fun onProgressChange(progress: Progress) {
        publishStatus()
    }

    open fun onPublishStatus() {}

    override fun addCloser(closer: Stoppable.Closer): Boolean {
        return stoppable.addCloser(closer)
    }

    open fun forceQuit() {
        if (!isInterrupted)
            interrupt()
    }

    val context: Context
        get() = app.getApplicationContext()


    val identity: Identity
        get() = withORs(from(Id.HashCode, hashCode()))
    protected val mediaScanner: MediaScannerConnection?
        protected get() = app.mediaScanner
    protected val name: String?
        protected get() = getName(context)

    abstract fun getName(context: Context?): String?
    val notificationHelper: NotificationHelper
        get() = app.notificationHelper
    val state: State
        get() {
            if (!isStarted) return State.Starting else if (!isFinished) return State.Running
            return State.Finished
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
        get() = stoppable.isInterrupted

    val isInterruptedByUser: Boolean
        get() = stoppable.isInterruptedByUser


    @Throws(TaskStoppedException::class)
    open fun post(message: TaskMessage) {
        throwIfStopped()
        val notification: DynamicNotification = message.toNotification(this).show()
        customNotification = notification
    }

    fun progress(): Progress {
        return Progress.dissect(progressListener)
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
        progressListener().progress = null
    }

    override fun removeClosers() {
        stoppable.removeClosers()
    }

    fun run(app: App?) {
        check(!(isStarted || isFinished || isInterrupted)) { javaClass.name + " isStarted" }
        startTime = System.currentTimeMillis()
        this.app,
        = app
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