package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.core.transfer.TransferItem

@Parcelize
@Entity(tableName = "transfer")
data class Transfer(
    @PrimaryKey
    val id: Long,
    val type: TransferItem.Type,
    var location: String,
    var web: Boolean = false,
    var dateCreated: Long = System.currentTimeMillis(),
) : Parcelable