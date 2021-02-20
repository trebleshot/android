package org.monora.uprotocol.client.android.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.monora.uprotocol.client.android.database.model.TransferTarget

@Dao
interface TransferTargetDao {
    @Query("SELECT * FROM transferTarget")
    fun getAll(): List<TransferTarget>

    @Query("SELECT * FROM transferTarget WHERE transferId = :transferId")
    fun getTargetsForTransfer(transferId: Long): List<TransferTarget>

    @Query("SELECT * FROM transferTarget WHERE clientUid = :clientUid")
    fun getTargetsForClient(clientUid: String): List<TransferTarget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg transferTarget: TransferTarget)
}