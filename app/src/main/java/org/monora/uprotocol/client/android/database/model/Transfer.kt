package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.core.transfer.TransferItem

@Parcelize
@Entity(
    tableName = "transfer",
    foreignKeys = [
        ForeignKey(
            entity = UClient::class, parentColumns = ["uid"], childColumns = ["clientUid"], onDelete = CASCADE
        ),
    ]
)
data class Transfer(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(index = true)
    val clientUid: String,
    val type: TransferItem.Type,
    var location: String,
    var web: Boolean = false,
    val dateCreated: Long = System.currentTimeMillis(),
) : Parcelable