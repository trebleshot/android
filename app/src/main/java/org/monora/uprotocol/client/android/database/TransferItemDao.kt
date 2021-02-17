package org.monora.uprotocol.client.android.database

import androidx.room.*
import org.monora.uprotocol.client.android.model.DefaultTransferItem
import org.monora.uprotocol.core.persistence.PersistenceProvider.STATE_PENDING
import org.monora.uprotocol.core.transfer.TransferItem

@Dao
interface TransferItemDao {
    @Query("SELECT EXISTS(SELECT * FROM DefaultTransferItem WHERE groupId == :groupId)")
    fun contains(groupId: Long): Boolean

    @Delete
    fun delete(transferItem: DefaultTransferItem)

    @Query("SELECT * FROM DefaultTransferItem WHERE groupId == :groupId AND id == :id LIMIT 1")
    fun get(groupId: Long, id: Long): DefaultTransferItem?

    @Query(
        "SELECT * FROM DefaultTransferItem WHERE groupId == :groupId AND state == $STATE_PENDING ORDER BY " +
                "name LIMIT 1"
    )
    fun getReceivable(groupId: Long): DefaultTransferItem?

    // TODO: 1/15/21 Remove the uid
    @Query("SELECT * FROM DefaultTransferItem WHERE groupId == :groupId AND id == :id AND type == :type LIMIT 1")
    fun get(groupId: Long, id: Long, type: TransferItem.Type): DefaultTransferItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(vararg clients: DefaultTransferItem)
}