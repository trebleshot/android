/*
 * Copyright (C) 2021 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.monora.uprotocol.client.android.database.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.*
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.client.android.model.ListItem

@Parcelize
@Entity(
    tableName = "sharedText",
    foreignKeys = [
        ForeignKey(
            entity = UClient::class, parentColumns = ["uid"], childColumns = ["clientUid"], onDelete = CASCADE
        ),
    ]
)
data class SharedText(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val clientUid: String?,
    var text: String,
    val created: Long = System.currentTimeMillis(),
    var modified: Long = created,
) : Parcelable, ListItem {
    @IgnoredOnParcel
    override val listId: Long
        get() = id.toLong() + created
}
