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
package com.genonbeta.TrebleShot.taskimport

import android.content.*
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.util.CommunicationBridge
import com.genonbeta.TrebleShot.util.CommunicationBridge.Companion.receiveResult

class TextShareTask(
    private val device: Device,
    private val address: DeviceAddress,
    private val text: String,
) : AsyncTask() {
    override fun onRun() {
        try {
            CommunicationBridge.connect(kuick, address, device, 0).use { bridge ->
                bridge.requestTextTransfer(text)
                if (bridge.receiveResult()) {
                    // TODO: 31.03.2020 implement
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_shareTextShort)
    }
}