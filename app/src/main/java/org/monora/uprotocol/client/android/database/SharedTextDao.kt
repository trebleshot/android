package org.monora.uprotocol.client.android.database

import androidx.lifecycle.LiveData
import androidx.room.*
import org.monora.uprotocol.client.android.database.model.SharedTextModel

@Dao
interface SharedTextDao {
    @Delete
    fun delete(sharedTextModel: SharedTextModel)

    @Query("SELECT * FROM sharedText")
    fun getAll(): LiveData<List<SharedTextModel>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sharedTextModel: SharedTextModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg sharedTextModel: SharedTextModel)
}