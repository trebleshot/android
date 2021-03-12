package org.monora.uprotocol.client.android.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.monora.uprotocol.client.android.database.model.WebClient

@Dao
interface WebClientDao {
    @Query("SELECT * FROM webClient WHERE address = :address")
    suspend fun get(address: String): WebClient?

    @Insert
    suspend fun insert(webClient: WebClient)
}