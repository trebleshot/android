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
package com.genonbeta.TrebleShot.model

import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.SelectionModel

class MappedSelectionModel<T : SelectionModel>(
    var selectionModel: T, private val engineConnection: IEngineConnection<T>,
) : SelectionModel {
    override fun canSelect(): Boolean = selectionModel.canSelect()

    override fun name(): String = selectionModel.name()

    override fun selected(): Boolean = selectionModel.selected()

    override fun select(selected: Boolean) {
        engineConnection.setSelected(selectionModel, selected)
    }

    companion object {
        private fun <T : SelectionModel> addToMappedObjectList(
            list: MutableList<MappedSelectionModel<*>>,
            connection: IEngineConnection<T>,
        ) {
            val selectedItemList = connection.getSelectionList() ?: return
            for (selectionModel in selectedItemList) list.add(MappedSelectionModel(selectionModel, connection))
        }

        @JvmStatic
        fun compileFrom(engine: IPerformerEngine?): List<MappedSelectionModel<*>> {
            val list: MutableList<MappedSelectionModel<*>> = ArrayList()
            if (engine != null) for (baseEngineConnection in engine.getConnectionList()) {
                if (baseEngineConnection is IEngineConnection<*>) {
                    addToMappedObjectList(list, baseEngineConnection)
                }
            }
            return list
        }
    }
}