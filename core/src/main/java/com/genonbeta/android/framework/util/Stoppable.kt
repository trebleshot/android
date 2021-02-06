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

/**
 * created by: Veli
 * date: 20.11.2017 00:15
 *
 *
 * A way of informing threads and objects. The aim is to make sure the same object can be used more than one places
 * (threads and UI elements). It also helps you make you are closing or removing temporary objects when the task is
 * cancelled.
 */
interface Stoppable {
    /**
     * Add an object to be invoked when the task is cancelled.
     *
     * @param closer to be called when the [.interrupt] is called
     * @return true when adding to the list is successful
     */
    open fun addCloser(closer: Closer?): Boolean

    /**
     * Check if the callback was previously added to the list.
     *
     * @param closer to be checked
     * @return true if it was already added
     */
    open fun hasCloser(closer: Closer?): Boolean

    /**
     * Objects pending to be called when the task is called.
     *
     * @return pending list of objects
     */
    open fun getClosers(): MutableList<Closer?>?

    /**
     * Ensure if the task has been cancelled.
     *
     * @return true if it was
     */
    open fun isInterrupted(): Boolean

    /**
     * Was the task called with [.interrupt] with userAction boolean set to true?
     *
     * @return true if the was cancelled with userAction boolean was true
     */
    open fun isInterruptedByUser(): Boolean

    /**
     * Cancel the task with 'userAction' is set to true.
     *
     * @see .interrupt
     */
    open fun interrupt(): Boolean

    /**
     * Cancel the task and call the [Closer] objects if it was not cancelled previously.
     *
     * @param userAction true if it is performed by user
     * @return true if it was not cancelled before
     */
    open fun interrupt(userAction: Boolean): Boolean

    /**
     * Remove a previously added @link Closer} object from the list.
     *
     * @param closer to be removed
     * @return true if it has been removed
     */
    open fun removeCloser(closer: Closer?): Boolean

    /**
     * @see .reset
     */
    open fun reset()

    /**
     * Reset the interrupted flags and remove [Closer] objects if needed.
     *
     * @param resetClosers true if you want to remove the [Closer] objects
     */
    open fun reset(resetClosers: Boolean)

    /**
     * Remove all closers.
     */
    open fun removeClosers()

    /**
     * When interrupted, invoke this. This will not be called a second time.
     */
    interface Closer {
        /**
         * [Stoppable.interrupt] will invoke this when an instance is provided using
         * [Stoppable.addCloser].
         *
         * @param userAction true the [Stoppable.interrupt] is invoked with userAction = true
         */
        open fun onClose(userAction: Boolean)
    }
}