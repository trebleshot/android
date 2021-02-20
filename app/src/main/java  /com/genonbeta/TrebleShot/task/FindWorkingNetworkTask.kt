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
package com.genonbeta.TrebleShot.task

import android.content.*
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.model.Device
import com.genonbeta.TrebleShot.model.DeviceAddress
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException
import com.genonbeta.TrebleShot.task.FindWorkingNetworkTask.CalculationResultListener
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.TrebleShot.util.CommunicationBridge.Companion.receiveResult
import java.io.IOException

class FindWorkingNetworkTask(private val device: Device) : AttachableAsyncTask<CalculationResultListener>() {
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        try {
            val knownAddressList: List<DeviceAddress> = AppUtils.getKuick(context).castQuery(
                Transfers.createAddressSelection(device.uid), DeviceAddress::class.java
            )
            progress.increaseTotalBy(knownAddressList.size)
            if (knownAddressList.isNotEmpty()) {
                for (address in knownAddressList) {
                    throwIfStopped()
                    ongoingContent = address.hostAddress
                    progress.increaseBy(1)
                    try {
                        CommunicationBridge.connect(kuick, address, device, 0).use { client ->
                            client.requestAcquaintance()
                            if (client.receiveResult()) {
                                post { anchor?.onCalculationResult(device, address) }
                                return
                            }
                        }
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
            post { anchor?.onCalculationResult(device, null) }
        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(context, e))
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_findAvailableNetwork)
    }

    interface CalculationResultListener : AttachedTaskListener {
        fun onCalculationResult(device: Device, address: DeviceAddress?)
    }
}