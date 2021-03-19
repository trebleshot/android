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

import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import java.net.NetworkInterface

class NetworkInterfaceModel(
    val networkInterface: NetworkInterface,
    val name: String,
) : ContentModel {
    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canSelect(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canRemove(): Boolean = false

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun dateCreated(): Long = throw UnsupportedOperationException()

    override fun dateModified(): Long = throw UnsupportedOperationException()

    override fun dateSupported(): Boolean = false

    override fun filter(charSequence: CharSequence): Boolean = networkInterface.displayName.contains(charSequence)
            && name.contains(charSequence)

    override fun id(): Long = networkInterface.hashCode().toLong()

    override fun length(): Long = throw UnsupportedOperationException()

    override fun lengthSupported(): Boolean = false

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun name(): String = name

    override fun remove(operationBackend: OperationBackend): Boolean {
        throw UnsupportedOperationException()
    }

    override fun selected(): Boolean = false

    override fun select(selected: Boolean) {

    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        throw UnsupportedOperationException()
    }
}