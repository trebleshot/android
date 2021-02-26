package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "webClient")
class WebClient(
    @PrimaryKey val address: String,
    val title: String,
    val blocked: Boolean = false,
    val created: Long = System.currentTimeMillis(),
) : Parcelable