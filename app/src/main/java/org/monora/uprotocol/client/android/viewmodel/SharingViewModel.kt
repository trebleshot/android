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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.monora.uprotocol.client.android.data.FileRepository
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.protocol.closeQuietly
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSeat
import org.monora.uprotocol.core.protocol.Direction
import javax.inject.Inject

@HiltViewModel
class SharingViewModel @Inject internal constructor(
    private val transportSeat: TransportSeat,
    private val transferRepository: TransferRepository,
    private val fileRepository: FileRepository,
) : ViewModel() {
    private var consumer: Job? = null

    private val _state = MutableLiveData<SharingState>()

    val state = liveData {
        emitSource(_state)
    }

    fun consume(bridge: CommunicationBridge, groupId: Long, contents: List<UTransferItem>) {
        if (consumer != null) return

        val transfer = Transfer(
            groupId,
            bridge.remoteClient.clientUid,
            Direction.Outgoing,
            fileRepository.appDirectory.originalUri.toString(),
        )

        consumer = viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.postValue(SharingState.Running)

                val result = bridge.requestFileTransfer(transfer.id, contents) {
                    runBlocking {
                        transferRepository.insert(transfer)
                    }
                }

                _state.postValue(SharingState.Success(transfer))

                if (result) {
                    transportSeat.beginFileTransfer(bridge, bridge.remoteClient, groupId, Direction.Outgoing)
                }
            } catch (e: Exception) {
                _state.postValue(SharingState.Error(e))
                e.printStackTrace()
                bridge.closeQuietly()
            } finally {
                consumer = null
            }
        }
    }
}

sealed class SharingState {
    object Running : SharingState()

    class Success(val transfer: Transfer) : SharingState()

    class Error(val exception: Exception) : SharingState()
}
