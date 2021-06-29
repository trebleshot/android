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
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.databinding.LayoutClientConnectionBinding
import org.monora.uprotocol.client.android.viewmodel.ClientPickerViewModel
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

        binding.executePendingBindings()
        clientConnectionViewModel.start(args.client, args.clientAddress)
        clientConnectionViewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is ConnectionState.Connected -> {
                    clientPickerViewModel.bridge.postValue(it.bridge)
                    findNavController().navigateUp()
                }
                is ConnectionState.Error -> {
                    binding.image.alpha = 0.5f
                    binding.text.alpha = 0.5f
                    binding.textOffline.visibility = View.VISIBLE
                }
                is ConnectionState.NoAddress -> {
                    binding.textOffline.text = getString(R.string.text_deviceOffline)
                    binding.textOffline.visibility = View.VISIBLE
                }
                is ConnectionState.Connecting -> {
                }
            }

            binding.progress.visibility = if (it.connecting) View.VISIBLE else View.GONE
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

                delay(2000)

                state.postValue(ConnectionState.Connected(bridge.connect()))
            }
        } catch (e: Exception) {
            state.postValue(ConnectionState.Error(e))
        } finally {
            job = null
        }
    }.also { job = it }
}

sealed class ConnectionState(val connecting: Boolean = false) {
    class Connected(val bridge: CommunicationBridge) : ConnectionState()

    class Error(val e: Exception) : ConnectionState()

    class NoAddress : ConnectionState()

    class Connecting : ConnectionState(connecting = true)
}