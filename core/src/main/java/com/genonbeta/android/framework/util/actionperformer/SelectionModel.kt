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

import com.genonbeta.android.framework.model.BaseModel

/**
 * date: 5.01.2018 10:58
 *
 * @see IPerformerEngine
 * @see IEngineConnection
 * @author Veli Tasalı
 */
interface SelectionModel : BaseModel {
    /**
     * Ensures whether this model accepts selection.
     *
     * This method is in-place because the selection engine may need to accept non-selectable objects at times.
     *
     * @return True if this model accepts selection.
     */
    fun canSelect(): Boolean

    /**
     * The current state of this selection model.
     *
     * @return True if marked as selected.
     */
    fun selected(): Boolean

    /**
     * Invoked when this state of this selection model needs to be altered by an [IEngineConnection] instance.
     *
     * The direct invocation is not recommended since the list may be refreshed from database and the state may be lost
     * unless one of the [IEngineConnection.setSelected] methods are used.
     *
     * @param selected True if the item should be marked as selected or false if otherwise.
     * @see IPerformerEngine.check
     */
    fun select(selected: Boolean)
}