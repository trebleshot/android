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
 * If you want to be informed when [IEngineConnection.setSelected] is invoked, you can do so with this listener.
 * Unlike [PerformerCallback], this doesn't have the ability to manipulate the process. This is informed after
 * the process is done.
 */
interface PerformerListener {
    /**
     * Invoked after the state of a [Selectable] has changed.
     *
     * @param engine     Holding an instance of this class.
     * @param owner      That is making the call.
     * @param selectable Whose state has been changed.
     * @param isSelected The new state to be set.
     * @param position   Of the [Selectable] in [SelectableProvider.getSelectableList].
     */
    fun onSelected(
        engine: IPerformerEngine, owner: IBaseEngineConnection, selectable: Selectable, isSelected: Boolean,
        position: Int,
    )

    /**
     * This method is called after a list of [Selectable]s has changed.
     *
     * @param engine     Holding an instance of this class.
     * @param owner      That is making the call.
     * @param selectableList The list of [Selectable]s that are being altered.
     * @param isSelected     The new state to be set.
     * @param positions     Of the [Selectable]s in [SelectableProvider.getSelectableList].
     */
    fun onSelected(
        engine: IPerformerEngine, owner: IBaseEngineConnection, selectableList: MutableList<out Selectable>,
        isSelected: Boolean, positions: IntArray,
    )
}