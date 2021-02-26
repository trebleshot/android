package org.monora.uprotocol.client.android.protocol

import android.content.Context
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientAddress
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject
import javax.inject.Singleton

class MainTransportSeat @Inject constructor(
    @ApplicationContext val context: Context,
    val persistenceProvider: PersistenceProvider,
): TransportSeat {
    override fun beginFileTransfer(
        bridge: CommunicationBridge?,
        client: Client,
        groupId: Long,
        type: TransferItem.Type
    ) {
        TODO("Not yet implemented")
    }

    override fun handleAcquaintanceRequest(client: Client, clientAddress: ClientAddress): Boolean {
        TODO("Not yet implemented")
    }

    override fun handleFileTransferRequest(client: Client, hasPin: Boolean, groupId: Long, jsonArray: String) {

    }

    override fun handleFileTransferState(client: Client, groupId: Long, isAccepted: Boolean) {
        TODO("Not yet implemented")
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