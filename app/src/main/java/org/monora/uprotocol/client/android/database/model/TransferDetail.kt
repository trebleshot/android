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

import androidx.room.DatabaseView
import org.monora.uprotocol.client.android.model.ListItem
import org.monora.uprotocol.core.protocol.Direction
import org.monora.uprotocol.core.transfer.TransferItem.State.Constants.DONE

@DatabaseView(
    viewName = "transferDetail",
    value = "SELECT transfer.id, transfer.location, transfer.clientUid, transfer.direction, transfer.dateCreated, " +
            "transfer.accepted, client.nickname AS clientNickname, COUNT(items.id) AS itemsCount, " +
            "COUNT(CASE WHEN items.state == '$DONE' THEN items.id END) as itemsDoneCount, SUM(items.size) AS size, " +
            "SUM(CASE WHEN items.state == '$DONE' THEN items.size END) as sizeOfDone FROM transfer " +
            "INNER JOIN client ON client.uid = transfer.clientUid " +
            "INNER JOIN transferItem items ON items.groupId = transfer.id GROUP BY items.groupId"
)
data class TransferDetail(
    val id: Long,
    val clientUid: String,
    val clientNickname: String,
    val location: String,
    val direction: Direction,
    val size: Long,
    val accepted: Boolean,
    val sizeOfDone: Long,
    val itemsCount: Int,
    val itemsDoneCount: Int,
    val dateCreated: Long,
) : ListItem {
    override val listId: Long
        get() = id + javaClass.hashCode()
}
