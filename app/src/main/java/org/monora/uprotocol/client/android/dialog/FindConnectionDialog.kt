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
package org.monora.uprotocol.client.android.dialog

import android.app.Activity
import android.content.DialogInterface
import org.monora.uprotocol.client.android.App
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.app.ProgressDialog
import org.monora.uprotocol.client.android.model.Device
import org.monora.uprotocol.client.android.model.DeviceAddress
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.task.FindWorkingNetworkTask
import org.monora.uprotocol.client.android.util.DeviceLoader

class FindConnectionDialog internal constructor(activity: Activity) : ProgressDialog(activity) {
    internal class LocalTaskBinder(
        var activity: Activity,
        var dialog: FindConnectionDialog,
        val device: Device,
        val listener: DeviceLoader.OnResolvedListener?,
    ) : FindWorkingNetworkTask.CalculationResultListener {
        override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
            if (dialog.isShowing) {
                if (task.finished) {
                    dialog.dismiss()
                } else {
                    dialog.setMessage(task.ongoingContent)
                    dialog.max = task.progress.getTotal()
                    dialog.progress = task.progress.getProgress()
                }
            }
        }

        override fun onTaskMessage(taskMessage: TaskMessage): Boolean {
            activity.runOnUiThread { taskMessage.toDialogBuilder(activity).show() }
            return true
        }

        override fun onCalculationResult(device: Device, address: DeviceAddress?) {
            if (address == null) {
                Builder(activity)
                    .setTitle(R.string.text_connectionError)
                    .setMessage(R.string.text_connectionToRemoteFailed)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_retry) { dialog: DialogInterface?, which: Int ->
                        show(activity, device, listener)
                    }
                    .show()
            } else listener?.onDeviceResolved(device, address)
        }
    }

    companion object {
        fun show(activity: Activity, device: Device, listener: DeviceLoader.OnResolvedListener?) {
            val dialog = FindConnectionDialog(activity)
            val binder = LocalTaskBinder(activity, dialog, device, listener)
            val task = FindWorkingNetworkTask(device)
            task.anchor = binder
            val removeOnClose = Runnable {
                task.removeAnchor()
                task.interrupt()
            }
            dialog.setTitle(R.string.text_automaticNetworkConnectionOngoing)
            dialog.setCancelable(false)
            dialog.setOnDismissListener { removeOnClose.run() }
            dialog.setOnCancelListener { removeOnClose.run() }
            dialog.setButton(BUTTON_NEGATIVE, activity.getString(R.string.butn_cancel)) { _: DialogInterface?, _: Int ->
                removeOnClose.run()
            }
            dialog.show()
            App.run(activity, task)
        }
    }
}