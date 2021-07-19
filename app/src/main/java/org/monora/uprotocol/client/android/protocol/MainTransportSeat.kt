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
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.data.TaskRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.task.transfer.IndexingParams
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.client.android.util.Files
import org.monora.uprotocol.client.android.util.TAG
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientAddress
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.TransferItem.Type.Incoming
import org.monora.uprotocol.core.transfer.Transfers
import javax.inject.Inject

class MainTransportSeat @Inject constructor(
    @ApplicationContext val context: Context,
    private val connectionFactory: ConnectionFactory,
    private val persistenceProvider: PersistenceProvider,
    private val clientRepository: ClientRepository,
    private val transferRepository: TransferRepository,
    private val sharedTextRepository: SharedTextRepository,
    private val taskRepository: TaskRepository,
    private val backend: Backend,
) : TransportSeat {
    override fun beginFileTransfer(
        bridge: CommunicationBridge,
        client: Client,
        groupId: Long,
        type: TransferItem.Type,
    ) {
        val transferOperation = MainTransferOperation(backend)

        if (type == Incoming) {
            Transfers.receive(bridge, transferOperation, groupId)
        } else {
            Transfers.send(bridge, transferOperation, groupId)
        }
    }

    // TODO: 7/19/21 Handle acquaintance requests when the client picker fragment is showing
    override fun handleAcquaintanceRequest(client: Client, clientAddress: ClientAddress): Boolean = true

    override fun handleFileTransferRequest(client: Client, hasPin: Boolean, groupId: Long, jsonArray: String) {
        check(client is UClient) {
            "Expected the UClient implementation"
        }

        taskRepository.register(
            context.getString(R.string.mesg_organizingFiles),
            IndexingParams(groupId, client, jsonArray, hasPin)
        ) { applicationScope, params, state ->
            applicationScope.launch(Dispatchers.IO) {
                try {
                    val saveLocation = Files.getApplicationDirectory(context).toString()
                    val transfer = Transfer(params.transferId, client.clientUid, Incoming, saveLocation)
                    val items = persistenceProvider.toTransferItemList(params.transferId, params.jsonData).map {
                        // TODO: 7/4/21 PersistenceProvider types can be improved?
                        check(it is UTransferItem) {
                            "Unexpected type"
                        }

                        it
                    }

                    if (items.isNotEmpty()) {
                        transferRepository.insert(transfer)
                        transferRepository.insert(items)

                        if (hasPin) {
                            // TODO: 7/19/21 File transfer operation should be started when has PIN.
                        } else {
                            backend.notifyFileRequest(client, transfer, items)
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun handleFileTransferState(client: Client, groupId: Long, isAccepted: Boolean) {
        if (!isAccepted) {
            runBlocking {
                val transfer = transferRepository.getTransfer(groupId)
                if (transfer != null && transfer.clientUid == client.clientUid) {
                    transferRepository.delete(transfer)
                }
            }
        }
    }

    override fun handleTextTransfer(client: Client, text: String) {
        check(client is UClient) {
            "Expected the UClient implementation"
        }

        val sharedText = SharedText(0, text)

        runBlocking {
            sharedTextRepository.insert(sharedText)
        }

        backend.services.notifications.notifyClipboardRequest(client, sharedText)
    }

    override fun hasOngoingTransferFor(groupId: Long, clientUid: String, type: TransferItem.Type): Boolean {
        return taskRepository.contains { it.params is TransferParams && it.params.id == groupId }
    }

    override fun hasOngoingIndexingFor(groupId: Long): Boolean {
        return taskRepository.contains { it.params is IndexingParams && it.params.transferId == groupId }
    }

    override fun notifyClientCredentialsChanged(client: Client) {
        TODO("Not yet implemented")
    }
}