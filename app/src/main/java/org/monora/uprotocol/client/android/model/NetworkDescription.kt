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

package org.monora.uprotocol.client.android.model

import android.net.MacAddress
import android.net.wifi.ScanResult
import android.net.wifi.WifiNetworkSuggestion
import android.os.Parcelable
import androidx.annotation.RequiresApi
import androidx.core.util.ObjectsCompat
import kotlinx.parcelize.Parcelize

@Parcelize
class NetworkDescription(var ssid: String, var bssid: String?, var password: String?) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (other is NetworkDescription) {
            return bssid == other.bssid || (bssid == null && ssid == other.ssid)
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return ObjectsCompat.hash(ssid, bssid, password)
    }
}
