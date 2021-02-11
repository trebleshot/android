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
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.util.CommunicationBridge

com.genonbeta.TrebleShot.dataobject.MappedSelectable.compileFrom

class TextShareTask(private val mDevice: Device, address: DeviceAddress, text: String) : AsyncTask() {
    private val mAddress: DeviceAddress
    private val mText: String
    override fun onRun() {
        try {
            CommunicationBridge.connect(kuick(), mAddress, mDevice, 0).use { bridge ->
                bridge.requestTextTransfer(mText)
                if (bridge.receiveResult()) {
                    // TODO: 31.03.2020 implement
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getName(context: Context?): String? {
        return null
    }

    init {
        mAddress = address
        mText = text
    }
}