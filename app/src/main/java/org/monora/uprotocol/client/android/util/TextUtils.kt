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
import org.monora.uprotocol.core.transfer.TransferItem
import java.net.NetworkInterface
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 12.11.2017 11:14
 */
object TextUtils {
    fun getAdapterName(adapterName: String): Int {
        val associatedNames: MutableMap<String, Int> = ArrayMap()
        val unknownInterface = R.string.text_interfaceUnknown
        associatedNames["wlan"] = R.string.text_interfaceWireless
        associatedNames["p2p"] = R.string.text_interfaceWifiDirect
        associatedNames["bt-pan"] = R.string.text_interfaceBluetooth
        associatedNames["eth"] = R.string.text_interfaceEthernet
        associatedNames["tun"] = R.string.text_interfaceVPN
        associatedNames["unk"] = unknownInterface
        for (displayName in associatedNames.keys) if (adapterName.startsWith(displayName)) {
            return associatedNames[displayName] ?: unknownInterface
        }
        return -1
    }

    fun getAdapterName(context: Context, networkInterface: NetworkInterface): String {
        return getAdapterName(context, networkInterface.displayName)
    }

    fun getAdapterName(context: Context, adapterName: String): String {
        val adapterNameResource = getAdapterName(adapterName)
        return if (adapterNameResource == -1) adapterName else context.getString(adapterNameResource)
    }

    fun getLetters(text: String = "?", length: Int): String {
        val breakAfter = length - 1
        val stringBuilder = StringBuilder()
        for (letter in text.split(" ".toRegex()).toTypedArray()) {
            if (stringBuilder.length > breakAfter) break
            if (letter.isEmpty()) continue
            stringBuilder.append(letter[0])
        }
        return stringBuilder.toString().toUpperCase()
    }

    fun makeWebShareLink(context: Context, address: String?): String {
        return context.getString(R.string.mode_webShareAddress, address, AppConfig.SERVER_PORT_WEBSHARE)
    }
}