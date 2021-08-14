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

package org.monora.uprotocol.client.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.databinding.LayoutSharingBinding
import org.monora.uprotocol.client.android.databinding.ListSharingItemBinding
import org.monora.uprotocol.client.android.itemcallback.UTransferItemCallback
import org.monora.uprotocol.client.android.util.CommonErrors
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingState
import org.monora.uprotocol.client.android.viewmodel.SharingViewModel
import org.monora.uprotocol.client.android.viewmodel.consume
import org.monora.uprotocol.client.android.viewmodel.content.TransferItemContentViewModel
import org.monora.uprotocol.core.transfer.TransferItem

@AndroidEntryPoint
class SharingFragment : Fragment(R.layout.layout_sharing) {
    private val args: SharingFragmentArgs by navArgs()

    private val sharingViewModel: SharingViewModel by viewModels()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = LayoutSharingBinding.bind(view)
        val adapter = SharingContentAdapter()

        binding.recyclerView.adapter = adapter

        binding.fab.setOnClickListener {
            findNavController().navigate(SharingFragmentDirections.pickClient())
        }

        adapter.submitList(args.contents.toList())

        clientPickerViewModel.bridge.observe(viewLifecycleOwner) { statefulBridge ->
            statefulBridge.consume()?.let {
                sharingViewModel.consume(it, args.groupId, args.contents.toList())
            }
        }

        sharingViewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is SharingState.Initial -> if (it.consume()) findNavController().navigate(
                    SharingFragmentDirections.pickClient()
                )
                is SharingState.Running -> {

                }
                is SharingState.Success -> {
                    findNavController().navigate(
                        SharingFragmentDirections.actionSharingFragmentToNavTransferDetails(
                            it.transfer
                        )
                    )
                }
                is SharingState.Error -> {
                    val msg = CommonErrors.messageOf(requireContext(), it.exception)
                    Snackbar.make(binding.fab, msg, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

class SharingContentAdapter : ListAdapter<UTransferItem, SharingViewHolder>(UTransferItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharingViewHolder {
        return SharingViewHolder(
            ListSharingItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: SharingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = with(getItem(position)) { groupId + id }
}

class SharingViewHolder(private val binding: ListSharingItemBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(transferItem: UTransferItem) {
        binding.viewModel = TransferItemContentViewModel(transferItem)
        binding.executePendingBindings()
    }
}
