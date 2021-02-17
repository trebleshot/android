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

import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class EngineConnection<T : Selectable>(provider: PerformerEngineProvider, host: SelectableHost<T>) :
    IEngineConnection<T> {
    private var engineProvider: PerformerEngineProvider? = provider

    private var selectableProvider: SelectableProvider<T>? = null

    private var selectableHost: SelectableHost<T>? = host

    private var definitiveTitle: CharSequence? = null

    private val selectionListenerList: MutableList<IEngineConnection.SelectionListener<T>> = ArrayList()

    override fun addSelectionListener(listener: IEngineConnection.SelectionListener<T>): Boolean {
        synchronized(selectionListenerList) {
            return selectionListenerList.contains(listener) || selectionListenerList.add(listener)
        }
    }

    protected fun changeSelectionState(selectable: T, selected: Boolean, position: Int): Boolean {
        if (selected != selectable.isSelectableSelected() && selectable.setSelectableSelected(selected)) {
            val engine = getEngineProvider()?.getPerformerEngine()
            val host = getSelectableHost()

            host?.getSelectableList()?.let {
                if (selected) it.add(selectable) else it.remove(selectable)
            }

            if (engine != null) {
                for (listener in selectionListenerList)
                    listener.onSelected(
                        engine,
                        this,
                        selectable,
                        selected,
                        position
                    )
                engine.informListeners(this, selectable, selected, position)
            } else Log.d(TAG, "changeSelectionState: Engine is empty. Skipping the call for listeners!")
            return true
        }
        return false
    }

    protected fun changeSelectionState(selectableList: MutableList<T>, selected: Boolean, positions: IntArray) {
        val engine = getEngineProvider()?.getPerformerEngine()
        for (selectable in selectableList) {
            if (selected != selectable.isSelectableSelected() && selectable.setSelectableSelected(selected))
                if (selected) getSelectedItemList()?.add(selectable) else getSelectedItemList()?.remove(selectable)
        }
        if (engine != null) {
            for (listener in selectionListenerList)
                listener.onSelected(
                    engine,
                    this,
                    selectableList,
                    selected,
                    positions
                )
            engine.informListeners(this, selectableList, selected, positions)
        } else Log.d(TAG, "changeSelectionState: Engine is empty. Skipping the call for listeners!")
    }

    override fun getDefinitiveTitle(): CharSequence? {
        return definitiveTitle
    }

    override fun getEngineProvider(): PerformerEngineProvider? {
        return engineProvider
    }

    override fun getGenericSelectedItemList(): MutableList<out Selectable>? {
        return getSelectedItemList()
    }

    override fun getGenericAvailableList(): MutableList<out Selectable>? {
        return getAvailableList()
    }

    override fun getSelectedItemList(): MutableList<T>? {
        return getSelectableHost()?.getSelectableList()
    }

    override fun getAvailableList(): MutableList<T>? {
        return getSelectableProvider()?.getSelectableList()
    }

    override fun getSelectableHost(): SelectableHost<T>? {
        return selectableHost
    }

    override fun getSelectableProvider(): SelectableProvider<T>? {
        return selectableProvider
    }

    override fun isSelectedOnHost(selectable: T): Boolean {
        return getSelectedItemList()?.contains(selectable) == true
    }

    override fun removeSelectionListener(listener: IEngineConnection.SelectionListener<T>): Boolean {
        synchronized(selectionListenerList) { return selectionListenerList.remove(listener) }
    }

    override fun setDefinitiveTitle(title: CharSequence?) {
        definitiveTitle = title
    }

    override fun setEngineProvider(provider: PerformerEngineProvider?) {
        engineProvider = provider
    }

    override fun setSelectableHost(host: SelectableHost<T>?) {
        selectableHost = host
    }

    override fun setSelectableProvider(provider: SelectableProvider<T>?) {
        selectableProvider = provider
    }

    @Throws(SelectableNotFoundException::class, CouldNotAlterException::class)
    override fun setSelected(holder: RecyclerView.ViewHolder): Boolean {
        return setSelected(holder.adapterPosition)
    }

    @Throws(SelectableNotFoundException::class, CouldNotAlterException::class)
    override fun setSelected(position: Int): Boolean {
        return try {
            getSelectableProvider()?.let {
                setSelected(it.getSelectableList()[position], position)
            } ?: false
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw SelectableNotFoundException("The selectable at the given position $position could not be found. ")
        }
    }

    @Throws(CouldNotAlterException::class)
    override fun setSelected(selectable: T): Boolean {
        return setSelected(selectable, RecyclerView.NO_POSITION)
    }

    override fun setSelected(selectable: T, selected: Boolean): Boolean {
        return setSelected(selectable, RecyclerView.NO_POSITION, selected)
    }

    @Throws(CouldNotAlterException::class)
    override fun setSelected(selectable: T, position: Int): Boolean {
        val newState = !isSelectedOnHost(selectable)
        if (!setSelected(selectable, position, newState, true)) throw CouldNotAlterException(
            "The selectable " + selectable + " state couldn't be altered. The " +
                    "reason may be that the engine was not available or selectable was not allowed to alter state"
        )
        return newState
    }

    override fun setSelected(selectable: T, position: Int, selected: Boolean): Boolean {
        return setSelected(selectable, position, selected, false)
    }

    override fun setSelected(selectableList: MutableList<T>, positions: IntArray, selected: Boolean): Boolean {
        val engine = getEngineProvider()?.getPerformerEngine()
        if (engine != null && engine.check(this, selectableList, selected, positions)) {
            changeSelectionState(selectableList, selected, positions)
            return true
        }
        return false
    }

    private fun setSelected(selectable: T, position: Int, selected: Boolean, checked: Boolean): Boolean {
        // Check if it is already the same.
        if (!checked && selected == isSelectedOnHost(selectable)) {
            if (selectable.isSelectableSelected() != selected && !selectable.setSelectableSelected(selected)) {
                // Selectable state state should change but reports a failure to do so.
                getSelectedItemList()?.remove(selectable)
                return false
            }
            return selected
        }
        val performerEngine = getEngineProvider()?.getPerformerEngine()
        return (performerEngine != null && performerEngine.check(this, selectable, selected, position)
                && changeSelectionState(selectable, selected, position))
    }

    companion object {
        val TAG: String = EngineConnection::class.java.simpleName
    }
}