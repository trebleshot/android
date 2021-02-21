/*
 * Copyright (C) 2019 Veli Tasalı
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
package com.genonbeta.TrebleShot.model

import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel
import java.util.*

class LoadedMember : TransferMember, ContentModel {
    lateinit var device: Device

    private var selected = false

    constructor()

    constructor(transferId: Long, deviceId: String, type: TransferItem.Type) : super(transferId, deviceId, type)

    override fun name(): String = device.username

    override fun canCopy(): Boolean = false

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = false

    override fun canRemove(): Boolean = true

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun dateCreated(): Long = device.lastUsageTime

    override fun dateModified(): Long = device.lastUsageTime

    override fun dateSupported(): Boolean = true

    override fun filter(charSequence: CharSequence): Boolean = false

    override fun id(): Long = transferId

    override fun length(): Long = throw UnsupportedOperationException()

    override fun lengthSupported(): Boolean = false

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun remove(operationBackend: OperationBackend): Boolean {
        TODO("Not yet implemented")
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        throw UnsupportedOperationException()
    }

    override fun canSelect(): Boolean = true

    override fun selected(): Boolean = false

    override fun select(selected: Boolean) {
        this.selected = selected
    }
}