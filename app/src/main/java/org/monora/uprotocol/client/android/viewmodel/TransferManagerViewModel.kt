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

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.TaskRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.protocol.rejectTransfer
import org.monora.uprotocol.client.android.protocol.startTransfer
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.client.android.util.TAG
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Inject

@HiltViewModel
class TransferManagerViewModel @Inject internal constructor(
    private val backend: Backend,
    private val clientRepository: ClientRepository,
    private val connectionFactory: ConnectionFactory,
    private val persistenceProvider: PersistenceProvider,
    private val transferRepository: TransferRepository,
    private val taskRepository: TaskRepository,
) : ViewModel() {
    fun rejectTransferRequest(
        client: Client,
        transfer: Transfer,
        rejectionState: MutableLiveData<RejectionState>? = null
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            rejectionState?.postValue(RejectionState.Running)

            try {
                val addresses = clientRepository.getInetAddresses(client.clientUid)

                CommunicationBridge.Builder(
                    connectionFactory, persistenceProvider, addresses
                ).apply {
                    setClearBlockedStatus(true)
                    setClientUid(client.clientUid)
                }.connect().use {
                    val result = it.rejectTransfer(transferRepository, transfer)
                    rejectionState?.postValue(RejectionState.Result(result))
                }
            } catch (e: Exception) {
                rejectionState?.postValue(RejectionState.Error(e))
            }
        }
    }

    fun toggleTransferOperation(client: Client, transfer: Transfer, detail: TransferDetail) {
        val bytesTotal = detail.size
        val bytesDone = detail.sizeOfDone

        if (taskRepository.cancelMatching { it.params is TransferParams && it.params.transfer.id == transfer.id }) {
            Log.d(TAG, "toggleTransferOperation: Toggle cancelled tasks")
            return
        }

        taskRepository.registerTransfer(
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
}


sealed class RejectionState {
    class Error(val exception: Exception) : RejectionState()

    object Running : RejectionState()

    class Result(val successful: Boolean) : RejectionState()
}