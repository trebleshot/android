package org.monora.uprotocol.client.android.database

import androidx.lifecycle.LiveData
import androidx.room.*
import org.monora.uprotocol.client.android.database.model.UClient

@Dao
interface ClientDao {
    @Delete
    fun delete(client: UClient)

    @Query("SELECT * FROM client WHERE uid == :uid LIMIT 1")
    fun get(uid: String): UClient?

    @Query("SELECT * FROM client")
    fun getAll(): LiveData<List<UClient>>

    @Insert
    fun insert(client: UClient)

    @Update
    fun update(client: UClient)
}