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
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.protocol.Direction
import javax.inject.Inject
import javax.inject.Singleton

typealias AcquaintanceCallback = (bridge: CommunicationBridge) -> Unit

@Singleton
class TransportRegistry @Inject constructor(
    private val backend: Backend,
) {
    private var guidance: GuidanceLifecycleObserver? = null

    fun handleGuidanceRequest(bridge: CommunicationBridge, direction: Direction) {
        val acquaintance = guidance

        if (acquaintance != null && acquaintance.direction != direction && acquaintance.enabled) {
            bridge.activeConnection.isRoaming = true
            backend.applicationScope.launch(Dispatchers.Main) {
                acquaintance.callback(bridge)
            }
        } else {
            bridge.send(false)
        }
    }

    fun registerForGuidanceRequests(
        lifecycleOwner: LifecycleOwner,
        direction: Direction,
        callback: AcquaintanceCallback
    ) {
        lifecycleOwner.lifecycle.addObserver(
            GuidanceLifecycleObserver(lifecycleOwner, direction, callback).also { guidance = it }
        )
    }

    internal data class GuidanceLifecycleObserver(
        private val lifecycleOwner: LifecycleOwner,
        val direction: Direction,
        val callback: AcquaintanceCallback,
    ) : LifecycleObserver {
        var enabled = false
            private set

        var destroyed = false
            private set

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
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
