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
package com.genonbeta.android.framework.util

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.util.ArrayList

class StoppableImpl : Stoppable {
    private var mInterrupted = false
    private var mInterruptedByUser = false
    private val mClosers: MutableList<Stoppable.Closer?>? = ArrayList()
    override fun addCloser(closer: Stoppable.Closer?): Boolean {
        synchronized(closers) { return closers.add(closer) }
    }

    override fun hasCloser(closer: Stoppable.Closer?): Boolean {
        synchronized(closers) { return closers.contains(closer) }
    }

    override fun getClosers(): MutableList<Stoppable.Closer?>? {
        return mClosers
    }

    override fun isInterrupted(): Boolean {
        return mInterrupted
    }

    override fun isInterruptedByUser(): Boolean {
        return mInterruptedByUser
    }

    override fun interrupt(): Boolean {
        return interrupt(true)
    }

    override fun interrupt(userAction: Boolean): Boolean {
        if (userAction) mInterruptedByUser = true
        if (isInterrupted) return false
        mInterrupted = true
        synchronized(closers) { for (closer in closers) closer.onClose(userAction) }
        return true
    }

    override fun removeCloser(closer: Stoppable.Closer?): Boolean {
        synchronized(closers) { return closers.remove(closer) }
    }

    override fun reset() {
        reset(true)
    }

    override fun reset(resetClosers: Boolean) {
        mInterrupted = false
        mInterruptedByUser = false
        if (resetClosers) removeClosers()
    }

    override fun removeClosers() {
        synchronized(closers) { closers.clear() }
    }
}