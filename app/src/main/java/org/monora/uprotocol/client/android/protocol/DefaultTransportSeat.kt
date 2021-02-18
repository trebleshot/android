package org.monora.uprotocol.client.android.protocol

import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientAddress
import org.monora.uprotocol.core.transfer.TransferItem

class DefaultTransportSeat : TransportSeat {
    override fun beginFileTransfer(
        bridge: CommunicationBridge?,
        client: Client?,
        groupId: Long,
        type: TransferItem.Type?
    ) {
        TODO("Not yet implemented")
    }

    override fun handleAcquaintanceRequest(client: Client?, clientAddress: ClientAddress?): Boolean {
        TODO("Not yet implemented")
    }

    override fun handleFileTransferRequest(client: Client?, hasPin: Boolean, groupId: Long, jsonArray: String?) {
        TODO("Not yet implemented")
    }

    override fun handleFileTransferState(client: Client?, groupId: Long, isAccepted: Boolean) {
        TODO("Not yet implemented")
    }

    override fun handleTextTransfer(client: Client?, text: String?) {
        TODO("Not yet implemented")
    }

    override fun hasOngoingTransferFor(groupId: Long, clientUid: String?, type: TransferItem.Type?): Boolean {
        TODO("Not yet implemented")
    }

    override fun hasOngoingIndexingFor(groupId: Long): Boolean {
        TODO("Not yet implemented")
    }

    override fun notifyClientCredentialsChanged(client: Client?) {
        TODO("Not yet implemented")
    }
}