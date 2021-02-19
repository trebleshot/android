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
 * A UI-related class that handles [IEngineConnection] and [PerformerCallback] to help them communicate with
 * the UI and each other.
 *
 * @see PerformerEngine as an implementation example
 */
interface IPerformerEngine {
    /**
     * Invoked when we want to ensure that there is at least one [IBaseEngineConnection] on any slot.
     *
     * @return True when there is at least one.
     */
    fun hasActiveSlots(): Boolean

    /**
     * Ensure that the related connection is known and has an active slot in the list of connections.
     *
     * @param selectionConnection That should have an active connection.
     * @return True if there is already a connection or added a new one.
     */
    fun ensureSlot(provider: PerformerEngineProvider, selectionConnection: IBaseEngineConnection): Boolean

    /**
     * Inform all the [PerformerListener] objects after the [check] call.
     *
     * Unlike that method, this doesn't have any ability to manipulate the task.
     *
     * @param engineConnection That is making the call.
     * @param model       Item that is being updated.
     * @param isSelected       True when [SelectionModel] is being marked as selected.
     * @param position         Of the [SelectionModel] which will be [RecyclerView.NO_POSITION] if not yet known.
     * @param T                Type of model expected to be received and used with [IEngineConnection].
     */
    fun <T : SelectionModel> informListeners(
        engineConnection: IEngineConnection<T>, model: T, isSelected: Boolean, position: Int,
    )

    /**
     * Inform all the [PerformerListener] objects after the [check] call.
     *
     * Unlike that method, this doesn't have any ability to manipulate the task.
     *
     * @param engineConnection That is making the call.
     * @param modelList   Of items that are being updated.
     * @param isSelected       True when being marked as selected.
     * @param positions        Of the [SelectionModel] list.
     * @param T                Type of model expected to be received and used over [IEngineConnection].
     */
    fun <T : SelectionModel> informListeners(
        engineConnection: IEngineConnection<T>,
        modelList: MutableList<T>,
        isSelected: Boolean,
        positions: IntArray,
    )

    /**
     * Remove the given connection from the list.
     *
     * @param selectionConnection To be removed.
     * @return True when the connection existed and removed.
     */
    fun removeSlot(selectionConnection: IBaseEngineConnection): Boolean

    /**
     * Remove all the connection instances from the known connections list.
     */
    fun removeSlots()

    /**
     * Usually invoked by [IEngineConnection.setSelected] to notify the [PerformerCallback] instances.
     *
     * @param engineConnection That is making the call.
     * @param model       That is being updated.
     * @param isSelected       True when [SelectionModel] is being marked as selected.
     * @param position         Of the [SelectionModel] which will be [RecyclerView.NO_POSITION] if not yet known.
     * @param T                Type of model expected to be received and used with [IEngineConnection].
     * @return True if all the listeners agreed to perform the selection operation.
     */
    fun <T : SelectionModel> check(
        engineConnection: IEngineConnection<T>, model: T, isSelected: Boolean, position: Int,
    ): Boolean

    /**
     * This is a call that is usually made by [IEngineConnection.setSelected] to notify the [PerformerCallback] classes.
     *
     * @param engineConnection That is making the call.
     * @param modelList   Of items that are being updated.
     * @param isSelected       True when the individual [SelectionModel] objects is intended to be marked as selected.
     * @param positions        Of the individual [SelectionModel] objects.
     * @param T                Type of model expected to be received and used with [IEngineConnection].
     * @return True if all the listeners agreed to perform the selection operation.
     */
    fun <T : SelectionModel> check(
        engineConnection: IEngineConnection<T>,
        modelList: MutableList<T>,
        isSelected: Boolean,
        positions: IntArray,
    ): Boolean

    /**
     * Compile the list of models that are held in the host of their owners, in other words, make a list of
     * models that are marked as selected from all connections. The problem is, though it is easier to
     * access each element, it isn't easy to refer to their owners after they are referred to as generic
     * [SelectionModel]. A better approach is to never mention them outside of their context.
     *
     * @return The compiled list of models that are selected.
     */
    fun getSelectionList(): MutableList<out SelectionModel>

    /**
     * If you need to individually refer to the list elements without losing their identity in the process, you can
     * use this method to access the each connection and make changes in their own context.
     *
     * @return The connection list.
     */
    fun getConnectionList(): MutableList<IBaseEngineConnection>

    /**
     * Add a callback that is able to manipulate the selection process.
     *
     * @param callback To be called during a selection process.
     * @return True when the callback has been added or is already exists.
     * @see removePerformerCallback
     */
    fun addPerformerCallback(callback: PerformerCallback): Boolean

    /**
     * Add a listener informed about selection operations.
     *
     * @param listener To be informed.
     * @return True if successful or already exists.
     * @see removePerformerListener
     */
    fun addPerformerListener(listener: PerformerListener): Boolean

    /**
     * Remove a previously added callback.
     *
     * @param callback To remove.
     * @return True when the callback was in the list and now removed.
     * @see addPerformerCallback
     */
    fun removePerformerCallback(callback: PerformerCallback): Boolean

    /**
     * Remove a previously added listener.
     *
     * @param listener To remove.
     * @return True when the listener was on the list and removed.
     * @see addPerformerListener
     */
    fun removePerformerListener(listener: PerformerListener): Boolean
}