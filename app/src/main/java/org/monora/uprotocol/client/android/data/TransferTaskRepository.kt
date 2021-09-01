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

package org.monora.uprotocol.client.android.data

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.protocol.closeQuietly
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.client.android.task.transfer.TransferRejectionParams
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferTaskRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backend: Backend,
    private val clientRepository: ClientRepository,
    private val connectionFactory: ConnectionFactory,
    private val persistenceProvider: PersistenceProvider,
    private val taskRepository: TaskRepository,
    private val transferRepository: TransferRepository,
    private val transportSeat: TransportSeat,
) {
    fun rejectTransfer(transfer: Transfer, client: Client) {
        if (taskRepository.contains { it.params is TransferRejectionParams && it.params.transfer.id == transfer.id }) {
            return
        }

        taskRepository.register(
            context.getString(R.string.rejecting),
            TransferRejectionParams(transfer, client)
        ) { applicationScope, params, state ->
            applicationScope.launch(Dispatchers.IO) {
                val addresses = clientRepository.getInetAddresses(params.client.clientUid)

                try {
                    CommunicationBridge.Builder(connectionFactory, persistenceProvider, addresses).apply {
                        setClearBlockedStatus(true)
                        setClientUid(params.client.clientUid)
                    }.connect().use {
                        if (it.requestNotifyTransferRejection(params.transfer.id)) {
                            transferRepository.delete(params.transfer)
                        }
                    }
                } catch (e: Exception) {
                    state.postValue(Task.State.Error(e))
                }
            }
        }
    }

    fun toggleTransferOperation(transfer: Transfer, client: Client, detail: TransferDetail) {
        val cancelledAny = taskRepository.cancelMatching {
            it.params is TransferParams && it.params.transfer.id == transfer.id
        }

        if (cancelledAny) {
            Log.d(TAG, "toggleTransferOperation: Found and cancelled tasks")
            return
        }

        taskRepository.register(
            TransferParams(transfer, client, detail.size)
        ) { applicationScope, params, state ->
            applicationScope.launch(Dispatchers.IO) {
                state.postValue(Task.State.Running(backend.context.getString(R.string.connect_to_client)))

                try {
                    val addresses = clientRepository.getInetAddresses(params.client.clientUid)
                    val bridge = CommunicationBridge.Builder(connectionFactory, persistenceProvider, addresses).apply {
                        setClearBlockedStatus(true)
                        setClientUid(params.client.clientUid)
                    }.connect()

                    if (bridge.requestFileTransferStart(params.transfer.id, params.transfer.direction)) {
                        transportSeat.beginFileTransfer(
                            bridge, params.client, params.transfer.id, params.transfer.direction
                        )
                    } else {
                        bridge.closeQuietly()
                    }
                } catch (e: Exception) {
                    state.postValue(Task.State.Error(e))
                }
            }
        }
    }

    companion object {
        private const val TAG = "TransferTaskRepository"
    }
}
