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
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.databinding.LayoutClientConnectionBinding
import org.monora.uprotocol.client.android.util.CommonErrorHelper
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
import org.monora.uprotocol.client.android.viewmodel.StatefulBridge
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Inject

@AndroidEntryPoint
class ClientConnectionFragment : Fragment(R.layout.layout_client_connection) {
    private val args: ClientConnectionFragmentArgs by navArgs()

    private val clientPickerViewModel: ClientPickerViewModel by activityViewModels()

    private val clientConnectionViewModel: ClientConnectionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutClientConnectionBinding.bind(view)
        binding.client = args.client

        binding.retryButton.setOnClickListener {
            clientConnectionViewModel.start(args.client, args.clientAddress)
        }

        binding.executePendingBindings()

        binding.retryButton.performClick()

        clientConnectionViewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is ConnectionState.Connected -> {
                    clientPickerViewModel.bridge.postValue(StatefulBridge.of(false, it.bridge))
                    findNavController().navigate(
                        ClientConnectionFragmentDirections.actionClientConnectionFragmentToOptionsFragment2()
                    )
                }
                is ConnectionState.Error -> {
                    binding.textOffline.text = CommonErrorHelper.messageOf(requireContext(), it.e).message
                }
                is ConnectionState.NoAddress -> {
                    binding.textOffline.text = getString(R.string.mesg_clientOffline)
                }
                is ConnectionState.Connecting -> {
                }
            }

            val alpha = if (it.isError) 0.5f else 1f

            binding.image.alpha = alpha
            binding.text.alpha = alpha
            binding.textOffline.visibility = if (it.isError) View.VISIBLE else View.GONE
            binding.progress.visibility = if (it.isConnecting) View.VISIBLE else View.GONE
            binding.retryButton.visibility =
                if (!it.isConnecting && it !is ConnectionState.Connected) View.VISIBLE else View.GONE
            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
        }
    }
}

@HiltViewModel
class ClientConnectionViewModel @Inject constructor(
    val connectionFactory: ConnectionFactory,
    val persistenceProvider: PersistenceProvider,
    var clientRepository: ClientRepository,
) : ViewModel() {
    private var job: Job? = null

    val state = MutableLiveData<ConnectionState>()

    fun start(client: UClient, address: UClientAddress?): Job = job ?: viewModelScope.launch(Dispatchers.IO) {
        val addresses = address?.let { listOf(it.inetAddress) } ?: clientRepository.getAddresses(client.clientUid).map {
            it.inetAddress
        }

        try {
            if (addresses.isEmpty()) {
                state.postValue(ConnectionState.NoAddress())
            } else {
                state.postValue(ConnectionState.Connecting())

                val bridge = CommunicationBridge.Builder(
                    connectionFactory, persistenceProvider, addresses
                ).apply {
                    setClearBlockedStatus(true)
                    setClientUid(client.clientUid)
                }

                state.postValue(ConnectionState.Connected(bridge.connect()))
            }
        } catch (e: Exception) {
            state.postValue(ConnectionState.Error(e))
        } finally {
            job = null
        }
    }.also { job = it }
}

sealed class ConnectionState(val isConnecting: Boolean = false, val isError: Boolean = false) {
    class Connected(val bridge: CommunicationBridge) : ConnectionState()

    class Error(val e: Exception) : ConnectionState(isError = true)

    class NoAddress : ConnectionState(isError = true)

    class Connecting : ConnectionState(isConnecting = true)
}