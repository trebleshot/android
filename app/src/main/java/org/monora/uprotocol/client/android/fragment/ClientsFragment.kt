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
package org.monora.uprotocol.client.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.EntryPoints
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListClientBinding
import org.monora.uprotocol.client.android.itemcallback.UClientItemCallback
import org.monora.uprotocol.client.android.viewholder.ClientViewHolder
import org.monora.uprotocol.client.android.viewmodel.ClientsViewModel
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentComponent

@AndroidEntryPoint
class ClientsFragment : Fragment(R.layout.layout_clients) {
    private val clientsViewModel: ClientsViewModel by viewModels()

    private val emptyContentViewModel: EmptyContentViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = Adapter()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.text_noClientList)
        emptyView.emptyImage.setImageResource(R.drawable.ic_devices_white_24dp)
        adapter.setHasStableIds(true)
        recyclerView.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter

        emptyView.executePendingBindings()
        clientsViewModel.clients.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }
    }

    class Adapter : ListAdapter<UClient, ClientViewHolder>(UClientItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
            return ClientViewHolder(
                ListClientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: ClientViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).uid.hashCode().toLong()
        }
    }
}