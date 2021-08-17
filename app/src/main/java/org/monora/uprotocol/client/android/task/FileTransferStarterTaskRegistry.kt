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

package org.monora.uprotocol.client.android.task

import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.protocol.Direction
import org.monora.uprotocol.core.transfer.TransferItem
import java.net.InetAddress

class FileTransferStarterTask(
    val connectionFactory: ConnectionFactory,
    val persistenceProvider: PersistenceProvider,
    val transfer: Transfer,
    val client: UClient,
    val direction: Direction,
    val addressList: List<InetAddress>,
) {
    /*override fun onRun() {
        try {
            CommunicationBridge.Builder(
                connectionFactory, persistenceProvider, addressList
            ).apply {
                setClientUid(client.clientUid)
            }.connect().use { bridge ->
                bridge.requestFileTransferStart(transfer.id, type)
                if (bridge.receiveResult()) {
                    backend.attach(FileTransferTaskRegistry(bridge, transfer, client, type))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(context, e))
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_transfer)
    }

    companion object {
        @Throws(
            TransferNotFoundException::class,
            DeviceNotFoundException::class,
            ConnectionNotFoundException::class,
            TargetNotFoundException::class
        )
        suspend fun createFrom(
            connectionFactory: ConnectionFactory,
            persistenceProvider: PersistenceProvider,
            clientRepository: ClientRepository,
            transferRepository: TransferRepository,
            groupId: Long,
            clientUid: String,
            type: TransferItem.Type,
        ): FileTransferStarterTaskRegistry {
            val client = clientRepository.getSingle(clientUid) ?: throw DeviceNotFoundException(clientUid)
            val transfer = transferRepository.getTransfer(groupId) ?: throw TransferNotFoundException(groupId)

            return createFrom(connectionFactory, persistenceProvider, clientRepository, transfer, client, type)
        }

        @Throws(TargetNotFoundException::class, ConnectionNotFoundException::class)
        suspend fun createFrom(
            connectionFactory: ConnectionFactory,
            persistenceProvider: PersistenceProvider,
            clientRepository: ClientRepository,
            transfer: Transfer,
            client: UClient,
            type: TransferItem.Type,
        ): FileTransferStarterTaskRegistry {
            val inetAddresses = clientRepository.getAddresses(client.clientUid).map { it.clientAddress }

            return FileTransferStarterTaskRegistry(
                connectionFactory, persistenceProvider, transfer, client, type, inetAddresses
            )
        }
    }*/
}
