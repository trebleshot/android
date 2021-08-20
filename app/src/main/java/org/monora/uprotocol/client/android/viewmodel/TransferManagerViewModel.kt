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

package org.monora.uprotocol.client.android.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.data.TransferTaskRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.protocol.Client
import javax.inject.Inject

@HiltViewModel
class TransferManagerViewModel @Inject internal constructor(
    private val transferTaskRepository: TransferTaskRepository,
) : ViewModel() {
    fun rejectTransferRequest(transfer: Transfer, client: Client) {
        transferTaskRepository.rejectTransfer(transfer, client)
    }

    fun toggleTransferOperation(transfer: Transfer, client: Client, detail: TransferDetail) {
        transferTaskRepository.toggleTransferOperation(transfer, client, detail)
    }
}
