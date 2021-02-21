package org.monora.uprotocol.client.android.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import org.monora.uprotocol.client.android.database.model.SharedTextModel

@Dao
interface SharedTextDao {
    @Delete
    fun delete(sharedTextModel: SharedTextModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg sharedTextModel: SharedTextModel)
}