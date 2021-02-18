package org.monora.uprotocol.client.android.database.model;

import androidx.room.*
import org.monora.uprotocol.core.transfer.TransferItem

@Entity(
    tableName = "transferTarget",
    foreignKeys = [
        ForeignKey(entity = DefaultClient::class, parentColumns = ["uid"], childColumns = ["clientUid"]),
        ForeignKey(entity = Transfer::class, parentColumns = ["id"], childColumns = ["transferId"])
    ]
)
data class TransferTarget(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(index = true)
    val clientUid: String,
    @ColumnInfo(index = true)
    val transferId: Long,
    val type: TransferItem.Type,
)
