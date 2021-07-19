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
import org.monora.uprotocol.client.android.database.model.UClient

@Dao
interface ClientDao {
    @Delete
    suspend fun delete(client: UClient)

    @Query("SELECT * FROM client WHERE uid = :uid LIMIT 1")
    suspend fun getSingle(uid: String): UClient?

    @Query("SELECT * FROM client WHERE uid = :uid LIMIT 1")
    fun get(uid: String): LiveData<UClient>

    @Query("SELECT * FROM client ORDER BY lastUsageTime DESC")
    fun getAll(): LiveData<List<UClient>>

    @Insert
    suspend fun insert(client: UClient)

    @Update
    suspend fun update(client: UClient)
}