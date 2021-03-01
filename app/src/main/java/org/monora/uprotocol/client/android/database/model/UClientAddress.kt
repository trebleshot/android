package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.core.protocol.ClientAddress
import java.net.InetAddress

@Parcelize
@Entity(
    tableName = "clientAddress",
    foreignKeys = [
        ForeignKey(
            entity = UClient::class, parentColumns = ["uid"], childColumns = ["clientUid"], onDelete = CASCADE
        ),
    ]
)
data class UClientAddress(
    @PrimaryKey
    var inetAddress: InetAddress,
    var clientUid: String,
    var lastUsageTime: Long = System.currentTimeMillis(),
) : ClientAddress, Parcelable {
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