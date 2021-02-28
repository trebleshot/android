package org.monora.uprotocol.client.android.protocol

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.database.AppDatabase
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
    val connectionFactory: ConnectionFactory,
    val persistenceProvider: PersistenceProvider,
    val appDatabase: AppDatabase,
    val backgroundBackend: BackgroundBackend,
) : TransportSeat {
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
        if (client !is UClient) throw UnsupportedOperationException("Expected the UClient implementation")

        backgroundBackend.run(
            IndexTransferTask(
                connectionFactory, persistenceProvider, appDatabase, groupId, jsonArray, client, hasPin
            )
        )
    }

    override fun handleFileTransferState(client: Client, groupId: Long, isAccepted: Boolean) {
        if (!isAccepted) {

        }
    }

    override fun handleTextTransfer(client: Client, text: String) {
        TODO("Not yet implemented")
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