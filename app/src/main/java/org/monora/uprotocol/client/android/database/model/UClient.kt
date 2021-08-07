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

package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.protocol.ClientType
import java.io.FileInputStream
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
    var revisionOfPicture: Long,
    var lastUsageTime: Long = System.currentTimeMillis(),
    var blocked: Boolean = false,
    var local: Boolean = false,
    var trusted: Boolean = false,
    var certificate: X509Certificate? = null,
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

    override fun getClientRevisionOfPicture(): Long = revisionOfPicture

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

    override fun setClientRevisionOfPicture(revision: Long) {
        this.revisionOfPicture = revision
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
