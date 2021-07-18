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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.databinding.LayoutSharingBinding
import org.monora.uprotocol.client.android.databinding.ListSharingItemBinding
import org.monora.uprotocol.client.android.itemcallback.UTransferItemCallback
import org.monora.uprotocol.client.android.util.CommonErrorHelper
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.consume
import org.monora.uprotocol.client.android.viewmodel.content.TransferItemContentViewModel
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceException
import java.net.ProtocolException
import javax.inject.Inject

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
                sharingViewModel.consume(it, args.transferId, args.contents.toList())
            }
        }

        sharingViewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is SharingState.Success -> {
                    findNavController().navigate(
                        SharingFragmentDirections.actionSharingFragmentToNavTransferDetails(
                            it.transfer
                        )
                    )
                }
                is SharingState.Error -> {
                    val msg = CommonErrorHelper.messageOf(requireContext(), it.exception).message
                    Snackbar.make(binding.fab, msg, Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}

@HiltViewModel
class SharingViewModel @Inject internal constructor(
    val transferRepository: TransferRepository,
) : ViewModel() {
    private var consumer: Job? = null

    private val _state = MutableLiveData<SharingState>()

    val state = liveData {
        emitSource(_state)
    }

    fun consume(bridge: CommunicationBridge, transferId: Long, contents: List<UTransferItem>) {
        if (consumer != null) return

        consumer = viewModelScope.launch(Dispatchers.IO) {
            try {
                if (bridge.requestFileTransfer(transferId, contents)) {
                    val transfer = transferRepository.getTransfer(transferId,) ?: throw PersistenceException(
                        "The transfer object should exist after success"
                    )
                    _state.postValue(SharingState.Success(transfer))
                } else {
                    throw ProtocolException()
                }
            } catch (e: Exception) {
                _state.postValue(SharingState.Error(e))
            } finally {
                consumer = null
            }
        }
    }
}

sealed class SharingState {
    class Success(val transfer: Transfer) : SharingState()

    class Error(val exception: Exception) : SharingState()
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