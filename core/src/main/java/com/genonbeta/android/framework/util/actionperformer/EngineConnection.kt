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

class EngineConnection<T : SelectionModel>(provider: PerformerEngineProvider, host: SelectionHost<T>) :
    IEngineConnection<T> {
    private var engineProvider: PerformerEngineProvider? = provider

    private var selectionModelProvider: SelectionModelProvider<T>? = null

    private var selectionHost: SelectionHost<T>? = host

    private var definitiveTitle: CharSequence? = null

    private val selectionListenerList: MutableList<IEngineConnection.SelectionListener<T>> = ArrayList()

    override fun addSelectionListener(listener: IEngineConnection.SelectionListener<T>): Boolean {
        synchronized(selectionListenerList) {
            return selectionListenerList.contains(listener) || selectionListenerList.add(listener)
        }
    }

    protected fun changeSelectionState(selectionModel: T, selected: Boolean, position: Int): Boolean {
        if (selected != selectionModel.selected() && selectionModel.canSelect()) {
            selectionModel.select(selected)

            val engine = getEngineProvider()?.getPerformerEngine()
            val selectionList = getSelectionList()

            if (selectionList != null) {
                val value = if (selected) selectionList.add(selectionModel) else selectionList.remove(selectionModel)
                Log.d(TAG, "changeSelectionState: Added? $value ${selectionList.hashCode()}")
            }

            if (engine != null) {
                for (listener in selectionListenerList) listener.onSelected(
                    engine, this, selectionModel, selected, position
                )
                engine.informListeners(this, selectionModel, selected, position)
            } else Log.d(TAG, "changeSelectionState: Engine is empty. Skipping listener invocation!")
            return true
        }
        return false
    }

    protected fun changeSelectionState(modelList: MutableList<T>, selected: Boolean, positions: IntArray) {
        val engine = getEngineProvider()?.getPerformerEngine()
        for (selectionModel in modelList) {
            if (selected != selectionModel.selected() && selectionModel.canSelect()) {
                selectionModel.select(selected)
                if (selected) getSelectionList()?.add(selectionModel) else getSelectionList()?.remove(selectionModel)
            }
        }
        if (engine != null) {
            for (listener in selectionListenerList)
                listener.onSelected(
                    engine,
                    this,
                    modelList,
                    selected,
                    positions
                )
            engine.informListeners(this, modelList, selected, positions)
        } else Log.d(TAG, "changeSelectionState: Engine is empty. Skipping the call for listeners!")
    }

    override fun getDefinitiveTitle(): CharSequence? {
        return definitiveTitle
    }

    override fun getEngineProvider(): PerformerEngineProvider? {
        return engineProvider
    }

    override fun getGenericSelectedList(): MutableList<out SelectionModel>? {
        return getSelectionList()
    }

    override fun getGenericAvailableList(): MutableList<out SelectionModel>? {
        return getAvailableList()
    }

    override fun getSelectionList(): MutableList<T>? {
        return getSelectableHost()?.getSelectionList()
    }

    override fun getAvailableList(): MutableList<T>? {
        return getSelectableProvider()?.getAvailableList()
    }

    override fun getSelectableHost(): SelectionHost<T>? {
        return selectionHost
    }

    override fun getSelectableProvider(): SelectionModelProvider<T>? {
        return selectionModelProvider
    }

    override fun isSelectedOnHost(model: T): Boolean {
        return getSelectionList()?.contains(model) == true
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

    override fun setSelectionHost(host: SelectionHost<T>?) {
        selectionHost = host
    }

    override fun setSelectionModelProvider(provider: SelectionModelProvider<T>?) {
        selectionModelProvider = provider
    }

    @Throws(SelectionModelNotFoundException::class, CouldNotAlterException::class)
    override fun setSelected(holder: RecyclerView.ViewHolder): Boolean {
        return setSelected(holder.adapterPosition)
    }

    @Throws(SelectionModelNotFoundException::class, CouldNotAlterException::class)
    override fun setSelected(position: Int): Boolean {
        return try {
            val item = getAvailableList()?.get(position) ?: return false
            setSelected(item, position)
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw SelectionModelNotFoundException("The model at the given position $position could not be found. ")
        } finally {
            Log.d("engineConnection", "Has items selected ${getSelectionList()}")
        }
    }

    @Throws(CouldNotAlterException::class)
    override fun setSelected(model: T): Boolean {
        return setSelected(model, RecyclerView.NO_POSITION)
    }

    override fun setSelected(model: T, selected: Boolean): Boolean {
        return setSelected(model, RecyclerView.NO_POSITION, selected)
    }

    @Throws(CouldNotAlterException::class)
    override fun setSelected(model: T, position: Int): Boolean {
        val newState = !isSelectedOnHost(model)
        if (!setSelected(model, position, newState, true)) throw CouldNotAlterException(
            "The model " + model + " state couldn't be altered. The " +
                    "reason may be that the engine was not available or selectable was not allowed to alter state"
        )
        return newState
    }

    override fun setSelected(model: T, position: Int, selected: Boolean): Boolean {
        return setSelected(model, position, selected, false)
    }

    override fun setSelected(modelList: MutableList<T>, positions: IntArray, selected: Boolean): Boolean {
        val engine = getEngineProvider()?.getPerformerEngine()
        if (engine != null && engine.check(this, modelList, selected, positions)) {
            changeSelectionState(modelList, selected, positions)
            return true
        }
        return false
    }

    private fun setSelected(selectionModel: T, position: Int, selected: Boolean, checked: Boolean): Boolean {
        // Check if it is already the same.
        if (!checked && selected == isSelectedOnHost(selectionModel)) {
            // If the states doesn't match, try to alter it and if that fails, remove the model from the host.
            if (selectionModel.selected() != selected
                && selectionModel.also { it.select(selected) }.selected() != selected
            ) {
                // Selectable state state should change but reports a failure to do so.
                getSelectionList()?.remove(selectionModel)
                return false
            }
            return selected
        }
        val performerEngine = getEngineProvider()?.getPerformerEngine()
        return (performerEngine != null
                && performerEngine.check(this, selectionModel, selected, position)
                && changeSelectionState(selectionModel, selected, position))
    }

    companion object {
        val TAG: String = EngineConnection::class.java.simpleName
    }
}