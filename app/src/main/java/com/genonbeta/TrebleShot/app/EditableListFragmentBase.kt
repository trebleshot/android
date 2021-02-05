/*
 * Copyright (C) 2019 Veli Tasalı
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
package com.genonbeta.TrebleShot.app

import android.net.Uri
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.ui.callbackimport.TitleProvider
import com.genonbeta.TrebleShot.widgetimport.EditableListAdapterBase
import com.genonbeta.android.framework.app.ListFragmentBase
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.android.framework.util.actionperformer.SelectableHost
import com.genonbeta.android.framework.util.actionperformer.SelectableProvider

/**
 * created by: veli
 * date: 14/04/18 10:35
 */
interface EditableListFragmentBase<T : Editable> : ListFragmentBase<T>, PerformerEngineProvider,
    IEngineConnection.SelectionListener<T>, SelectableProvider<T>, SelectableHost<T>, TitleProvider {
    fun applyViewingChanges(gridSize: Int)
    fun changeGridViewSize(gridSize: Int)
    fun changeOrderingCriteria(id: Int)
    fun changeSortingCriteria(id: Int)
    val adapterImpl: EditableListAdapterBase<T>?
    val engineConnection: IEngineConnection<T>
    fun getFilteringDelegate(): EditableListFragment.FilteringDelegate<T>
    val listView: RecyclerView?
    val orderingCriteria: Int
    val sortingCriteria: Int
    fun getUniqueSettingKey(setting: String): String
    val isGridSupported: Boolean
    val isLocalSelectionActivated: Boolean
    val isRefreshRequested: Boolean
    val isSortingSupported: Boolean
    val isUsingLocalSelection: Boolean
    fun loadIfRequested(): Boolean
    fun openUri(uri: Uri): Boolean
    fun setFilteringDelegate(delegate: EditableListFragment.FilteringDelegate<T>?)
}