package org.monora.uprotocol.client.android.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.monora.uprotocol.client.android.database.model.SharedText

@Dao
interface SharedTextDao {
    @Delete
    suspend fun delete(sharedText: SharedText)

    @Query("SELECT * FROM sharedText ORDER BY created DESC")
    fun getAll(): Flow<List<SharedText>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sharedText: SharedText)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg sharedText: SharedText)

    @Update
    suspend fun update(sharedText: SharedText)
}