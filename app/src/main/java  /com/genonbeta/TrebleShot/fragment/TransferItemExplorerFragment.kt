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
package com.genonbeta.TrebleShot.fragment

import android.content.*
import com.genonbeta.TrebleShot.R
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.activity.TransferDetailActivity
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter
import com.genonbeta.TrebleShot.adapter.TransferPathResolverRecyclerAdapter
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import java.io.File

/**
 * created by: veli
 * date: 3/11/19 7:37 PM
 */
class TransferItemExplorerFragment : TransferItemListFragment() {
    private lateinit var pathView: RecyclerView

    lateinit var toggleButton: ExtendedFloatingActionButton

    private lateinit var pathAdapter: TransferPathResolverRecyclerAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bottomSpaceShown = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutResId = R.layout.layout_transfer_explorer
        dividerResId = R.id.layout_transfer_explorer_separator
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        toggleButton = view.findViewById(R.id.layout_transfer_explorer_efab)
        pathView = view.findViewById(R.id.layout_transfer_explorer_recycler)
        pathAdapter = TransferPathResolverRecyclerAdapter(requireContext())
        val layoutManager = LinearLayoutManager(
            context, RecyclerView.HORIZONTAL,
            false
        )
        layoutManager.stackFromEnd = true
        pathView.setHasFixedSize(true)
        pathView.layoutManager = layoutManager
        pathView.adapter = pathAdapter
        pathAdapter.clickListener = object : PathResolverRecyclerAdapter.OnClickListener<String?> {
            override fun onClick(holder: PathResolverRecyclerAdapter.Holder<String?>) {
                // FIXME: 2/20/21 go path
                //goPath(holder.index.data)
            }
        }

        activity?.let {
            if (it is TransferDetailActivity) it.showMenus()
        }
    }

    override fun onListRefreshed() {
        super.onListRefreshed()
        // FIXME: 2/20/21 go path
        //val path = adapter.path
        //pathAdapter.goTo(adapter.member, path?.split(File.separator.toRegex())?.toTypedArray())
        pathAdapter.notifyDataSetChanged()
        if (pathAdapter.itemCount > 0) pathView.smoothScrollToPosition(pathAdapter.itemCount - 1)
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_files)
    }
}