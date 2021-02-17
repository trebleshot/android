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

/**
 * When [IPerformerEngine] is finally notified by [IEngineConnection], it may call one or more
 * [PerformerCallback] instances to manipulate the given selection process by allowing or not allowing an
 * item to be selected.
 */
interface PerformerCallback {
    /**
     * Invoked when the selection state of a selectable is about to change. By returning false, it can be stopped from
     * happening. Notice that it shouldn't mean that returning true is enough, which means if any other listener returns
     * false, they will override and cancel the task.
     *
     * @param engine     That is holding an instance of this class.
     * @param owner      That is making the call to alter the selection state of the [Selectable].
     * @param selectable Whose state is being altered.
     * @param isSelected To be set.
     * @param position   Of the [Selectable] in [SelectableProvider.getSelectableList].
     * @return True to approve the change of the change of the state.
     */
    fun onSelection(
        engine: IPerformerEngine, owner: IBaseEngineConnection, selectable: Selectable, isSelected: Boolean,
        position: Int,
    ): Boolean

    /**
     * Invoked when the selection state of a selectable is about to change. By returning false, it can be stopped from
     * happening. Notice that it shouldn't mean that returning true is enough, which means if any other listener returns
     * false, they will override and cancel the task.
     *
     * @param engine     That is holding an instance of this class.
     * @param owner      That is making the call to alter the selection state of the [Selectable].
     * @param selectableList Whose states are being altered.
     * @param isSelected     To be set.
     * @param positions      Of the [Selectable]s in [SelectableProvider.getSelectableList].
     * @return True to approve the change of the change of the states.
     */
    fun onSelection(
        engine: IPerformerEngine, owner: IBaseEngineConnection, selectableList: MutableList<out Selectable>,
        isSelected: Boolean, positions: IntArray,
    ): Boolean
}