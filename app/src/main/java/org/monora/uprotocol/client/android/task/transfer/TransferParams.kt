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

package org.monora.uprotocol.client.android.task.transfer

import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Job
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.core.protocol.Client
import org.monora.uprotocol.core.transfer.TransferItem

data class TransferParams(
    val transfer: Transfer,
    val client: Client,
    var bytesTotal: Long,
    var bytesSessionTotal: Long = 0L
) {
    var averageSpeed = 0L

    var bytesOngoing = 0L

    var count = 0

    var job: Job? = null

    var lastFile: DocumentFile? = null

    var ongoing: TransferItem? = null

    val startTime = System.currentTimeMillis()
}
