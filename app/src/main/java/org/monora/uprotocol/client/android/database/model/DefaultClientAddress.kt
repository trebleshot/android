package org.monora.uprotocol.client.android.database.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import org.monora.uprotocol.core.protocol.ClientAddress
import java.net.InetAddress

@Entity(tableName = "clientAddress")
data class DefaultClientAddress(
    @PrimaryKey
    var inetAddress: InetAddress,
    var clientUid: String,
    var lastUsageTime: Long,
) : ClientAddress {
    override fun getClientAddress(): InetAddress = inetAddress

    override fun getClientAddressLastUsageTime(): Long = lastUsageTime

    override fun getClientAddressOwnerUid(): String = clientUid

    override fun setClientAddress(inetAddress: InetAddress) {
        this.inetAddress = inetAddress
    }

    override fun setClientAddressLastUsageTime(lastUsageTime: Long) {
        this.lastUsageTime = lastUsageTime
    }

    override fun setClientAddressOwnerUid(clientUid: String) {
        this.clientUid = clientUid
    }
}