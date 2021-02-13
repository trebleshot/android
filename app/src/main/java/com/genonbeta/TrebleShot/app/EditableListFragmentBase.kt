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

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.ui.callback.TitleProvider
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
    val adapterImpl: EditableListAdapterBase<T>

    val engineConnection: IEngineConnection<T>

    var filteringDelegate: EditableListFragment.FilteringDelegate<T>

    var gridSize: Int

    var isGridSupported: Boolean

    var isFilteringSupported: Boolean

    var isLocalSelectionActivated: Boolean

    var isRefreshRequested: Boolean

    var isSelectByClick: Boolean

    var isSortingSupported: Boolean

    var isTwoRowLayout: Boolean

    var isUsingLocalSelection: Boolean

    val listView: RecyclerView

    var orderingCriteria: Int

    var sortingCriteria: Int

    fun applyViewingChanges(gridSize: Int)

    fun getUniqueSettingKey(setting: String): String

    fun openUri(uri: Uri): Boolean

    fun loadIfRequested(): Boolean

    fun requireActivity(): FragmentActivity

    fun requireContext(): Context
}
