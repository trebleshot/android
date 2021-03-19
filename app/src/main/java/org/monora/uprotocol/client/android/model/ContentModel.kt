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

import com.genonbeta.android.framework.util.actionperformer.SelectionModel
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend

interface ContentModel : SelectionModel {
    fun canCopy(): Boolean

    fun canMove(): Boolean

    fun canShare(): Boolean

    fun canRemove(): Boolean

    fun canRename(): Boolean

    fun copy(operationBackend: OperationBackend, destination: Destination): Boolean

    fun dateCreated(): Long

    fun dateModified(): Long

    fun dateSupported(): Boolean

    fun filter(charSequence: CharSequence): Boolean

    fun id(): Long

    fun length(): Long

    fun lengthSupported(): Boolean

    fun move(operationBackend: OperationBackend, destination: Destination): Boolean

    fun remove(operationBackend: OperationBackend): Boolean

    fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean
}