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

import java.util.*

class PerformerEngine : IPerformerEngine {
    private val connectionList: MutableList<IBaseEngineConnection> = ArrayList()

    private val performerCallbackList: MutableList<PerformerCallback> = ArrayList()

    private val performerListenerList: MutableList<PerformerListener> = ArrayList()

    override fun <T : Selectable> check(
        engineConnection: IEngineConnection<T>, selectable: T, isSelected: Boolean, position: Int,
    ): Boolean {
        synchronized(performerCallbackList) {
            for (callback in performerCallbackList) if (!callback.onSelection(
                    this,
                    engineConnection,
                    selectable,
                    isSelected,
                    position
                )
            ) return false
        }
        return true
    }

    override fun <T : Selectable> check(
        engineConnection: IEngineConnection<T>, selectableList: MutableList<T>,
        isSelected: Boolean, positions: IntArray,
    ): Boolean {
        synchronized(performerCallbackList) {
            for (callback in performerCallbackList) if (!callback.onSelection(
                    this,
                    engineConnection,
                    selectableList,
                    isSelected,
                    positions
                )
            ) return false
        }
        return true
    }

    override fun getSelectionList(): MutableList<out Selectable> {
        val selectableList: MutableList<Selectable> = ArrayList<Selectable>()
        synchronized(connectionList) {
            for (baseEngineConnection in connectionList)
                baseEngineConnection.getGenericSelectedItemList()?.let { selectableList.addAll(it) }
        }
        return selectableList
    }

    override fun getConnectionList(): MutableList<IBaseEngineConnection> {
        return ArrayList(connectionList)
    }

    override fun hasActiveSlots(): Boolean {
        return connectionList.size > 0
    }

    override fun ensureSlot(provider: PerformerEngineProvider, selectionConnection: IBaseEngineConnection): Boolean {
        synchronized(connectionList) {
            if (connectionList.contains(selectionConnection) || connectionList.add(selectionConnection)) {
                if (selectionConnection.getEngineProvider() !== provider) selectionConnection.setEngineProvider(provider)
                return true
            }
        }
        return false
    }

    override fun <T : Selectable> informListeners(
        engineConnection: IEngineConnection<T>, selectable: T,
        isSelected: Boolean, position: Int,
    ) {
        synchronized(performerListenerList) {
            for (listener in performerListenerList) listener.onSelected(
                this,
                engineConnection,
                selectable,
                isSelected,
                position
            )
        }
    }

    override fun <T : Selectable> informListeners(
        engineConnection: IEngineConnection<T>, selectableList: MutableList<T>,
        isSelected: Boolean, positions: IntArray,
    ) {
        synchronized(performerListenerList) {
            for (listener in performerListenerList) listener.onSelected(
                this,
                engineConnection,
                selectableList,
                isSelected,
                positions
            )
        }
    }

    override fun removeSlot(selectionConnection: IBaseEngineConnection): Boolean {
        synchronized(connectionList) { return connectionList.remove(selectionConnection) }
    }

    override fun removeSlots() {
        synchronized(connectionList) { connectionList.clear() }
    }

    override fun addPerformerCallback(callback: PerformerCallback): Boolean {
        synchronized(performerCallbackList) {
            return performerCallbackList.contains(callback) || performerCallbackList.add(callback)
        }
    }

    override fun addPerformerListener(listener: PerformerListener): Boolean {
        synchronized(performerListenerList) {
            return performerListenerList.contains(listener) || performerListenerList.add(listener)
        }
    }

    override fun removePerformerCallback(callback: PerformerCallback): Boolean {
        synchronized(performerCallbackList) { return performerCallbackList.remove(callback) }
    }

    override fun removePerformerListener(listener: PerformerListener): Boolean {
        synchronized(performerListenerList) { return performerListenerList.remove(listener) }
    }
}