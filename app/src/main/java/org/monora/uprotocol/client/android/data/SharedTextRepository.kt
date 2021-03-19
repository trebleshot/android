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

package org.monora.uprotocol.client.android.data

import androidx.lifecycle.LiveData
import org.monora.uprotocol.client.android.database.SharedTextDao
import org.monora.uprotocol.client.android.database.model.SharedText
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedTextRepository @Inject constructor(
    private val sharedTextDao: SharedTextDao,
) {
    suspend fun delete(sharedText: SharedText) = sharedTextDao.delete(sharedText)

    fun getSharedTexts(): LiveData<List<SharedText>> = sharedTextDao.getAll()

    suspend fun insert(sharedText: SharedText) = sharedTextDao.insert(sharedText)

    suspend fun update(sharedText: SharedText) = sharedTextDao.update(sharedText)
}