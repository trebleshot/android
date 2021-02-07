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

import com.genonbeta.android.framework.`object`.Selectable

/**
 * When [IPerformerEngine] is finally notified by [IEngineConnection], it may call one or more
 * [PerformerCallback] instances to manipulate the given selection process by allowing or not allowing an
 * item to be selected.
 */
interface PerformerCallback {
    /**
     * This method is called when the selection state of a selectable is about to change. By returning false, it can
     * be stopped from happening. Notice that it shouldn't mean that returning true is enough, which means if any
     * other listener returns false, they will override and cancel the task.
     *
     * @param engine     that is holding an instance of this class
     * @param owner      is the connection that is making the call to alter the selection state of the
     * [Selectable]
     * @param selectable is the [Selectable] whose state is being changed
     * @param isSelected is the new state that is about to be set
     * @param position   is where the [Selectable] is positioned in
     * [SelectableProvider.getSelectableList]
     * @return true when the state of param selectable can be changed
     */
    fun onSelection(
        engine: IPerformerEngine, owner: IBaseEngineConnection, selectable: Selectable, isSelected: Boolean,
        position: Int,
    ): Boolean

    /**
     * This method is called when the selection state of a selectable is about to change. By returning false, it can
     * be stopped from happening. Notice that it shouldn't mean that returning true is enough, which means if any
     * other listener returns false, they will override and cancel the task.
     *
     * @param engine         that is holding an instance of this class
     * @param owner          is the connection that is making the call to alter the selection state of the
     * [Selectable]
     * @param selectableList is the list of [Selectable]s whose states are being changed
     * @param isSelected     is the new state that is about to be set
     * @param positions      are where the [Selectable]s are positioned in
     * [SelectableProvider.getSelectableList]
     * @return true when you approve the new changes
     */
    fun onSelection(
        engine: IPerformerEngine, owner: IBaseEngineConnection, selectableList: MutableList<out Selectable>,
        isSelected: Boolean, positions: IntArray,
    ): Boolean
}