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
package org.monora.uprotocol.client.android.app

import android.content.Context
import android.net.Uri
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.widget.ListingAdapterBase
import com.genonbeta.android.framework.app.ListingFragmentBase
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.android.framework.util.actionperformer.SelectionHost
import com.genonbeta.android.framework.util.actionperformer.SelectionModelProvider
import org.monora.uprotocol.client.android.model.ContentModel

/**
 * created by: veli
 * date: 14/04/18 10:35
 */
interface ListingFragmentBase<T : ContentModel> : ListingFragmentBase, PerformerEngineProvider,
    IEngineConnection.SelectionListener<T>, SelectionModelProvider<T>, SelectionHost<T> {
    val adapterImpl: ListingAdapterBase<T>

    val engineConnection: IEngineConnection<T>

    var filteringDelegate: ListingFragment.FilteringDelegate<T>

    var filteringSupported: Boolean

    val listView: RecyclerView

    var localSelectionActivated: Boolean

    val localSelectionMode: Boolean

    var selectByClickEnabled: Boolean

    var refreshRequested: Boolean

    fun getUniqueSettingKey(setting: String): String

    fun openUri(uri: Uri): Boolean

    fun loadIfRequested(): Boolean

    fun requireActivity(): FragmentActivity

    fun requireContext(): Context
}
