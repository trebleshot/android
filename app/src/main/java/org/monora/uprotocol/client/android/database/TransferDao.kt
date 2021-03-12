package org.monora.uprotocol.client.android.database;

import androidx.lifecycle.LiveData
import androidx.room.*
import org.monora.uprotocol.client.android.database.model.Transfer;
import org.monora.uprotocol.core.transfer.TransferItem

@Dao
interface TransferDao {
    @Query("SELECT EXISTS(SELECT * FROM transfer WHERE id == :groupId)")
    suspend fun contains(groupId: Long): Boolean

    @Query("SELECT * FROM transfer WHERE id = :transferId")
    suspend fun get(transferId: Long): Transfer?

    @Query("SELECT * FROM transfer WHERE id = :transferId AND clientUid = :clientUid AND type = :type")
    suspend fun get(transferId: Long, clientUid: String, type: TransferItem.Type): Transfer?

    @Query("SELECT * FROM transfer")
    fun getAll(): LiveData<List<Transfer>>

    @Query("UPDATE transfer SET web = 0")
    suspend fun hideTransfersFromWeb()

    @Insert
    suspend fun insert(transfer: Transfer)

    @Update
    suspend fun update(transfer: Transfer)
}
