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
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.databinding.LayoutTransferDetailsBinding
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.util.TAG
import org.monora.uprotocol.client.android.viewmodel.RejectionState
import org.monora.uprotocol.client.android.viewmodel.TransferDetailsViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferDetailContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferStateContentViewModel
import javax.inject.Inject

@AndroidEntryPoint
class TransferDetailsFragment : Fragment(R.layout.layout_transfer_details) {
    @Inject
    lateinit var backend: Backend

    @Inject
    lateinit var factory: TransferDetailsViewModel.Factory

    private val args: TransferDetailsFragmentArgs by navArgs()

    private val viewModel: TransferDetailsViewModel by viewModels {
        TransferDetailsViewModel.ModelFactory(factory, args.transfer)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutTransferDetailsBinding.bind(view)

        binding.image.setOnClickListener {
            viewModel.client.value?.let {
                findNavController().navigate(
                    TransferDetailsFragmentDirections.actionTransferDetailsFragmentToClientDetailsFragment2(it)
                )
            }
        }
        binding.showFilesButton.setOnClickListener {
            findNavController().navigate(
                TransferDetailsFragmentDirections.actionTransferDetailsFragmentToTransferItemFragment(args.transfer)
            )
        }
        binding.floatingActionButton.setOnClickListener {
            viewModel.toggleTransferOperation()
        }
        binding.rejectButton.setOnClickListener { button ->
            viewModel.rejectTransferRequest()
        }

        viewModel.transferDetail.observe(viewLifecycleOwner) {
            if (it == null) {
                findNavController().navigateUp()
            } else {
                binding.transferViewModel = TransferDetailContentViewModel(it)
                binding.executePendingBindings()
            }
        }

        viewModel.client.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.clientViewModel = ClientContentViewModel(it)
                binding.executePendingBindings()
            }
        }

        viewModel.state.observe(viewLifecycleOwner) {
            binding.stateViewModel = when (val state = it?.state) {
                is Task.State.Progress -> TransferStateContentViewModel(it, state.total, state.progress)
                else -> TransferStateContentViewModel(it)
            }

            binding.executePendingBindings()
        }

        viewModel.rejectionState.observe(viewLifecycleOwner) {
            binding.rejectButton.isEnabled = it !is RejectionState.Running
        }
    }

    companion object {
        const val ACTION_TRANSFER_DETAIL = "org.monora.uprotocol.client.android.action.TRANSFER_DETAIL"
    }
}