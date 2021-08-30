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

import android.net.Uri
import androidx.lifecycle.LiveData
import org.monora.uprotocol.client.android.database.WebTransferDao
import org.monora.uprotocol.client.android.database.model.WebTransfer
import org.monora.uprotocol.client.android.util.Networks
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebDataRepository @Inject constructor(
    private val webTransferDao: WebTransferDao,
) {
    private val sharedContents = mutableListOf<Any>()

    val isServing
        get() = sharedContents.isNotEmpty()

    fun clear() {
        synchronized(sharedContents) {
            sharedContents.clear()
        }
    }

    fun getList() = sharedContents.toList()

    fun getReceivedContent(id: Int): LiveData<WebTransfer> = webTransferDao.get(id)

    suspend fun getReceivedContent(uri: Uri): WebTransfer? = webTransferDao.get(uri)

    fun getReceivedContents() = webTransferDao.getAll()

    suspend fun insert(webTransfer: WebTransfer) = webTransferDao.insert(webTransfer)

    fun getNetworkInterfaces() = Networks.getInterfaces()

    suspend fun remove(transfer: WebTransfer) {
        webTransferDao.remove(transfer)
    }

    fun serve(list: List<Any>) {
        synchronized(sharedContents) {
            sharedContents.clear()
            sharedContents.addAll(list)
        }
    }
}
