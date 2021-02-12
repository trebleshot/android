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

import androidx.recyclerview.widget.RecyclerView

/**
 * The base class for engine connection. This is free of generics for most compatibility.
 */
interface IBaseEngineConnection {
    /**
     * Compile the list of available items
     *
     * @return the list that is available with [SelectableProvider]
     */
    fun getGenericAvailableList(): MutableList<out Selectable>?

    /**
     * Compile the list of selected items.
     *
     * @return the list that is available with [SelectableHost]
     */
    fun getGenericSelectedItemList(): MutableList<out Selectable>?

    /**
     * Titles are only for helping the end-user know the connection in a UX manner.
     *
     * @return the representing title
     */
    fun getDefinitiveTitle(): CharSequence?

    /**
     * Set the engine provider usually an [android.app.Activity] implementing it.
     *
     * @param engineProvider the provider
     */
    fun setEngineProvider(engineProvider: PerformerEngineProvider?)

    /**
     * @return the provider that will supply us with [IPerformerEngine] that we will operate on.
     */
    fun getEngineProvider(): PerformerEngineProvider?

    /**
     * Set the title that may be used by UI elements that need to identify this connection for user.
     *
     * @param title use to identify this connection
     */
    fun setDefinitiveTitle(title: CharSequence?)

    /**
     * Find the selectable using [RecyclerView.ViewHolder.getAdapterPosition] and toggle its selection state.
     *
     * @param holder that we will use to find the location
     * @return true if the given selectable is selected
     * @throws SelectableNotFoundException when the given position with the holder doesn't point to a selectable
     */
    @Throws(SelectableNotFoundException::class, CouldNotAlterException::class)
    fun setSelected(holder: RecyclerView.ViewHolder): Boolean

    /**
     * Find the selectable in the list that is made available by [SelectableProvider]
     *
     * @throws SelectableNotFoundException when the the given position doesn't point to a selectable
     * @throws CouldNotAlterException      when the call fails to complete for some reason (see error msg for details)
     */
    @Throws(SelectableNotFoundException::class, CouldNotAlterException::class)
    fun setSelected(position: Int): Boolean
}