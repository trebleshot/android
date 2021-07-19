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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.databinding.LayoutTransferDetailsBinding
import org.monora.uprotocol.client.android.protocol.MainTransferOperation
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.client.android.util.TAG
import org.monora.uprotocol.client.android.viewmodel.TransferDetailsViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferDetailContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferStateContentViewModel
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.Transfers
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

    // TODO: 7/19/21 Remove test injections
    @Inject
    lateinit var connectionFactory: ConnectionFactory

    @Inject
    lateinit var transferRepository: TransferRepository

    @Inject
    lateinit var persistenceProvider: PersistenceProvider

    @Inject
    lateinit var clientRepository: ClientRepository

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
            viewModel.client.value?.let { client ->
                backend.register(
                    "Send files",
                    TransferParams(args.transfer.id)
                ) { applicationScope, params, state ->
                    applicationScope.launch(Dispatchers.IO) {
                        state.postValue(Task.State.Running("Starting"))

                        val addresses = clientRepository.getAddresses(client.clientUid).map {
                            it.inetAddress
                        }

                        try {
                            val bridge = CommunicationBridge.Builder(
                                connectionFactory, persistenceProvider, addresses
                            ).apply {
                                setClearBlockedStatus(true)
                                setClientUid(client.clientUid)
                            }.connect()

                            Log.d(TAG, "onViewCreated: Start transfer ${args.transfer.id}")

                            if (bridge.requestFileTransferStart(args.transfer.id, args.transfer.type)) {
                                val transferOperation = MainTransferOperation(backend)

                                Log.d(TAG, "onViewCreated: It was okay!")

                                if (args.transfer.type == TransferItem.Type.Incoming) {
                                    Log.d(TAG, "onViewCreated: Receiving")
                                    Transfers.receive(bridge, transferOperation, args.transfer.id)
                                } else {
                                    Log.d(TAG, "onViewCreated: Sending")
                                    Transfers.send(bridge, transferOperation, args.transfer.id)
                                }
                            } else {
                                Log.d(TAG, "onViewCreated: Returned false")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        viewModel.transferDetail.observe(viewLifecycleOwner) {
            if (it != null) {
                binding.transferViewModel = TransferDetailContentViewModel(it)
                binding.executePendingBindings()
            }
        }

        viewModel.client.observe(viewLifecycleOwner) {
            if (it == null) {
                findNavController().popBackStack()
            } else {
                binding.clientViewModel = ClientContentViewModel(it)
                binding.executePendingBindings()
            }
        }

        viewModel.state.observe(viewLifecycleOwner) {
            binding.stateViewModel = TransferStateContentViewModel(it)
            binding.executePendingBindings()

            when (val state = it?.state) {
                is Task.State.Progress -> {
                    binding.progressBar.max = state.total
                    binding.progressBar.progress = state.progress
                }
            }
        }
    }
}