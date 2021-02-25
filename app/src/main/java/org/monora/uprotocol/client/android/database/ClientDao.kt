package org.monora.uprotocol.client.android.database

import androidx.room.*
import org.monora.uprotocol.client.android.database.model.UClient

@Dao
interface ClientDao {
    @Delete
    fun delete(client: UClient)

    @Query("SELECT * FROM client WHERE uid == :uid LIMIT 1")
    fun get(uid: String): UClient?

    @Query("SELECT * FROM client")
    fun getAll(): List<UClient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg clients: UClient)

    @Update
    fun updateAll(vararg clients: UClient)
}