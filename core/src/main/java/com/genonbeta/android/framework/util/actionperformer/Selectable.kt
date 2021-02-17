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
 * date: 5.01.2018 10:58
 *
 * @see IPerformerEngine
 * @see IEngineConnection
 * @author Veli Tasalı
 */
interface Selectable {
    /**
     * This title is intended for UI purposes.
     *
     * @return The human-readable title for this selectable.
     */
    fun getSelectableTitle(): String

    /**
     * The current state of this selectable.
     *
     * @return True if marked as selected.
     */
    fun isSelectableSelected(): Boolean

    /**
     * Invoked when this state of this selectable needs to be altered by an [IEngineConnection] instance.
     *
     * The direct invocation is not recommended since the list may be refreshed from database and the state may be lost
     * unless one of the [IEngineConnection.setSelected] methods are used.
     *
     * @param selected True if the item should be marked as selected or false if otherwise.
     * @return True if this selectable instance allows altering its selection state.
     * @see IPerformerEngine.check
     */
    fun setSelectableSelected(selected: Boolean): Boolean
}