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
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.monora.uprotocol.client.android.database.model.SharedText

@Dao
interface SharedTextDao {
    @Delete
    suspend fun delete(sharedText: SharedText)

    @Query("SELECT * FROM sharedText ORDER BY created DESC")
    fun getAll(): LiveData<List<SharedText>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sharedText: SharedText)

    @Update
    suspend fun update(sharedText: SharedText)
}