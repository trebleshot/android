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

package org.monora.uprotocol.client.android.protocol

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.backend.TransportRegistry
import org.monora.uprotocol.client.android.data.FileRepository
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.data.TaskRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.IndexingParams
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.persistence.PersistenceException
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientAddress
import org.monora.uprotocol.core.protocol.ClipboardType
import org.monora.uprotocol.core.protocol.Direction
import org.monora.uprotocol.core.protocol.Direction.Incoming
import org.monora.uprotocol.core.protocol.communication.ContentException
import org.monora.uprotocol.core.transfer.Transfers
import javax.inject.Inject

class MainTransportSeat @Inject constructor(
    @ApplicationContext val context: Context,
    private val backend: Backend,
    private val fileRepository: FileRepository,
    private val persistenceProvider: PersistenceProvider,
    private val sharedTextRepository: SharedTextRepository,
    private val taskRepository: TaskRepository,
    private val transferRepository: TransferRepository,
    private val transportRegistry: TransportRegistry,
) : TransportSeat {
    override fun beginFileTransfer(
        bridge: CommunicationBridge,
        client: Client,
        groupId: Long,
        direction: Direction,
    ) {
        bridge.activeConnection.isRoaming = true

        runBlocking {
            val transfer = transferRepository.getTransfer(groupId) ?: throw PersistenceException(
                "Missing group $groupId"
            )
            val detail = transferRepository.getTransferDetailDirect(groupId) ?: throw PersistenceException(
                "Missing detail $groupId"
            )

            if (!transfer.accepted) {
                transfer.accepted = true
                transferRepository.update(transfer)
            }

            taskRepository.register(
                TransferParams(transfer, client, detail.size, detail.sizeOfDone),
            ) { applicationScope, params, state ->
                applicationScope.launch(Dispatchers.IO) {
                    val operation = MainTransferOperation(
                        backend, transferRepository, params, state, bridge.cancellationCallback
                    )

                    bridge.use {
                        try {
                            if (transfer.direction.isIncoming) {
                                Transfers.receive(it, operation, transfer.id)
                            } else {
                                Transfers.send(it, operation, transfer.id)
                            }
                        } catch (e: Exception) {
                            state.postValue(Task.State.Error(e))
                        }
                    }
                }
            }
        }
    }

    override fun handleClipboardRequest(client: Client, content: String, type: ClipboardType): Boolean {
        check(client is UClient) {
            "Expected the UClient implementation"
        }

        val sharedText = SharedText(0, client.clientUid, content)

        runBlocking {
            sharedTextRepository.insert(sharedText)
        }

        backend.services.notifications.notifyClipboardRequest(client, sharedText)
        return true
    }

    override fun handleFileTransferRequest(
        client: Client,
        hasPin: Boolean,
        groupId: Long,
        jsonArray: String
    ): Boolean {
        check(client is UClient) {
            "Expected the UClient implementation"
        }

        val storageLocation = fileRepository.appDirectory.originalUri.toString()
        val transfer = Transfer(groupId, client.clientUid, Incoming, storageLocation, accepted = hasPin)
        var items: List<UTransferItem>? = null

        runBlocking {
            taskRepository.register(
                context.getString(R.string.preparing),
                IndexingParams(groupId, client, jsonArray, hasPin),
            ) { applicationScope, params, state ->
                applicationScope.launch(coroutineContext) {
                    state.postValue(Task.State.Running(context.getString(R.string.organizing_files_notice)))

                    Transfers.toTransferItemList(jsonArray).map {
                        val item = persistenceProvider.createTransferItemFor(
                            params.groupId, it.id, it.name, it.mimeType, it.size, it.directory, Incoming
                        )

                        check(item is UTransferItem) {
                            "Unexpected type"
                        }

                        item
                    }.also {
                        if (it.isNotEmpty()) {
                            transferRepository.insert(transfer)
                            transferRepository.insert(it)

                            items = it
                        }
                    }
                }
            }
        }

        return items?.let {
            if (!transportRegistry.handleTransferRequest(transfer, hasPin) && !hasPin) {
                backend.services.notifications.notifyTransferRequest(client, transfer, it)
            }
            hasPin
        } ?: false
    }

    override fun handleFileTransferRejection(client: Client, groupId: Long): Boolean {
        return runBlocking {
            val transfer = transferRepository.getTransfer(groupId) ?: throw ContentException(
                ContentException.Error.NotFound
            )

            if (transfer.clientUid != client.clientUid) {
                throw ContentException(ContentException.Error.NotAccessible)
            } else if (!transfer.accepted) {
                transferRepository.delete(transfer)
                return@runBlocking true
            }
            false
        }
    }

    override fun handleGuidanceRequest(
        bridge: CommunicationBridge,
        client: Client,
        clientAddress: ClientAddress,
        direction: Direction,
    ) {
        check(client is UClient) {
            "Expected the UClient implementation"
        }

        check(clientAddress is UClientAddress) {
            "Expected the UClientAddress implementation"
        }

        transportRegistry.handleGuidanceRequest(bridge, direction)
    }

    override fun hasOngoingTransferFor(groupId: Long, clientUid: String, direction: Direction): Boolean {
        return taskRepository.contains { it.params is TransferParams && it.params.transfer.id == groupId }
    }

    override fun hasOngoingIndexingFor(groupId: Long): Boolean {
        return taskRepository.contains { it.params is IndexingParams && it.params.groupId == groupId }
    }

    override fun notifyClientCredentialsChanged(client: Client) {
        check(client is UClient) {
            "Unexpected UClient implementation"
        }
        backend.services.notifications.notifyClientCredentialsChanged(client)
    }
}
