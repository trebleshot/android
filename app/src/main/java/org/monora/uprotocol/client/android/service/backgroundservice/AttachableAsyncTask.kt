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

import android.os.Handler
import android.os.Looper

abstract class AttachableAsyncTask<T : AttachedTaskListener> : BaseAttachableAsyncTask() {
    private var handler = Handler(Looper.myLooper() ?: Looper.getMainLooper())

    @get:Throws(TaskStoppedException::class)
    var anchor: T? = null
        set(value) {
            field = value
            publishStatus(true)
        }

    override fun hasAnchor(): Boolean {
        return anchor != null
    }

    private fun notifyAnchor(state: State) {
        anchor?.onTaskStateChange(this, state)
    }

    @Throws(TaskStoppedException::class)
    override fun post(message: TaskMessage) {
        if (anchor?.onTaskMessage(message) == false)
            super.post(message)
    }

    fun post(runnable: Runnable) {
        handler.post(runnable)
    }

    override fun publishStatus(force: Boolean): Boolean {
        if (!super.publishStatus(force) && !force)
            return false
        val state = getState()
        handler.post { notifyAnchor(state) }
        return true
    }

    override fun removeAnchor() {
        anchor = null
    }
}