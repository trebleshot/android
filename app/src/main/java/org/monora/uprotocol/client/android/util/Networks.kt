/*
 * Copyright (C) 2020 Veli Tasalı
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

import org.monora.uprotocol.client.android.config.AppConfig
import java.net.Inet4Address
import java.net.NetworkInterface

object Networks {
    fun NetworkInterface.getFirstInet4Address(): Inet4Address? {
        val addresses = this.inetAddresses
        while (addresses.hasMoreElements()) {
            val address = addresses.nextElement()
            if (address is Inet4Address) return address
        }
        return null
    }

    fun getInterfaces(
        ipV4only: Boolean = true,
        avoidedInterfaces: Array<String>? = AppConfig.DEFAULT_DISABLED_INTERFACES
    ): List<NetworkInterface> {
        val filteredInterfaceList: MutableList<NetworkInterface> = ArrayList()
        try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()
                var avoidedInterface = false
                if (avoidedInterfaces != null && avoidedInterfaces.isNotEmpty()) {
                    for (match in avoidedInterfaces) {
                        if (networkInterface.displayName.startsWith(match)) avoidedInterface = true
                    }
                }
                if (avoidedInterface) continue
                val addressList = networkInterface.inetAddresses
                while (addressList.hasMoreElements()) {
                    val address = addressList.nextElement()
                    if (!address.isLoopbackAddress && (address is Inet4Address || !ipV4only)) {
                        filteredInterfaceList.add(networkInterface)
                        break
                    }
                }
            }
        } catch (ignored: Exception) {
        }
        return filteredInterfaceList
    }
}
