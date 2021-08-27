/*
 * Copyright (C) 2021 Veli Tasalı
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
package org.monora.uprotocol.client.android.fragment.pickclient

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListPickClientBinding
import org.monora.uprotocol.client.android.itemcallback.UClientItemCallback
import org.monora.uprotocol.client.android.viewmodel.ClientsViewModel
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel

@AndroidEntryPoint
class PickClientFragment : Fragment(R.layout.layout_clients) {
    private val clientsViewModel: ClientsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = Adapter { client, clickType ->
            when (clickType) {
                ClientContentViewModel.ClickType.Default -> findNavController().navigate(
                    PickClientFragmentDirections.actionClientsFragmentToClientConnectionFragment(client)
                )
                ClientContentViewModel.ClickType.Details -> findNavController().navigate(
                    PickClientFragmentDirections.actionClientsFragmentToClientDetailsFragment(client)
                )
            }
        }
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.empty_clients_list)
        emptyView.emptyImage.setImageResource(R.drawable.ic_devices_white_24dp)
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        emptyView.executePendingBindings()
        clientsViewModel.clients.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }
    }

    class ClientViewHolder(
        val binding: ListPickClientBinding,
        val clickListener: (UClient, ClientContentViewModel.ClickType) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(client: UClient) {
            binding.viewModel = ClientContentViewModel(client)
            binding.clickListener =
                View.OnClickListener { clickListener(client, ClientContentViewModel.ClickType.Default) }
            binding.detailsClickListener =
                View.OnClickListener { clickListener(client, ClientContentViewModel.ClickType.Details) }
            binding.executePendingBindings()
        }
    }

    class Adapter(
        private val clickListener: (UClient, ClientContentViewModel.ClickType) -> Unit,
    ) : ListAdapter<UClient, ClientViewHolder>(UClientItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientViewHolder {
            return ClientViewHolder(
                ListPickClientBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                clickListener
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
