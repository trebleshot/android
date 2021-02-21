package org.monora.uprotocol.client.android.database

import androidx.room.*
import org.monora.uprotocol.client.android.database.model.DefaultClientAddress

@Dao
interface ClientAddressDao {
    @Delete
    fun delete(user: DefaultClientAddress)

    @Query("SELECT * FROM clientAddress")
    fun getAll(): List<DefaultClientAddress>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg users: DefaultClientAddress)

    @Update
    fun updateAll(vararg users: DefaultClientAddress)
}