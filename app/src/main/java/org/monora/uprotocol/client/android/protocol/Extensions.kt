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
package org.monora.uprotocol.client.android.protocol

import android.content.Context
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.runBlocking
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.backend.TaskRegistry
import org.monora.uprotocol.client.android.data.TaskRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.Transfers
import java.net.ProtocolException

val TransferItem.Type.isIncoming
    get() = this == TransferItem.Type.Incoming

fun CommunicationBridge.startTransfer(
    backend: Backend,
    transferRepository: TransferRepository,
    params: TransferParams,
    state: MutableLiveData<Task.State>
) {
    if (requestFileTransferStart(params.transfer.id, params.transfer.type)) {
        runFileTransfer(this, backend, transferRepository, params, state)
    } else {
        throw ProtocolException("Remote rejected the request without providing the cause.")
    }
}

fun CommunicationBridge.rejectTransfer(transferRepository: TransferRepository, transfer: Transfer): Boolean {
    if (requestNotifyTransferRejection(transfer.id)) {
        runBlocking {
            transferRepository.delete(transfer)
        }
        return true
    }
    return false
}

fun runFileTransfer(
    bridge: CommunicationBridge,
    backend: Backend,
    transferRepository: TransferRepository,
    params: TransferParams,
    state: MutableLiveData<Task.State>,
) {
    if (!params.transfer.accepted) {
        params.transfer.accepted = true
        runBlocking {
            transferRepository.update(params.transfer)
        }
    }

    val operation = MainTransferOperation(backend, transferRepository, params, state)

    if (params.transfer.type.isIncoming) {
        Transfers.receive(bridge, operation, params.transfer.id)
    } else {
        Transfers.send(bridge, operation, params.transfer.id)
    }
}