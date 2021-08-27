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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.NavPickClientDirections
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutConnectionOptionsBinding
import org.monora.uprotocol.client.android.databinding.ListClientGridBinding
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.viewholder.ClientGridViewHolder
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.ClientsViewModel
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import org.monora.uprotocol.core.protocol.Direction

@AndroidEntryPoint
class ConnectionOptionsFragment : Fragment(R.layout.layout_connection_options) {
    private val args: ConnectionOptionsFragmentArgs by navArgs()

    private val clientsViewModel: ClientsViewModel by viewModels()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = LayoutConnectionOptionsBinding.bind(view)
        val adapter = OnlineClientsAdapter { clientRoute, clickType ->
            when (clickType) {
                ClientContentViewModel.ClickType.Default -> findNavController().navigate(
                    ConnectionOptionsFragmentDirections.actionOptionsFragmentToClientConnectionFragment(
                        clientRoute.client, clientRoute.address
                    )
                )
                ClientContentViewModel.ClickType.Details -> findNavController().navigate(
                    ConnectionOptionsFragmentDirections.actionOptionsFragmentToClientDetailsFragment(clientRoute.client)
                )
            }
        }
        val emptyContentViewModel = EmptyContentViewModel()

        adapter.setHasStableIds(true)
        binding.emptyView.emptyText.setText(R.string.empty_online_clients_list)
        binding.emptyView.emptyImage.setImageResource(R.drawable.ic_devices_white_24dp)
        binding.emptyView.viewModel = emptyContentViewModel
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager?.let {
            if (it is GridLayoutManager) {
                it.orientation = GridLayoutManager.HORIZONTAL
            }
        }
        binding.senderNoticeViews.visibility = if (args.direction == Direction.Outgoing) View.VISIBLE else View.GONE

        binding.clickListener = View.OnClickListener { v: View ->
            when (v.id) {
                R.id.clientsButton -> findNavController().navigate(
                    ConnectionOptionsFragmentDirections.actionOptionsFragmentToClientsFragment()
                )
                R.id.generateQrCodeButton -> findNavController().navigate(
                    ConnectionOptionsFragmentDirections.actionOptionsFragmentToNetworkManagerFragment(args.direction)
                )
                R.id.manualAddressButton -> findNavController().navigate(
                    ConnectionOptionsFragmentDirections.actionOptionsFragmentToManualConnectionFragment()
                )
                R.id.scanQrCodeButton -> findNavController().navigate(
                    ConnectionOptionsFragmentDirections.actionOptionsFragmentToBarcodeScannerFragment()
                )
            }
        }

        binding.executePendingBindings()

        clientsViewModel.onlineClients.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(binding.recyclerView, it.isNotEmpty())
        }

        clientPickerViewModel.registerForGuidanceRequests(viewLifecycleOwner, args.direction) { bridge ->
            clientPickerViewModel.bridge.postValue(bridge)
            findNavController().navigate(NavPickClientDirections.xmlPop())
        }

        clientPickerViewModel.registerForTransferRequests(viewLifecycleOwner) { transfer, _ ->
            if (args.direction == Direction.Incoming) {
                findNavController().navigate(
                    ConnectionOptionsFragmentDirections.actionOptionsFragmentToNavTransferDetails(transfer)
                )
            }
        }
    }
}

class OnlineClientsAdapter(
    private val clickListener: (ClientRoute, ClientContentViewModel.ClickType) -> Unit
) : ListAdapter<ClientRoute, ClientGridViewHolder>(ClientRouteItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ClientGridViewHolder {
        return ClientGridViewHolder(
            ListClientGridBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            clickListener
        )
    }

    override fun onBindViewHolder(holder: ClientGridViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).client.uid.hashCode().toLong()
    }
}

class ClientRouteItemCallback : DiffUtil.ItemCallback<ClientRoute>() {
    override fun areItemsTheSame(oldItem: ClientRoute, newItem: ClientRoute): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: ClientRoute, newItem: ClientRoute): Boolean {
        return oldItem == newItem
    }
}
