package org.monora.uprotocol.client.android.database

import androidx.room.*
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.core.protocol.ClientAddress

@Dao
interface ClientAddressDao {
    @Delete
    fun delete(user: UClientAddress)

    @Query("SELECT * FROM clientAddress")
    fun getAll(): List<UClientAddress>

    @Query("SELECT * FROM clientAddress WHERE clientUid = :clientUid")
    suspend fun getAll(clientUid: String): List<UClientAddress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: UClientAddress)

    @Update
    fun updateAll(vararg users: UClientAddress)
}