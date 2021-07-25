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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.TaskRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.protocol.registerTransfer
import org.monora.uprotocol.client.android.protocol.rejectTransfer
import org.monora.uprotocol.client.android.protocol.startTransfer
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory

class TransferDetailsViewModel @AssistedInject internal constructor(
    taskRepository: TaskRepository,
    userRepository: ClientRepository,
    private val transferRepository: TransferRepository,
    private val persistenceProvider: PersistenceProvider,
    private val connectionFactory: ConnectionFactory,
    private val clientRepository: ClientRepository,
    private val backend: Backend,
    @Assisted private val transfer: Transfer,
) : ViewModel() {
    val client = userRepository.get(transfer.clientUid)

    val transferDetail = transferRepository.getTransferDetail(transfer.id)

    val state = taskRepository.subscribeToTask {
        if (it.params is TransferParams && it.params.transfer.id == transfer.id) it.params else null
    }

    private val _rejectionState = MutableLiveData<RejectionState>()

    val rejectionState = liveData {
        emitSource(_rejectionState)
    }

    fun rejectTransferRequest() {
        val client = client.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _rejectionState.postValue(RejectionState.Running)

            try {
                val addresses = clientRepository.getInetAddresses(client.clientUid)

                CommunicationBridge.Builder(
                    connectionFactory, persistenceProvider, addresses
                ).apply {
                    setClearBlockedStatus(true)
                    setClientUid(client.clientUid)
                }.connect().use {
                    _rejectionState.postValue(RejectionState.Result(it.rejectTransfer(transferRepository, transfer)))
                }
            } catch (e: Exception) {
                _rejectionState.postValue(RejectionState.Error(e))
            }
        }
    }

    fun toggleTransferOperation() {
        val client = client.value ?: return
        val detail = transferDetail.value ?: return
        val bytesTotal = detail.size
        val bytesDone = detail.sizeOfDone

        backend.registerTransfer(
            TransferParams(transfer, client, bytesTotal, bytesDone)
        ) { applicationScope, params, state ->
            applicationScope.launch(Dispatchers.IO) {
                state.postValue(Task.State.Running(backend.context.getString(R.string.text_connectingToClient)))

                try {
                    val addresses = clientRepository.getInetAddresses(params.client.clientUid)

                    CommunicationBridge.Builder(
                        connectionFactory, persistenceProvider, addresses
                    ).apply {
                        setClearBlockedStatus(true)
                        setClientUid(params.client.clientUid)
                    }.connect().use {
                        it.startTransfer(backend, transferRepository, params, state)
                    }
                } catch (e: Exception) {
                    state.postValue(Task.State.Error(e))
                }
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(transfer: Transfer): TransferDetailsViewModel
    }

    class ModelFactory(
        private val factory: Factory,
        private val transfer: Transfer,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            check(modelClass.isAssignableFrom(TransferDetailsViewModel::class.java)) {
                "Requested unknown view model type"
            }

            return factory.create(transfer) as T
        }
    }
}

sealed class RejectionState {
    class Error(val exception: Exception) : RejectionState()

    object Running : RejectionState()

    class Result(val successful: Boolean) : RejectionState()
}