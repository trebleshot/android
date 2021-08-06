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

package org.monora.uprotocol.client.android.util

import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.core.CommunicationBridge

val CommunicationBridge.clientRoute: ClientRoute
    get() {
        val client = this.remoteClient
        val address = this.remoteClientAddress

        check(client is UClient) {
            "Unsupported client wrapper class: ${client.javaClass.simpleName}"
        }

        check(address is UClientAddress) {
            "Unsupported client address wrapper class: ${address.javaClass.simpleName}"
        }

        return ClientRoute(client, address)
    }
