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
package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.protocol.NoAddressException
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Inject

@HiltViewModel
class ClientConnectionViewModel @Inject internal constructor(
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
                throw NoAddressException()
            }

            state.postValue(ConnectionState.Connecting())

            val bridge = CommunicationBridge.Builder(
                connectionFactory, persistenceProvider, addresses
            ).apply {
                setClearBlockedStatus(true)
                setClientUid(client.clientUid)
            }

            state.postValue(ConnectionState.Connected(bridge.connect()))
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

    class Connecting : ConnectionState(isConnecting = true)
}