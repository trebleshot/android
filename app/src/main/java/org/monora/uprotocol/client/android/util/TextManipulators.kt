/*
 * Copyright (C) 2019 Veli Tasalı
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

import android.content.Context
import androidx.collection.ArrayMap
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig
import java.net.NetworkInterface
import java.util.*

/**
 * created by: Veli
 * date: 12.11.2017 11:14
 */
object TextManipulators {
    fun getWebShareAddress(context: Context, address: String?): String {
        return context.getString(R.string.web_share_address, address, AppConfig.SERVER_PORT_WEBSHARE)
    }

    fun getLetters(text: String = "?", length: Int): String {
        val breakAfter = length - 1
        val stringBuilder = StringBuilder()
        for (letter in text.split(" ".toRegex()).toTypedArray()) {
            if (stringBuilder.length > breakAfter) break
            if (letter.isEmpty()) continue
            stringBuilder.append(letter[0])
        }
        return stringBuilder.toString().uppercase(Locale.getDefault())
    }

    fun String.toFriendlySsid() = this.replace("\"", "").let {
        if (it.startsWith(AppConfig.PREFIX_ACCESS_POINT))
            it.substring((AppConfig.PREFIX_ACCESS_POINT.length))
        else it
    }.replace("_", " ")

    fun toNetworkTitle(adapterName: String): Int {
        val unknownInterface = R.string.unknown_interface
        val associatedNames: MutableMap<String, Int> = ArrayMap()
        associatedNames["wlan"] = R.string.wifi
        associatedNames["p2p"] = R.string.wifi_direct
        associatedNames["bt-pan"] = R.string.bluetooth
        associatedNames["eth"] = R.string.ethernet
        associatedNames["tun"] = R.string.vpn_interface
        associatedNames["unk"] = unknownInterface
        for (displayName in associatedNames.keys) if (adapterName.startsWith(displayName)) {
            return associatedNames[displayName] ?: unknownInterface
        }
        return -1
    }

    fun String.toNetworkTitle(context: Context): String {
        val adapterNameResource = toNetworkTitle(this)
        return if (adapterNameResource == -1) this else context.getString(adapterNameResource)
    }

    fun NetworkInterface.toNetworkTitle(context: Context): String {
        return this.displayName.toNetworkTitle(context)
    }
}
