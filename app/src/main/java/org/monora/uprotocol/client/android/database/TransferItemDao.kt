package org.monora.uprotocol.client.android.database

import androidx.room.*
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.core.persistence.PersistenceProvider.STATE_PENDING
import org.monora.uprotocol.core.transfer.TransferItem

@Dao
interface TransferItemDao {
    @Delete
    fun delete(transferItem: UTransferItem)

    @Query("SELECT * FROM transferItem WHERE groupId == :groupId AND id == :id LIMIT 1")
    fun get(groupId: Long, id: Long): UTransferItem?

    @Query("SELECT * FROM transferItem WHERE groupId == :groupId AND state == $STATE_PENDING ORDER BY name LIMIT 1")
    fun getReceivable(groupId: Long): UTransferItem?

    @Query("SELECT * FROM transferItem WHERE groupId == :groupId AND id == :id AND type == :type LIMIT 1")
    fun get(groupId: Long, id: Long, type: TransferItem.Type): UTransferItem?

    @Query("SELECT * FROM transferItem WHERE location = :location AND type = :type")
    fun get(location: String, type: TransferItem.Type): UTransferItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg transfers: UTransferItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<UTransferItem>)

    @Query("UPDATE transferItem SET state = $STATE_PENDING WHERE groupId = :groupId")
    fun updateTemporaryFailuresAsPending(groupId: Long)
}