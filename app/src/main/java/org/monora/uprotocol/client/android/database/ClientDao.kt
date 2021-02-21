package org.monora.uprotocol.client.android.database

import androidx.room.*
import org.monora.uprotocol.client.android.database.model.DefaultClient

@Dao
interface ClientDao {
    @Delete
    fun delete(client: DefaultClient)

    @Query("SELECT * FROM client WHERE uid == :uid LIMIT 1")
    fun get(uid: String): DefaultClient?

    @Query("SELECT * FROM client")
    fun getAll(): List<DefaultClient>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg clients: DefaultClient)

    @Update
    fun updateAll(vararg clients: DefaultClient)
}