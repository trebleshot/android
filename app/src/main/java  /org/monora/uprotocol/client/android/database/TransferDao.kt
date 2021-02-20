package org.monora.uprotocol.client.android.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query
import org.monora.uprotocol.client.android.database.model.Transfer;

@Dao
interface TransferDao
{
    @Query("SELECT * FROM transfer")
    fun getAll(): List<Transfer>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg transfers: Transfer)
}
