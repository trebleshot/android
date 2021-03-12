package org.monora.uprotocol.client.android.database

import androidx.lifecycle.LiveData
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.monora.uprotocol.client.android.database.model.SharedText

@Dao
interface SharedTextDao {
    @Delete
    suspend fun delete(sharedText: SharedText)

    @Query("SELECT * FROM sharedText ORDER BY created DESC")
    fun getAll(): LiveData<List<SharedText>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sharedText: SharedText)

    @Update
    suspend fun update(sharedText: SharedText)
}