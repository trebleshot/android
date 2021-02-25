package org.monora.uprotocol.client.android.database.model;

import android.os.Parcelable
import androidx.room.*
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.core.transfer.TransferItem


@Parcelize
@Entity(
    tableName = "transferTarget",
    foreignKeys = [
        ForeignKey(entity = UClient::class, parentColumns = ["uid"], childColumns = ["clientUid"]),
        ForeignKey(entity = Transfer::class, parentColumns = ["id"], childColumns = ["groupId"])
    ]
)
data class TransferTarget(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    @ColumnInfo(index = true)
    val clientUid: String,
    @ColumnInfo(index = true)
    val groupId: Long,
    val type: TransferItem.Type,
) : Parcelable
