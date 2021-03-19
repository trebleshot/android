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

package org.monora.uprotocol.client.android.model

import android.os.Parcelable
import androidx.room.Ignore
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel

@Parcelize
data class AudioMediaModel(
    val id: Long,
    val artist: String,
    val song: String,
    val folder: String,
    val album: String,
) : ContentModel, Parcelable {
    @IgnoredOnParcel
    @Ignore
    private var selected = false

    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canRemove(): Boolean = false

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        TODO("Not yet implemented")
    }

    override fun dateCreated(): Long {
        TODO("Not yet implemented")
    }

    override fun dateModified(): Long {
        TODO("Not yet implemented")
    }

    override fun dateSupported(): Boolean = false

    override fun filter(charSequence: CharSequence): Boolean = false

    override fun id(): Long = id

    override fun length(): Long {
        TODO("Not yet implemented")
    }

    override fun lengthSupported(): Boolean {
        TODO("Not yet implemented")
    }

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        TODO("Not yet implemented")
    }

    override fun remove(operationBackend: OperationBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun canSelect(): Boolean {
        TODO("Not yet implemented")
    }

    override fun selected(): Boolean {
        TODO("Not yet implemented")
    }

    override fun select(selected: Boolean) {
        TODO("Not yet implemented")
    }

    override fun name(): String {
        TODO("Not yet implemented")
    }
}