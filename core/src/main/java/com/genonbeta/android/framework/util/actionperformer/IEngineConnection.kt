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
 * This class takes care of connecting [IPerformerEngine] to the UI element that needs to be free of limitations
 * like knowing whether the [T] is something that it can work on. It does that by extending from
 * [IBaseEngineConnection]. Also note that the term "connection" is used loosely and doesn't mean that there is an
 * IPC connection or whatsoever.
 *
 * @param <T> The derivative of the [Selectable] class
</T> */
interface IEngineConnection<T : Selectable?> : IBaseEngineConnection {
    /**
     * Add a listener that will only be called by this specific connection or more connections with same T parameter.
     *
     * @param listener to be called when the selection state of a selectable changes
     * @return true when the listener is added or already exist
     */
    open fun addSelectionListener(listener: SelectionListener<T?>?): Boolean

    /**
     * @return a quick call of [SelectableHost.getSelectableList]
     * @see .getSelectableHost
     */
    open fun getSelectedItemList(): MutableList<T?>?

    /**
     * @return a quick call of [SelectableProvider.getSelectableList]
     * @see .getSelectableProvider
     */
    open fun getAvailableList(): MutableList<T?>?

    /**
     * @return the host that holds the selected objects
     * @see SelectableHost
     */
    open fun getSelectableHost(): SelectableHost<T?>?

    /**
     * @return the provider that is used to identify the selectable object
     * @see SelectableProvider
     */
    open fun getSelectableProvider(): SelectableProvider<T?>?

    /**
     * Ensure that the given selectable object is stored in [SelectableHost]
     *
     * @param selectable that needs to be checked whether it is stored
     * @return true when it exists in the host's list
     */
    open fun isSelectedOnHost(selectable: T?): Boolean

    /**
     * Remove the previously added listener.
     *
     * @param listener to be removed
     * @return true when the listened was exist and now removed
     */
    open fun removeSelectionListener(listener: SelectionListener<T?>?): Boolean

    /**
     * @param host to hold the items marked as selected
     */
    open fun setSelectableHost(host: SelectableHost<T?>?)

    /**
     * @param provider that gives the items
     * @see .getSelectableProvider
     */
    open fun setSelectableProvider(provider: SelectableProvider<T?>?)

    /**
     * Alter the state of the selectable without specifying its location in [.getAvailableList]. Even
     * though it shouldn't be important to have the position, it may later be required to be used with
     * [IPerformerEngine.check]. Also because the new state is not
     * specified, it will be the opposite what it previously was.
     *
     * @return true if the given selectable is selected
     * @throws CouldNotAlterException when the call fails to complete for some reason (see error msg for details)
     * @see .setSelected
     */
    @Throws(CouldNotAlterException::class)
    open fun setSelected(selectable: T?): Boolean

    /**
     * Apart from [.setSelected], this does not make decision on the new state.
     *
     * @see .setSelected
     */
    open fun setSelected(selectable: T?, selected: Boolean): Boolean

    /**
     * The same as [.setSelected], but this time the position is also provided. Also, because,
     * the new state will be based upon the old state, the methods that don't take the new state as a parameter will
     * return the new state instead of the result of the call. The result of the call can still be determined by using
     * try-catch blocks.
     *
     * @param selectable to be altered
     * @return true if the given selectable is selected
     * @throws CouldNotAlterException when the call fails to complete for some reason (see error msg for details)
     * @see .setSelected
     */
    @Throws(CouldNotAlterException::class)
    open fun setSelected(selectable: T?, position: Int): Boolean

    /**
     * Mark the given selectable with the given state 'selected'. If it is already in that state
     * return true and don't call [IPerformerEngine.check].
     * The return value reflects if the call is successful, not the selection state.
     *
     * @param selectable to be altered
     * @param position   where the selectable is located in [.getAvailableList] which can also be
     * [RecyclerView.NO_POSITION] if it is not known
     * @param selected   is the new state
     * @return true if the new state is applied or was already the same
     */
    open fun setSelected(selectable: T?, position: Int, selected: Boolean): Boolean

    /**
     * Mark all the selectables in the list. This method call has the characteristics similar to [.setSelected] with only difference being this works on more than one selectable at a time. The
     * listeners for individual items won't be invoked. You should only wait for [SelectionListener.onSelected] to be invoked.
     *
     * @param selectableList to be altered
     * @param positions      where the selectables are located in [.getAvailableList] and which has the same size as
     * 'selectableList' parameter
     * @param selected       is the new state
     * @return true when, other than selectable rejecting to alter state, everything is okay
     */
    open fun setSelected(selectableList: MutableList<T?>?, positions: IntArray?, selected: Boolean): Boolean

    /**
     * This is only called by the [IEngineConnection] owning it. The idea here is that you want to update
     * the UI according to changes made on the connection, but don't want to be warned when connections unrelated to
     * what you are dealing with changes as it happens with [PerformerListener] on [IPerformerEngine].
     *
     * @param <T> type that this listener will be called from
    </T> */
    interface SelectionListener<T : Selectable?> {
        /**
         * When an individual [Selectable] has been changed, this is called.
         *
         * @param engine     that is holding an instance of this class
         * @param owner      is the connection that is making the call
         * @param selectable is the [Selectable] whose state has been changed
         * @param isSelected is the new state that has been set
         * @param position   is where the [Selectable] is positioned in
         * [SelectableProvider.getSelectableList]
         */
        open fun onSelected(
            engine: IPerformerEngine?, owner: IEngineConnection<T?>?, selectable: T?, isSelected: Boolean,
            position: Int
        )

        /**
         * When a list of [Selectable]s have been changed, this is called.
         *
         * @param engine         that is holding an instance of this class
         * @param owner          is the connection that is making the call
         * @param selectableList is the list of [Selectable]s whose states have been changed
         * @param isSelected     is the new state that has been set
         * @param positions      are where the [Selectable]s are positioned in
         * [SelectableProvider.getSelectableList]
         */
        open fun onSelected(
            engine: IPerformerEngine?,
            owner: IEngineConnection<T?>?,
            selectableList: MutableList<T?>?,
            isSelected: Boolean,
            positions: IntArray?
        )
    }
}