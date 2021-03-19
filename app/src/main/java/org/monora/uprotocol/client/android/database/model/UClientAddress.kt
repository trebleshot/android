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
import androidx.room.ColumnInfo
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
    @ColumnInfo(index = true)
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