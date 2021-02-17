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
 * The base class for engine connection. Items are kept as generics for compatibility.
 */
interface IBaseEngineConnection {
    /**
     * Compile the list of available items.
     *
     * @return The list that is available within [SelectableProvider].
     */
    fun getGenericAvailableList(): MutableList<out Selectable>?

    /**
     * Compile the list of selected items.
     *
     * @return The list that is available within [SelectableHost].
     */
    fun getGenericSelectedItemList(): MutableList<out Selectable>?

    /**
     * The human-readable title for this connection.
     *
     * @return The title representing this connection.
     */
    fun getDefinitiveTitle(): CharSequence?

    /**
     * Sets the engine provider.
     *
     * @param engineProvider That will provide the [IPerformerEngine] implementation.
     * @see [getEngineProvider]
     */
    fun setEngineProvider(provider: PerformerEngineProvider?)

    /**
     * Queries the [IPerformerEngine] provider.
     *
     * @return The provider.
     * @see [setEngineProvider]
     */
    fun getEngineProvider(): PerformerEngineProvider?

    /**
     * Sets the human-readable title for this connection.
     *
     * @param title Used to identify this connection.
     */
    fun setDefinitiveTitle(title: CharSequence?)

    /**
     * Find the selectable using [RecyclerView.ViewHolder.getAdapterPosition] and toggle its selection state.
     *
     * @param holder That will provide the selectable position.
     * @return True if operation was successful.
     * @throws SelectableNotFoundException When the given position does not point to a selectable.
     */
    @Throws(SelectableNotFoundException::class, CouldNotAlterException::class)
    fun setSelected(holder: RecyclerView.ViewHolder): Boolean

    /**
     * Find the selectable in the list that is made available by [SelectableProvider].
     *
     * @throws SelectableNotFoundException When the given position does not point to a selectable.
     * @throws CouldNotAlterException      If the selectable could not altered.
     */
    @Throws(SelectableNotFoundException::class, CouldNotAlterException::class)
    fun setSelected(position: Int): Boolean
}