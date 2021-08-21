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

package org.monora.uprotocol.client.android.backend

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.protocol.Direction
import javax.inject.Inject
import javax.inject.Singleton

typealias AcquaintanceCallback = (bridge: CommunicationBridge) -> Unit

typealias TransferRequestCallback = (transfer: Transfer, hasPin: Boolean) -> Unit

@Singleton
class TransportRegistry @Inject constructor(
    private val backend: Backend,
) {
    private var guidanceRequest: GuidanceLifecycleObserver? = null

    private var transferRequest: TransferRequestLifecycleObserver? = null

    fun handleGuidanceRequest(bridge: CommunicationBridge, direction: Direction) {
        val guidanceRequest = guidanceRequest

        if (guidanceRequest != null && guidanceRequest.direction != direction && guidanceRequest.enabled) {
            bridge.activeConnection.isRoaming = true
            backend.applicationScope.launch(Dispatchers.Main) {
                guidanceRequest.callback(bridge)
            }
        } else {
            bridge.send(false)
        }
    }

    fun handleTransferRequest(transfer: Transfer, hasPin: Boolean): Boolean {
        val transferRequest = transferRequest

        if (transferRequest != null && transferRequest.enabled) {
            backend.applicationScope.launch(Dispatchers.Main) {
                transferRequest.callback(transfer, hasPin)
            }
            return true
        }

        return false
    }

    fun registerForGuidanceRequests(
        lifecycleOwner: LifecycleOwner,
        direction: Direction,
        callback: AcquaintanceCallback
    ) {
        lifecycleOwner.lifecycle.addObserver(
            GuidanceLifecycleObserver(lifecycleOwner, direction, callback).also { guidanceRequest = it }
        )
    }

    fun registerForTransferRequests(
        lifecycleOwner: LifecycleOwner,
        callback: TransferRequestCallback,
    ) {
        lifecycleOwner.lifecycle.addObserver(
            TransferRequestLifecycleObserver(lifecycleOwner, callback).also { transferRequest = it }
        )
    }

    internal class TransferRequestLifecycleObserver(
        lifecycleOwner: LifecycleOwner,
        val callback: TransferRequestCallback,
    ) : HandlerLifecycleObserver(lifecycleOwner)

    internal class GuidanceLifecycleObserver(
        lifecycleOwner: LifecycleOwner,
        val direction: Direction,
        val callback: AcquaintanceCallback,
    ) : HandlerLifecycleObserver(lifecycleOwner)

    internal open class HandlerLifecycleObserver(
        private val lifecycleOwner: LifecycleOwner
    ) : LifecycleObserver {
        var enabled = false
            private set

        var destroyed = false
            private set

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        fun start() {
            enabled = true
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        fun stop() {
            enabled = false
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun destroy() {
            lifecycleOwner.lifecycle.removeObserver(this)
            destroyed = true
        }
    }
}
