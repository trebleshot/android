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
    fun containsTransfer(groupId: Long) = transferDao.contains(groupId)

    fun delete(transferItem: UTransferItem) = transferItemDao.delete(transferItem)

    fun getReceivable(groupId: Long) = transferItemDao.getReceivable(groupId)

    fun getTransfer(transferId: Long): Transfer? = transferDao.get(transferId)

    fun getTransfer(
        transferId: Long,
        clientUid: String,
        type: TransferItem.Type,
    ): Transfer? = transferDao.get(transferId, clientUid, type)

    fun getTransferItem(groupId: Long, id: Long) = transferItemDao.get(groupId, id)

    fun getTransferItem(groupId: Long, id: Long, type: TransferItem.Type) = transferItemDao.get(groupId, id, type)

    fun getTransferItem(location: String, type: TransferItem.Type) = transferItemDao.get(location, type)

    fun getTransfers() = transferDao.getAll()

    suspend fun hideTransfersFromWeb() = transferDao.hideTransfersFromWeb()

    fun insert(transfer: Transfer) = transferDao.insert(transfer)

    fun insert(list: List<UTransferItem>) = transferItemDao.insertAll(list)

    suspend fun update(transfer: Transfer) = transferDao.update(transfer)

    fun update(transferItem: UTransferItem) = transferItemDao.update(transferItem)

    fun updateTemporaryFailuresAsPending(groupId: Long) = transferItemDao.updateTemporaryFailuresAsPending(groupId)
}