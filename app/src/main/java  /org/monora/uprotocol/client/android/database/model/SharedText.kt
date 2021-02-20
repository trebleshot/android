package org.monora.uprotocol.client.android.database.model;

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "text")
data class SharedText(
    @PrimaryKey
    val id: Int,
    val text: String,
    val client: DefaultClient,
)
