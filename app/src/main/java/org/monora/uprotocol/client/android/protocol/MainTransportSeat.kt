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
import dagger.Lazy
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.task.IndexTransferTask
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientAddress
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject

class MainTransportSeat @Inject constructor(
    @ApplicationContext val context: Context,
    private val connectionFactory: ConnectionFactory,
    private val persistenceProvider: PersistenceProvider,
    private val clientRepository: ClientRepository,
    private val transferRepository: TransferRepository,
    private val sharedTextRepository: SharedTextRepository,
    backgroundBackend: Lazy<BackgroundBackend>,
) : TransportSeat {
    private val backgroundBackend by lazy {
        backgroundBackend.get()
    }

    override fun beginFileTransfer(
        bridge: CommunicationBridge,
        client: Client,
        groupId: Long,
        type: TransferItem.Type,
    ) {
        TODO("Not yet implemented")
    }

    override fun handleAcquaintanceRequest(client: Client, clientAddress: ClientAddress): Boolean = true

    override fun handleFileTransferRequest(client: Client, hasPin: Boolean, groupId: Long, jsonArray: String) {
        check(client is UClient) {
            "Expected the UClient implementation"
        }

        backgroundBackend.run(
            IndexTransferTask(
                connectionFactory,
                persistenceProvider,
                clientRepository,
                transferRepository,
                groupId,
                jsonArray,
                client,
                hasPin
            )
        )
    }

    override fun handleFileTransferState(client: Client, groupId: Long, isAccepted: Boolean) {
        if (!isAccepted) {

        }
    }

    override fun handleTextTransfer(client: Client, text: String) {
        check(client is UClient) {
            "Expected the UClient implementation"
        }

        val sharedText = SharedText(0, text)

        //sharedTextRepository.insert(sharedText)
        backgroundBackend.notificationHelper.notifyClipboardRequest(client, sharedText)
    }

    override fun hasOngoingTransferFor(groupId: Long, clientUid: String, type: TransferItem.Type): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasOngoingIndexingFor(groupId: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun notifyClientCredentialsChanged(client: Client) {
        TODO("Not yet implemented")
    }
}