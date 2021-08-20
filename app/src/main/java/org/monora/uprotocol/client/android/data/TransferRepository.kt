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

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.database.TransferDao
import org.monora.uprotocol.client.android.database.TransferItemDao
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.core.protocol.Direction
import org.monora.uprotocol.core.transfer.TransferItem
import java.io.IOException
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val fileRepository: FileRepository,
    private val transferDao: TransferDao,
    private val transferItemDao: TransferItemDao,
) {
    private val context = WeakReference(context)

    suspend fun containsTransfer(groupId: Long) = transferDao.contains(groupId)

    suspend fun delete(transfer: Transfer) = transferDao.delete(transfer)

    suspend fun delete(transferItem: UTransferItem) = transferItemDao.delete(transferItem)

    fun getIncomingFile(transferItem: UTransferItem, transfer: Transfer): DocumentFile {
        val pseudoFile = getIncomingPseudoFile(transferItem, transfer, true)
        if (!pseudoFile.canWrite()) {
            throw IOException("File cannot be created or you don't have permission write to it")
        }
        return pseudoFile
    }

    private fun getIncomingPseudoFile(
        item: UTransferItem, transfer: Transfer, createIfNeeded: Boolean,
    ): DocumentFile = Files.createFileWithNestedPaths(
        context.get()!!,
        getTransferStorage(transfer),
        item.itemDirectory,
        item.itemMimeType,
        item.location,
        createIfNeeded
    )

    suspend fun getReceivable(groupId: Long) = transferItemDao.getReceivable(groupId)

    suspend fun getTransfer(groupId: Long): Transfer? = transferDao.get(groupId)

    fun getTransferDetail(groupId: Long): LiveData<TransferDetail> = transferDao.getDetail(groupId)

    fun getTransferDetailDirect(groupId: Long): TransferDetail? = transferDao.getDetailDirect(groupId)

    fun getTransferDetails(): LiveData<List<TransferDetail>> = transferDao.getDetails()

    suspend fun getTransferItem(
        groupId: Long,
        id: Long,
        direction: Direction,
    ): UTransferItem? = transferItemDao.get(groupId, id, direction)

    fun getTransferItems(groupId: Long) = transferItemDao.getAll(groupId)

    fun getTransferStorage(transfer: Transfer): DocumentFile {
        val defaultFolder = fileRepository.appDirectory
        val context = context.get()!!

        try {
            val saveLocation = DocumentFile.fromUri(context, Uri.parse(transfer.location), false)
            if (saveLocation.isDirectory() && saveLocation.canWrite()) {
                return saveLocation
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaultFolder
    }

    suspend fun insert(transfer: Transfer) = transferDao.insert(transfer)

    suspend fun insert(list: List<UTransferItem>) = transferItemDao.insert(list)


    fun saveReceivedFile(
        transfer: Transfer,
        currentFile: DocumentFile,
        transferItem: TransferItem,
    ): DocumentFile {
        val context = context.get()!!
        val savePath = getTransferStorage(transfer)
        val name = Files.getUniqueFileName(context, savePath, transferItem.itemName)
        val renamedFile = currentFile.renameTo(context, name) ?: throw IOException("Failed to rename: $currentFile")
        if (transferItem is UTransferItem) {
            transferItem.location = renamedFile.getUri().toString()
        }
        return renamedFile
    }

    suspend fun update(transfer: Transfer) = transferDao.update(transfer)

    suspend fun update(transferItem: UTransferItem) = transferItemDao.update(transferItem)
}
