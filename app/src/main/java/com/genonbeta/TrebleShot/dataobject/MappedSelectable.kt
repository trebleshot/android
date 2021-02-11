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
package com.genonbeta.TrebleShot.dataobject

import com.genonbeta.android.framework.`object`.Selectable
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine

class MappedSelectable<T : Selectable>(var selectable: T, val engineConnection: IEngineConnection<T>) : Selectable {
    val selectableTitle: String
        get() = selectable.getSelectableTitle()

    val isSelectableSelected: Boolean
        get() = selectable.isSelectableSelected()

    override fun setSelectableSelected(selected: Boolean): Boolean {
        return engineConnection.setSelected(selectable, selected)
    }

    companion object {
        private fun <T : Selectable> addToMappedObjectList(
            list: MutableList<MappedSelectable<*>>,
            connection: IEngineConnection<T>
        ) {
            val selectedItemList = connection.getSelectedItemList() ?: return
            for (selectable in selectedItemList) list.add(MappedSelectable(selectable, connection))
        }

        @JvmStatic
        fun compileFrom(engine: IPerformerEngine?): List<MappedSelectable<*>> {
            val list: MutableList<MappedSelectable<*>> = ArrayList()
            if (engine != null) for (baseEngineConnection in engine.getConnectionList()) {
                if (baseEngineConnection is IEngineConnection<*>) addToMappedObjectList(
                    list,
                    baseEngineConnection
                )
            }
            return list
        }
    }
}