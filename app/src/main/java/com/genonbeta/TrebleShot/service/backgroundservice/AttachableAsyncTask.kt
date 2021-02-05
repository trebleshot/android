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

import android.os.Handler
import android.os.Looper
import com.genonbeta.TrebleShot.service.backgroundserviceimport.TaskStoppedException

abstract class AttachableAsyncTask<T : AttachedTaskListener> : BaseAttachableAsyncTask() {
    private var mAnchor: T? = null
    private var mHandler: Handler? = null
    override fun hasAnchor(): Boolean {
        return mAnchor != null
    }

    @get:Throws(TaskStoppedException::class)
    var anchor: T?
        get() {
            throwIfStopped()
            return mAnchor
        }
    private val handler: Handler
        private get() {
            if (mHandler == null) {
                val myLooper = Looper.myLooper()
                mHandler = Handler(myLooper ?: Looper.getMainLooper())
            }
            return mHandler!!
        }

    private fun notifyAnchor(state: State) {
        if (hasAnchor()) mAnchor!!.onTaskStateChange(this, state)
    }

    @Throws(TaskStoppedException::class)
    override fun post(message: TaskMessage) {
        val anchor = anchor
        if (anchor == null || !anchor.onTaskMessage(message))
            super.post(message)
    }

    fun post(runnable: Runnable) {
        handler.post(runnable)
    }

    override fun publishStatus(force: Boolean): Boolean {
        if (!super.publishStatus(force))
            return false
        val state = state
        handler.post { notifyAnchor(state) }
        return true
    }

    override fun removeAnchor() {
        mAnchor = null
    }

    fun setAnchor(anchor: T) {
        mAnchor = anchor
        publishStatus(true)
    }
}