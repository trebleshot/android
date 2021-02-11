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
package com.genonbeta.TrebleShot.util

import android.content.Context
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.dataobject.TransferItem
import java.net.NetworkInterface
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 12.11.2017 11:14
 */
object TextUtils {
    fun getAdapterName(adapterName: String): Int {
        val associatedNames: MutableMap<String, Int> = ArrayMap()
        associatedNames["wlan"] = R.string.text_interfaceWireless
        associatedNames["p2p"] = R.string.text_interfaceWifiDirect
        associatedNames["bt-pan"] = R.string.text_interfaceBluetooth
        associatedNames["eth"] = R.string.text_interfaceEthernet
        associatedNames["tun"] = R.string.text_interfaceVPN
        associatedNames["unk"] = R.string.text_interfaceUnknown
        for (displayName in associatedNames.keys) if (adapterName.startsWith(displayName)) return associatedNames[displayName]!!
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

    fun getTransactionFlagString(
        context: Context, transferItem: TransferItem, percentFormat: NumberFormat,
        deviceId: String?,
    ): String {
        // Because it takes more arguments when the 'object' is 'Type.OUTGOING', it will be
        // chosen by the appropriate value that it may contain.
        var flag: TransferItem.Flag?
        if (TransferItem.Type.OUTGOING == transferItem.type) {
            val flags = transferItem.flags
            when {
                deviceId != null -> flag = transferItem.getFlag(deviceId)
                flags.isEmpty() -> flag = TransferItem.Flag.PENDING
                flags.size == 1 -> flag = flags[0]
                else -> {
                    flag = TransferItem.Flag.PENDING
                    var pos = 0
                    for (flagTesting in flags) {
                        val relativeOrdinal: Int = when (flagTesting) {
                            TransferItem.Flag.DONE -> 0
                            TransferItem.Flag.PENDING -> 1
                            TransferItem.Flag.REMOVED -> 2
                            TransferItem.Flag.INTERRUPTED -> 3
                            TransferItem.Flag.IN_PROGRESS -> 4
                            else -> 0
                        }
                        if (pos <= relativeOrdinal) {
                            pos = relativeOrdinal
                            flag = flagTesting
                        }
                    }
                }
            }
        } else flag = transferItem.flag
        return when (flag) {
            TransferItem.Flag.DONE -> percentFormat.format(1.0)
            TransferItem.Flag.IN_PROGRESS -> percentFormat.format(
                if (transferItem.comparableSize == 0L || flag.bytesValue == 0L) 0 else java.lang.Long.valueOf(
                    flag.bytesValue
                ).toDouble() / java.lang.Long.valueOf(transferItem.comparableSize).toDouble()
            )
            else -> context.getString(getTransactionFlagString(flag))
        }
    }

    fun getTransactionFlagString(flag: TransferItem.Flag?): Int {
        return when (flag) {
            TransferItem.Flag.PENDING -> R.string.text_flagPending
            TransferItem.Flag.DONE -> R.string.text_taskCompleted
            TransferItem.Flag.INTERRUPTED -> R.string.text_flagInterrupted
            TransferItem.Flag.IN_PROGRESS -> R.string.text_flagRunning
            TransferItem.Flag.REMOVED -> R.string.text_flagRemoved
            else -> R.string.text_unknown
        }
    }

    fun makeWebShareLink(context: Context, address: String?): String {
        return context.getString(R.string.mode_webShareAddress, address, AppConfig.SERVER_PORT_WEBSHARE)
    }

    fun searchWord(word: String, searchThis: String?): Boolean {
        return searchThis == null || searchThis.isEmpty() || word.toLowerCase().contains(searchThis.toLowerCase())
    }

    fun trimText(text: String, length: Int): String {
        return if (text.length <= length) text else text.substring(0, length)
    }
}