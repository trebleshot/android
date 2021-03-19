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
import androidx.core.util.ObjectsCompat
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel
import retrofit2.http.Field

@Parcelize
@Entity(tableName = "sharedText")
data class SharedText(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    var text: String,
    val created: Long = System.currentTimeMillis(),
    var modified: Long = created,
) : ContentModel, Parcelable {
    @IgnoredOnParcel
    @Ignore
    var selected: Boolean = false

    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canSelect(): Boolean = true

    override fun canRemove(): Boolean = true

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun dateCreated(): Long = created

    override fun dateModified(): Long = modified

    override fun dateSupported(): Boolean = true

    override fun filter(charSequence: CharSequence): Boolean = text.contains(charSequence)

    override fun id(): Long = ObjectsCompat.hash(id, created).toLong()

    override fun length(): Long = throw UnsupportedOperationException()

    override fun lengthSupported(): Boolean = false

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun name(): String = this.text

    override fun remove(operationBackend: OperationBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun selected(): Boolean = selected

    override fun select(selected: Boolean) {
        this.selected = selected
    }
}