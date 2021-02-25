package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientType
import java.security.cert.X509Certificate

@Parcelize
@Entity(tableName = "client")
data class UClient(
    @PrimaryKey
    var uid: String,
    var nickname: String,
    var manufacturer: String,
    var product: String,
    var type: ClientType,
    var versionName: String,
    var versionCode: Int,
    var protocolVersion: Int,
    var protocolVersionMin: Int,
    var lastUsageTime: Long,
    var blocked: Boolean,
    var local: Boolean,
    var trusted: Boolean,
    var certificate: X509Certificate?,
) : Client, Parcelable {
    override fun getClientCertificate(): X509Certificate? = certificate

    override fun getClientLastUsageTime(): Long = lastUsageTime

    override fun getClientManufacturer(): String = manufacturer

    override fun getClientNickname(): String = nickname

    override fun getClientProduct(): String = product

    override fun getClientProtocolVersion(): Int = protocolVersion

    override fun getClientProtocolVersionMin(): Int = protocolVersionMin

    override fun getClientType(): ClientType = type

    override fun getClientUid(): String = uid

    override fun getClientVersionCode(): Int = versionCode

    override fun getClientVersionName(): String = versionName

    override fun isClientBlocked(): Boolean = blocked

    override fun isClientLocal(): Boolean = local

    override fun isClientTrusted(): Boolean = trusted

    override fun setClientBlocked(blocked: Boolean) {
        this.blocked = blocked
    }

    override fun setClientCertificate(certificate: X509Certificate?) {
        this.certificate = certificate
    }

    override fun setClientLastUsageTime(lastUsageTime: Long) {
        this.lastUsageTime = lastUsageTime
    }

    override fun setClientLocal(local: Boolean) {
        this.local = local
    }

    override fun setClientManufacturer(manufacturer: String) {
        this.manufacturer = manufacturer
    }

    override fun setClientNickname(nickname: String) {
        this.nickname = nickname
    }

    override fun setClientProduct(product: String) {
        this.product = product
    }

    override fun setClientProtocolVersion(protocolVersion: Int) {
        this.protocolVersion = protocolVersion
    }

    override fun setClientProtocolVersionMin(protocolVersionMin: Int) {
        this.protocolVersionMin = protocolVersionMin
    }

    override fun setClientTrusted(trusted: Boolean) {
        this.trusted = trusted
    }

    override fun setClientType(type: ClientType) {
        this.type = type
    }

    override fun setClientUid(uid: String) {
        this.uid = uid
    }

    override fun setClientVersionCode(versionCode: Int) {
        this.versionCode = versionCode
    }

    override fun setClientVersionName(versionName: String) {
        this.versionName = versionName
    }
}