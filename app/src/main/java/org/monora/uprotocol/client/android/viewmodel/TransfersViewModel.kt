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

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.data.ClientRepository
import org.monora.uprotocol.client.android.data.TaskRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.client.android.task.transfer.TransferState
import org.monora.uprotocol.client.android.util.TAG
import org.monora.uprotocol.client.android.viewmodel.content.TransferStateContentViewModel
import javax.inject.Inject

@HiltViewModel
class TransfersViewModel @Inject internal constructor(
    private val clientRepository: ClientRepository,
    private val taskRepository: TaskRepository,
    private val transferRepository: TransferRepository,
) : ViewModel() {
    val transferDetails = transferRepository.getTransferDetails()

    suspend fun getTransfer(groupId: Long): Transfer? = transferRepository.getTransfer(groupId)

    suspend fun getClient(clientUid: String): UClient? = clientRepository.getDirect(clientUid)

    fun subscribe(transferDetail: TransferDetail) = taskRepository.subscribeToTask {
        if (it.params is TransferParams && it.params.transfer.id == transferDetail.id) it.params else null
    }.map {
        TransferStateContentViewModel.from(it)
    }
}