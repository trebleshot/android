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
package com.genonbeta.android.framework.ui

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import android.content.Context
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.view.Menu
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import android.view.MenuItem
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * The idea here is that this class bridges one or more menus with a [IEngineConnection] to perform a specific
 * task whenever a new selectable is adder or removed and whenever the any item on a menu is clicked.
 *
 *
 * The class that is responsible for the performer menu should also provide the [IPerformerEngine]
 * to which this will add callbacks and listeners.
 *
 *
 * Because [Selectable] is referred to as the base class, the [Callback] methods shouldn't be used to
 * identify the derivatives. Instead, you should use the engine connection to identify the objects.
 */
class PerformerMenu(private val mContext: Context?, callback: Callback) : PerformerCallback, PerformerListener,
    MenuItem.OnMenuItemClickListener {
    private val mMenuInflater: MenuInflater?
    private val mCallback: Callback?

    /**
     * @return the application context
     */
    fun getContext(): Context? {
        return mContext
    }

    /**
     * @return the inflate the menu resources
     */
    fun getMenuInflater(): MenuInflater? {
        return mMenuInflater
    }

    fun invokeMenuItemSelected(menuItem: MenuItem?): Boolean {
        return mCallback.onPerformerMenuSelected(this, menuItem)
    }

    /**
     * Load the given menu by calling [Callback.onPerformerMenuList].
     *
     * @param targetMenu to populate
     * @return true when the given menu is populated
     */
    fun load(targetMenu: Menu?): Boolean {
        if (!populateMenu(targetMenu)) return false
        for (i in 0 until targetMenu.size()) targetMenu.getItem(i).setOnMenuItemClickListener(this)
        return true
    }

    /**
     * This is a call similar to [android.app.Activity.onCreateOptionsMenu]. This creates the menu list
     * which will be provided by [Callback.onPerformerMenuList]. If you
     * are not willing to make the [.invokeMenuItemSelected] calls manually, use
     * [.load] so that menu item selection calls will be handled directly by the [Callback].
     *
     *
     * The main difference is that when you want to work with more than one [IEngineConnection], the best is to
     * avoid using this, because you will often will not able to treat each [IEngineConnection] individually.
     * However, for example, if you are using a fragment and want to bridge default fragment callbacks like
     * [androidx.fragment.app.Fragment.onOptionsItemSelected] with this, it is best to use this so
     * that you can trigger menu creation as needed. To give an example again, you may want to keep a boolean variable
     * that goes 'selectionActivated' which will be used to assess whether the menu items will represent the selection.
     * And to reset the menus you can use [Activity.invalidateOptionsMenu] method.
     *
     * @param targetMenu to be populated.
     */
    fun populateMenu(targetMenu: Menu?): Boolean {
        return mCallback.onPerformerMenuList(this, getMenuInflater(), targetMenu)
    }

    /**
     * Register the callbacks of this instance, so that any change will be reported to us.
     *
     * @param engine that we are going to be informed about
     */
    fun setUp(engine: IPerformerEngine?) {
        engine.addPerformerListener(this)
        engine.addPerformerCallback(this)
    }

    /**
     * Unregister the previously registered callbacks of this instance.
     *
     * @param engine that we are no longer to be informed about
     */
    fun dismantle(engine: IPerformerEngine?) {
        engine.removePerformerCallback(this)
        engine.removePerformerListener(this)
    }

    override fun onSelection(
        engine: IPerformerEngine?, owner: IBaseEngineConnection?, selectable: Selectable?,
        isSelected: Boolean, position: Int
    ): Boolean {
        return mCallback.onPerformerMenuItemSelection(
            this, engine, owner, selectable, isSelected,
            position
        )
    }

    override fun onSelection(
        engine: IPerformerEngine?, owner: IBaseEngineConnection?,
        selectableList: MutableList<out Selectable?>?, isSelected: Boolean, positions: IntArray?
    ): Boolean {
        return mCallback.onPerformerMenuItemSelection(
            this, engine, owner, selectableList, isSelected,
            positions
        )
    }

    override fun onSelected(
        engine: IPerformerEngine?, owner: IBaseEngineConnection?, selectable: Selectable?,
        isSelected: Boolean, position: Int
    ) {
        mCallback.onPerformerMenuItemSelected(this, engine, owner, selectable, isSelected, position)
    }

    override fun onSelected(
        engine: IPerformerEngine?, owner: IBaseEngineConnection?,
        selectableList: MutableList<out Selectable?>?, isSelected: Boolean, positions: IntArray?
    ) {
        mCallback.onPerformerMenuItemSelected(this, engine, owner, selectableList, isSelected, positions)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return mCallback.onPerformerMenuSelected(this, item)
    }

    /**
     * The callback to connect the menu actions to. The data will be redirected from the other callbacks.
     */
    interface Callback {
        /**
         * Called when [PerformerMenu.load] is invoked to populate the menu.
         *
         * @param performerMenu instance that redirects the call
         * @param inflater      to inflate the menus with
         * @param targetMenu    to populate
         * @return true when there was not problem populating the menu
         */
        open fun onPerformerMenuList(performerMenu: PerformerMenu?, inflater: MenuInflater?, targetMenu: Menu?): Boolean

        /**
         * Called when a menu item on a populated menu (with callbacks registered) was clicked.
         *
         * @param performerMenu instance that redirects the call
         * @param item          that was clicked.
         * @return true when the input is known and the descendant is not needed the perform any other action
         */
        open fun onPerformerMenuSelected(performerMenu: PerformerMenu?, item: MenuItem?): Boolean

        /**
         * Called when a [Selectable] is being altered. This is called during the process which is not still
         * finished.
         *
         * @param performerMenu instance that redirects the call
         * @param engine        owning the [IBaseEngineConnection]
         * @param owner         that is managing the selection list and informing the [IPerformerEngine]
         * @param selectable    that is being altered
         * @param isSelected    is true when the new state is selected or false if otherwise
         * @param position      where the selectable is at on [SelectableProvider]
         * @return true if there is no problem with altering the state of selection of the selectable
         */
        open fun onPerformerMenuItemSelection(
            performerMenu: PerformerMenu?, engine: IPerformerEngine?,
            owner: IBaseEngineConnection?, selectable: Selectable?, isSelected: Boolean,
            position: Int
        ): Boolean

        /**
         * Called when a [Selectable] is being altered. This is called during the process which is not still
         * finished.
         *
         * @param performerMenu  instance that redirects the call
         * @param engine         owning the [IBaseEngineConnection]
         * @param owner          that is managing the selection list and informing the [IPerformerEngine]
         * @param selectableList that is being altered
         * @param isSelected     is true when the new state is selected or false if otherwise
         * @param positions      where the selectables are at on [SelectableProvider]
         * @return true if there is no problem with altering the state of selection of the selectable
         */
        open fun onPerformerMenuItemSelection(
            performerMenu: PerformerMenu?, engine: IPerformerEngine?,
            owner: IBaseEngineConnection?, selectableList: MutableList<out Selectable?>?,
            isSelected: Boolean, positions: IntArray?
        ): Boolean

        /**
         * Called after the [.onPerformerMenuItemSelection] to inform about the new state of the selectable.
         *
         * @param performerMenu instance that redirects the call
         * @param engine        owning the [IBaseEngineConnection]
         * @param owner         that is managing the selection list and informing the [IPerformerEngine]
         * @param selectable    that is being altered
         * @param isSelected    is true when the new state is selected or false if otherwise
         * @param position      where the selectable is at on [SelectableProvider]
         */
        open fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu?, engine: IPerformerEngine?,
            owner: IBaseEngineConnection?, selectable: Selectable?, isSelected: Boolean,
            position: Int
        )

        /**
         * Called after the [.onPerformerMenuItemSelection] to inform about the new state of the list of selectables.
         *
         * @param performerMenu  instance that redirects the call
         * @param engine         owning the [IBaseEngineConnection]
         * @param owner          that is managing the selection list and informing the [IPerformerEngine]
         * @param selectableList that is being altered
         * @param isSelected     is true when the new state is selected or false if otherwise
         * @param positions      where the selectables are at on [SelectableProvider]
         */
        open fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu?, engine: IPerformerEngine?,
            owner: IBaseEngineConnection?, selectableList: MutableList<out Selectable?>?,
            isSelected: Boolean, positions: IntArray?
        )
    }

    /**
     * Create an instance of PerformerMenu that to handle menus and listeners together.
     *
     * @param context  to access resources
     * @param callback to inform about and get input for selections, and menu item clicks
     */
    init {
        mMenuInflater = SupportMenuInflater(getContext())
        mCallback = callback
    }
}