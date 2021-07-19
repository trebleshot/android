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

package org.monora.uprotocol.client.android.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.core.persistence.PersistenceProvider.STATE_PENDING
import org.monora.uprotocol.core.transfer.TransferItem

@Dao
interface TransferItemDao {
    @Delete
    suspend fun delete(transferItem: UTransferItem)

    @Query("SELECT * FROM transferItem WHERE groupId = :groupId ORDER BY name")
    fun getAll(groupId: Long): LiveData<List<UTransferItem>>

    @Query("SELECT * FROM transferItem WHERE groupId = :groupId AND state == $STATE_PENDING ORDER BY name LIMIT 1")
    suspend fun getReceivable(groupId: Long): UTransferItem?

    @Query("SELECT * FROM transferItem WHERE groupId = :groupId AND id = :id AND type = :type LIMIT 1")
    suspend fun get(groupId: Long, id: Long, type: TransferItem.Type): UTransferItem?

    @Query("SELECT * FROM transferItem WHERE location = :location AND type = :type")
    suspend fun get(location: String, type: TransferItem.Type): UTransferItem?

    @Insert
    suspend fun insert(list: List<UTransferItem>)

    @Update
    suspend fun update(transferItem: UTransferItem)

    @Query("UPDATE transferItem SET state = $STATE_PENDING WHERE groupId = :groupId")
    suspend fun updateTemporaryFailuresAsPending(groupId: Long)
}