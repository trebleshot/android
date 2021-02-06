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
package com.genonbeta.android.framework.util.actionperformer

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
 * A UI-related class that handles [IEngineConnection] and [PerformerCallback] to help them communicate with
 * the UI and each other.
 *
 * @see PerformerEngine as an implementation example
 */
interface IPerformerEngine {
    /**
     * This is called when we want to ensure if there is any [IBaseEngineConnection] on any slot.
     *
     * @return true when there is at least one
     */
    open fun hasActiveSlots(): Boolean

    /**
     * Ensure that the related connection is known and has an active slot in the list of connections.
     *
     * @param selectionConnection is the connection that should have an active connection
     * @return true if there is already a connection or added a new one.
     */
    open fun ensureSlot(provider: PerformerEngineProvider?, selectionConnection: IBaseEngineConnection?): Boolean

    /**
     * Inform all the [PerformerListener] objects after the [.check] call. Unlike that method, this doesn't have any ability to manipulate the task.
     *
     * @param engineConnection that is making the call
     * @param selectable       item that is being updated
     * @param isSelected       true when [Selectable] is being marked as selected
     * @param position         the position of the [Selectable] in the list which should be
     * [RecyclerView.NO_POSITION] if it is not known
     * @param <T>              type of selectable expected to be received and used over [IEngineConnection]
    </T> */
    open fun <T : Selectable?> informListeners(
        engineConnection: IEngineConnection<T?>?, selectable: T?,
        isSelected: Boolean, position: Int
    )

    /**
     * Inform all the [PerformerListener] objects after the [.check] call. Unlike that method, this doesn't have any ability to manipulate the task.
     *
     * @param engineConnection that is making the call
     * @param selectableList   item list that is being updated
     * @param isSelected       true when [Selectable] is being marked as selected
     * @param positions        the position array of the [Selectable] list which can be
     * [RecyclerView.NO_POSITION] individually
     * @param <T>              type of selectable expected to be received and used over [IEngineConnection]
    </T> */
    open fun <T : Selectable?> informListeners(
        engineConnection: IEngineConnection<T?>?, selectableList: MutableList<T?>?,
        isSelected: Boolean, positions: IntArray?
    )

    /**
     * Remove the connection from the list that is no longer needed.
     *
     * @param selectionConnection is the connection to be removed
     * @return true when the connection exists and removed
     */
    open fun removeSlot(selectionConnection: IBaseEngineConnection?): Boolean

    /**
     * Remove all the connection instances from the known connections list.
     */
    open fun removeSlots()

    /**
     * This is a call that is usually made by [IEngineConnection.setSelected] to notify the
     * [PerformerCallback] classes.
     *
     * @param engineConnection that is making the call
     * @param selectable       item that is being updated
     * @param isSelected       true when [Selectable] is being marked as selected
     * @param position         the position of the [Selectable] in the list which should be
     * [RecyclerView.NO_POSITION] if it is not known.
     * @param <T>              type of selectable expected to be received and used over [IEngineConnection]
    </T> */
    open fun <T : Selectable?> check(
        engineConnection: IEngineConnection<T?>?, selectable: T?, isSelected: Boolean,
        position: Int
    ): Boolean

    /**
     * This is a call that is usually made by [IEngineConnection.setSelected] to notify the
     * [PerformerCallback] classes.
     *
     * @param engineConnection that is making the call
     * @param selectableList   that is being updated
     * @param isSelected       true when the individual [Selectable] objects is intended to marked as selected
     * @param positions        the position array of the individual [Selectable] objects which should correspond
     * [RecyclerView.NO_POSITION] is not known
     * @param <T>              type of selectable expected to be received and used over [IEngineConnection]
     * @return
    </T> */
    open fun <T : Selectable?> check(
        engineConnection: IEngineConnection<T?>?, selectableList: MutableList<T?>?,
        isSelected: Boolean, positions: IntArray?
    ): Boolean

    /**
     * Compile the list of selectables that are held in the host of their owners, in other words, make a list of
     * selectables that are marked as selected from all connections. The problem is, though this is easier to
     * access each element, it isn't easy to refer to their owners after they are referred to as generic
     * [Selectable]. The better approach is to never mention them outside of their context.
     *
     * @return the compiled list
     */
    open fun getSelectionList(): MutableList<out Selectable?>?

    /**
     * If you need to individually refer to the list elements without losing their identity in the process, you can
     * use this method to access the each connection and make changes in their own context.
     *
     * @return a compiled list of connections
     */
    open fun getConnectionList(): MutableList<IBaseEngineConnection?>?

    /**
     * Add a listener to be called when something changes on the selection and manipulate it before completing the
     * process.
     *
     * @param callback to be called during the process
     * @return true when the callback already exists or added
     * @see .removePerformerCallback
     */
    open fun addPerformerCallback(callback: PerformerCallback?): Boolean

    /**
     * Add a listener to be called after something changes on the selection list.
     *
     * @param listener to be called.
     * @return true when the listener is added or on the list
     * @see .removePerformerListener
     */
    open fun addPerformerListener(listener: PerformerListener?): Boolean

    /**
     * Remove the previously added callback.
     *
     * @param callback to be removed
     * @return true when the callback was in the list and removed
     * @see .addPerformerCallback
     */
    open fun removePerformerCallback(callback: PerformerCallback?): Boolean

    /**
     * Remove a previously added listener from the list of listeners that are called when a selectable state changes.
     *
     * @param listener to be removed
     * @return true when the listener was on the list and removed
     * @see .addPerformerListener
     */
    open fun removePerformerListener(listener: PerformerListener?): Boolean
}