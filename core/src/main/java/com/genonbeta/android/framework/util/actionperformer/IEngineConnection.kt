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
 * This class takes care of connecting [IPerformerEngine] to an UI element.
 *
 * UI element showing the selection operation doesn't need to know what [T] is other than it is a model.
 *
 * The term "connection" is used loosely and doesn't mean that there is an IPC connection or similar.
 *
 * @param T The derivative of the [SelectionModel] class.
 */
interface IEngineConnection<T : SelectionModel> : IBaseEngineConnection {
    /**
     * Add a listener that will only be called by this specific connection or more connections with same T parameter.
     *
     * @param listener To be called when the selection state of a model changes.
     * @return True when the listener is added or already exist.
     */
    fun addSelectionListener(listener: SelectionListener<T>): Boolean

    /**
     * Queries the selected item list.
     *
     * @return A shortcut to [SelectionHost.getSelectionList].
     * @see [getSelectableHost]
     */
    fun getSelectionList(): MutableList<T>?

    /**
     * Queries the items that are available for selection.
     *
     * @return A shortcut to [SelectionModelProvider.getAvailableList].
     * @see [getSelectableProvider]
     */
    fun getAvailableList(): MutableList<T>?

    /**
     * The host that keeps the model items that are marked as selected.
     *
     * @return The host.
     * @see SelectionHost
     */
    fun getSelectableHost(): SelectionHost<T>?

    /**
     * The provider of the items available for selection.
     *
     * @return The provider.
     * @see SelectionModelProvider
     */
    fun getSelectableProvider(): SelectionModelProvider<T>?

    /**
     * Ensure that the given model object is stored in [SelectionHost].
     *
     * @param model To check.
     * @return True if the model is on the host.
     */
    fun isSelectedOnHost(model: T): Boolean

    /**
     * Remove a previously added listener.
     *
     * @param listener To remove.
     * @return True when the listener was on the list and now removed.
     */
    fun removeSelectionListener(listener: SelectionListener<T>): Boolean

    /**
     * Sets the selection host that keeps the model items that are marked as selected.
     *
     * @param host That keeps the selected items.
     */
    fun setSelectionHost(host: SelectionHost<T>?)

    /**
     * Sets the provider for available items.
     *
     * @param provider That provides the items available for selection.
     * @see getSelectableProvider
     */
    fun setSelectionModelProvider(provider: SelectionModelProvider<T>?)

    /**
     * Alter the state of the model without specifying its location in [getAvailableList].
     *
     * Even though it shouldn't be important to have the position, it may later be required to use with
     * [IPerformerEngine.check].
     *
     * Also, because the new state is not specified, it will be the opposite what it previously was.
     *
     * @return True when the state of the given model has been successfully altered.
     * @throws CouldNotAlterException If selection state could not be altered.
     */
    @Throws(CouldNotAlterException::class)
    fun setSelected(model: T): Boolean

    /**
     * Alter the state of the models without specifying its location in [getAvailableList].
     *
     * Even though it shouldn't be important to have the position, it may later be required to use with
     * [IPerformerEngine.check].
     *
     * @return True when the state of the given model has been successfully altered.
     */
    fun setSelected(model: T, selected: Boolean): Boolean

    /**
     * Change the selection state of a model.
     *
     * The position will help avoid looking for the position of the item in the list.
     *
     * The new state will be the opposite of what it previously was.
     *
     * @param model To alter.
     * @return True when the state of the given model has been successfully altered.
     * @throws CouldNotAlterException If selection state could not be altered.
     */
    @Throws(CouldNotAlterException::class)
    fun setSelected(model: T, position: Int): Boolean

    /**
     * Mark the given model with the given state 'selected'.
     *
     * If it is already in that state return true and don't call [IPerformerEngine.check].
     *
     * @param model To alter
     * @param position   Of the model in [getAvailableList].
     * @param selected   That will be new state.
     * @return True if the requested state has been applied or was already the same.
     */
    fun setSelected(model: T, position: Int, selected: Boolean): Boolean

    /**
     * Mark all the models in the list.
     *
     * The listeners for individual items won't be invoked.
     *
     * @param modelList To alter.
     * @param positions      Of the models in [getAvailableList] in the same size and order of model list.
     * @param selected       To apply as the state.
     * @return True when, other than some models rejecting to alter state, everything was okay.
     */
    fun setSelected(modelList: MutableList<T>, positions: IntArray, selected: Boolean): Boolean

    /**
     * Invoked only by the [IEngineConnection] owning it.
     *
     * The idea here is that you want to update the UI according to changes made on the connection, but don't want to
     * be warned when connections unrelated to you changes, which is also the case with a [PerformerListener]
     * on [IPerformerEngine].
     *
     * @param T Type that this listener will be called from.
     */
    interface SelectionListener<T : SelectionModel> {
        /**
         * Invoked upon altering an individual [SelectionModel].
         *
         * @param engine     Bound to this class.
         * @param owner      Making the call.
         * @param model Being altered.
         * @param isSelected State to set.
         * @param position   of the [SelectionModel] in [SelectionModelProvider.getAvailableList].
         */
        fun onSelected(
            engine: IPerformerEngine, owner: IEngineConnection<T>, model: T, isSelected: Boolean, position: Int,
        )

        /**
         * When a list of [SelectionModel]s have been changed, this is called.
         *
         * @param engine         Bound to this class.
         * @param owner          Making the call.
         * @param modelList Being altered.
         * @param isSelected     State to set.
         * @param positions      Of [SelectionModel]s in [SelectionModelProvider.getAvailableList]ç
         */
        fun onSelected(
            engine: IPerformerEngine,
            owner: IEngineConnection<T>,
            modelList: MutableList<T>,
            isSelected: Boolean,
            positions: IntArray,
        )
    }
}