package org.monora.uprotocol.client.android.data

import org.monora.uprotocol.client.android.database.TransferDao
import org.monora.uprotocol.client.android.database.TransferItemDao
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransferRepository @Inject constructor(
    private val transferDao: TransferDao,
    private val transferItemDao: TransferItemDao,
) {
    suspend fun containsTransfer(groupId: Long) = transferDao.contains(groupId)

    suspend fun delete(transferItem: UTransferItem) = transferItemDao.delete(transferItem)

    suspend fun getReceivable(groupId: Long) = transferItemDao.getReceivable(groupId)

    suspend fun getTransfer(transferId: Long): Transfer? = transferDao.get(transferId)

    suspend fun getTransfer(
        transferId: Long,
        clientUid: String,
        type: TransferItem.Type,
    ): Transfer? = transferDao.get(transferId, clientUid, type)

    suspend fun getTransferItem(groupId: Long, id: Long) = transferItemDao.get(groupId, id)

    suspend fun getTransferItem(
        groupId: Long,
        id: Long,
        type: TransferItem.Type,
    ) = transferItemDao.get(groupId, id, type)

    suspend fun getTransferItem(location: String, type: TransferItem.Type) = transferItemDao.get(location, type)

    fun getTransfers() = transferDao.getAll()

    suspend fun hideTransfersFromWeb() = transferDao.hideTransfersFromWeb()

    suspend fun insert(transfer: Transfer) = transferDao.insert(transfer)

    suspend fun insert(list: List<UTransferItem>) = transferItemDao.insert(list)

    suspend fun update(transfer: Transfer) = transferDao.update(transfer)

    suspend fun update(transferItem: UTransferItem) = transferItemDao.update(transferItem)

    suspend fun updateTemporaryFailuresAsPending(
        groupId: Long,
    ) = transferItemDao.updateTemporaryFailuresAsPending(groupId)
}