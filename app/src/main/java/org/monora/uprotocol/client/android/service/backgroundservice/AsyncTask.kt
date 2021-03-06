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
package org.monora.uprotocol.client.android.service.backgroundservice

import android.app.PendingIntent
import android.content.*
import android.media.MediaScannerConnection
import com.genonbeta.android.database.Progress
import com.genonbeta.android.framework.util.Stoppable
import com.genonbeta.android.framework.util.StoppableImpl
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.model.Identifiable
import org.monora.uprotocol.client.android.model.Identifier.Companion.from
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.model.Identity.Companion.withORs
import org.monora.uprotocol.client.android.util.DynamicNotification
import org.monora.uprotocol.client.android.util.Notifications
import org.monora.uprotocol.client.android.util.StoppableJob

abstract class AsyncTask : StoppableJob(), Stoppable, Identifiable {
    var activityIntent: PendingIntent? = null
        private set

    protected lateinit var backend: BackgroundBackend
        private set

    override val closers: MutableList<Stoppable.Closer>
        get() = stoppable.closers

    val context: Context
        get() = backend.context

    private var customHashCode = 0

    protected var customNotification // The notification that is not part of the default notification.
            : DynamicNotification? = null

    var finished = false
        private set

    override val identity: Identity
        get() = withORs(from(Id.HashCode, hashCode()))

    val mediaScanner: MediaScannerConnection
        get() = backend.mediaScanner

    val name: String
        get() = getName(context)

    val notifications: Notifications
        get() = backend.notificationHelper

    var ongoingContent: String? = null

    val progress: Progress.Context = ProgressContext()

    var started = false
        private set

    var startTime: Long = 0
        private set

    private var stoppable = StoppableImpl()

    protected open fun onProgressChange(progress: Progress) {
        publishStatus()
    }

    open fun onPublishStatus() {}

    override fun addCloser(closer: Stoppable.Closer): Boolean {
        return stoppable.addCloser(closer)
    }

    open fun forceQuit() {
        if (!interrupted())
            interrupt()
    }

    abstract fun getName(context: Context): String

    fun getState(): State {
        return if (!started) State.Starting else if (!finished) State.Running else State.Finished
    }

    open fun getTaskGroup() = TASK_GROUP_DEFAULT

    override fun hasCloser(closer: Stoppable.Closer): Boolean {
        return stoppable.hasCloser(closer)
    }

    override fun hashCode(): Int {
        return if (customHashCode != 0) customHashCode else super.hashCode()
    }

    override fun interrupt(): Boolean {
        return stoppable.interrupt()
    }

    override fun interrupt(userAction: Boolean): Boolean {
        return stoppable.interrupt(userAction)
    }

    override fun interrupted(): Boolean = stoppable.interrupted()

    override fun interruptedByUser(): Boolean = stoppable.interruptedByUser()

    @Throws(TaskStoppedException::class)
    open fun post(message: TaskMessage) {
        throwIfStopped()
        val notification: DynamicNotification = message.toNotification(this).show()
        customNotification = notification
    }

    fun publishStatus(): Boolean {
        return publishStatus(false)
    }

    protected open fun publishStatus(force: Boolean): Boolean {
        return started && !finished && backend.publishTaskNotifications(force)
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
        check(!(started && !finished)) { "Can't reset when the task is running" }
        started = false
        finished = false
        progress.progress = null
    }

    override fun removeClosers() {
        stoppable.removeClosers()
    }

    fun run(backgroundBackend: BackgroundBackend) {
        check(!(started || finished || interrupted())) { javaClass.name + " isStarted" }

        startTime = System.currentTimeMillis()
        backend = backgroundBackend

        started = true
        publishStatus(true)

        try {
            run(stoppable)
        } catch (e: TaskStoppedException) {
            e.printStackTrace()
        } finally {
            finished = true
            publishStatus(true)
        }
    }

    fun setContentIntent(context: Context, intent: Intent) {
        customHashCode = hashIntent(intent)
        activityIntent = PendingIntent.getActivity(context, 0, intent, 0)
    }

    @Throws(TaskStoppedException::class)
    fun throwIfStopped() {
        if (interrupted())
            throw TaskStoppedException("This task been interrupted", interrupted())
    }

    private inner class ProgressContext : Progress.SimpleContext() {
        override fun onProgressChange(progress: Progress): Boolean {
            this@AsyncTask.onProgressChange(progress)
            publishStatus()
            return !interrupted()
        }
    }

    enum class Id {
        HashCode
    }

    enum class State {
        Starting, Running, Finished
    }

    companion object {
        const val TASK_GROUP_DEFAULT = "TASK_GROUP_DEFAULT"

        fun hashIntent(intent: Intent): Int {
            val builder = StringBuilder()
                .append(intent.component)
                .append(intent.data)
                .append(intent.getPackage())
                .append(intent.action)
                .append(intent.flags)
                .append(intent.type)

            if (intent.extras != null)
                builder.append(intent.extras.toString())

            return builder.toString().hashCode()
        }
    }
}