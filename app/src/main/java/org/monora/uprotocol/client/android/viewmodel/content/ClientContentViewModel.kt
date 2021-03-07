package org.monora.uprotocol.client.android.viewmodel.content

import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.core.protocol.Client

class ClientContentViewModel(client: UClient) {
    val client: Client = client

    val nickname = client.clientNickname

    val clientType = client.clientType.name
}