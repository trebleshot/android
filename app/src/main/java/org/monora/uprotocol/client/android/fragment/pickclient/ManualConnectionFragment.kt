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
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutManualConnectionBinding
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.util.clientRoute
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.protocol.communication.client.UnauthorizedClientException
import java.net.InetAddress
import java.net.UnknownHostException
import javax.inject.Inject

@AndroidEntryPoint
class ManualConnectionFragment : Fragment(R.layout.layout_manual_connection) {
    private val viewModel: ManualConnectionViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = LayoutManualConnectionBinding.bind(view)

        binding.confirmButton.setOnClickListener {
            val address = binding.editText.text?.trim()?.toString()

            if (address.isNullOrEmpty()) {
                binding.editText.error = getString(R.string.host_address_invalid_notice)
            } else {
                binding.editText.error = null
                viewModel.connect(address)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) {
            when (it) {
                is ManualConnectionState.Loading -> {

                }
                is ManualConnectionState.Error -> when (it.exception) {
                    is UnknownHostException -> binding.editText.error = getString(R.string.error_unknown_host)
                    is UnauthorizedClientException -> binding.editText.error = getString(R.string.error_not_allowed)
                    else -> binding.editText.error = it.exception.message
                }
                is ManualConnectionState.Loaded -> if (!it.isUsed) {
                    it.isUsed = true

                    findNavController().navigate(
                        ManualConnectionFragmentDirections.actionManualConnectionFragmentToAcceptClientFragment(
                            it.clientRoute
                        )
                    )
                }
            }

            TransitionManager.beginDelayedTransition(binding.root as ViewGroup)
            binding.progressBar.visibility = if (it.loading) View.VISIBLE else View.GONE
            binding.confirmButton.isEnabled = !it.loading
        }
    }
}

@HiltViewModel
class ManualConnectionViewModel @Inject internal constructor(
    private var connectionFactory: ConnectionFactory,
    private var persistenceProvider: PersistenceProvider,
) : ViewModel() {
    private val _state = MutableLiveData<ManualConnectionState>()

    private var _job: Job? = null

    val state = liveData {
        emitSource(_state)
    }

    fun connect(address: String) = _job ?: viewModelScope.launch(Dispatchers.IO) {
        _state.postValue(ManualConnectionState.Loading())

        try {
            val inetAddress = InetAddress.getByName(address)
            CommunicationBridge.connect(connectionFactory, persistenceProvider, inetAddress).use {
                it.send(false)
                _state.postValue(ManualConnectionState.Loaded(it.clientRoute))
            }
        } catch (e: Exception) {
            _state.postValue(ManualConnectionState.Error(e))
        } finally {
            _job = null
        }
    }.also { _job = it }
}

sealed class ManualConnectionState(val loading: Boolean = false) {
    class Loading : ManualConnectionState(true)

    class Error(val exception: Exception) : ManualConnectionState()

    class Loaded(val clientRoute: ClientRoute, var isUsed: Boolean = false) : ManualConnectionState()
}
