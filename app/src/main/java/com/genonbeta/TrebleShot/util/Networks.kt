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
package com.genonbeta.TrebleShot.util

import com.genonbeta.TrebleShot.adapter.ActiveConnectionListAdapter.EditableNetworkInterface
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.*

object Networks {
    fun getFirstInet4Address(networkInterface: EditableNetworkInterface): Inet4Address? {
        return getFirstInet4Address(networkInterface.getInterface())
    }

    fun getFirstInet4Address(networkInterface: NetworkInterface): Inet4Address? {
        val addresses = networkInterface.inetAddresses
        while (addresses.hasMoreElements()) {
            val address = addresses.nextElement()
            if (address is Inet4Address) return address
        }
        return null
    }

    fun getInterfaces(ipV4only: Boolean, avoidedInterfaces: Array<String>?): List<NetworkInterface> {
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