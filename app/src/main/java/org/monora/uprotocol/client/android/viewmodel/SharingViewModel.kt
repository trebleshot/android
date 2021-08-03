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

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.core.CommunicationBridge
import java.net.ProtocolException
import javax.inject.Inject

@HiltViewModel
class SharingViewModel @Inject internal constructor(
    val transferRepository: TransferRepository,
) : ViewModel() {
    private var consumer: Job? = null

    private val _state = MutableLiveData<SharingState>(SharingState.Initial())

    val state = liveData {
        emitSource(_state)
    }

    fun consume(bridge: CommunicationBridge, transfer: Transfer, contents: List<UTransferItem>) {
        if (consumer != null) return

        consumer = viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.postValue(SharingState.Running)

                bridge.use {
                    val result = it.requestFileTransfer(transfer.id, contents) {
                        runBlocking {
                            transferRepository.insert(transfer)
                        }
                    }

                    if (result) {
                        _state.postValue(SharingState.Success(transfer))
                    } else {
                        throw ProtocolException("Unexpected result")
                    }
                }
            } catch (e: Exception) {
                _state.postValue(SharingState.Error(e))
            } finally {
                consumer = null
            }
        }
    }
}

sealed class SharingState {
    class Initial : SharingState() {
        private var consumed: Boolean = false

        fun consume(): Boolean = if (consumed) false else {
            consumed = true
            true
        }
    }

    object Running : SharingState()

    class Success(val transfer: Transfer) : SharingState()

    class Error(val exception: Exception) : SharingState()
}
