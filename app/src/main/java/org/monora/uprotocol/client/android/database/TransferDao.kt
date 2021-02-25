package org.monora.uprotocol.client.android.database;

import androidx.room.*
import org.monora.uprotocol.client.android.database.model.Transfer;

@Dao
interface TransferDao {
    @Query("UPDATE transfer SET web = 0")
    suspend fun hideTransfersFromWeb()

    @Query("SELECT * FROM transfer WHERE id = :transferId")
    fun get(transferId: Long): Transfer?

    @Query("SELECT * FROM transfer")
    fun getAll(): List<Transfer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg transfers: Transfer)

    @Update
    suspend fun update(transfer: Transfer)
}
