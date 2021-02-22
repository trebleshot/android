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
package org.monora.uprotocol.client.android.ui.helpimport

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.AddDeviceActivity
import org.monora.uprotocol.client.android.activity.AddDeviceActivity.AvailableFragment

class ConnectionSetUpAssistant(activity: Activity) {
    val context: Context = activity.applicationContext

    val dialogInstance: AlertDialog.Builder
        get() = AlertDialog.Builder(context)
            .setTitle(R.string.text_connectionWizard)

    // use barcode scanner
    val isThereQRCode: Unit
        get() {
            dialogInstance
                .setMessage(R.string.ques_connectionWizardIsThereQRCode)
                .setNeutralButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_yes) { dialog, which -> // use barcode scanner
                    updateFragment(AvailableFragment.ScanQrCode)
                }
                .setNegativeButton(R.string.butn_no) { dialog, which -> useHotspot() }
                .show()
        }

    fun useNetwork() {
        dialogInstance
            .setMessage(R.string.ques_connectionWizardUseNetwork)
            .setNeutralButton(R.string.butn_cancel, null)
            .setPositiveButton(R.string.butn_yes) { dialog, which -> // open network settings
                updateFragment(AvailableFragment.GenerateQrCode)
            }
            .setNegativeButton(R.string.butn_no) { dialog, which -> useKnownDevices() }
            .show()
    }

    fun useKnownDevices() {
        dialogInstance
            .setMessage(R.string.ques_connectionWizardUseKnownDevices)
            .setNeutralButton(R.string.butn_cancel, null)
            .setPositiveButton(R.string.butn_yes) { dialog, which -> // open known devices settings
                updateFragment(AvailableFragment.AllDevices)
            }
            .setNegativeButton(R.string.butn_retry) { dialog, which -> isOtherDeviceReady }
            .show()
    }

    fun useHotspot() {
        dialogInstance
            .setMessage(R.string.ques_connectionWizardUseHotspot)
            .setNeutralButton(R.string.butn_cancel, null)
            .setPositiveButton(R.string.butn_yes) { dialog, which -> // open hotspot settings
                updateFragment(AvailableFragment.CreateHotspot)
            }
            .setNegativeButton(R.string.butn_no) { dialog, which -> useNetwork() }
            .show()
    }

    val isOtherDeviceReady: Unit
        get() {
            dialogInstance
                .setMessage(R.string.ques_connectionWizardIsOtherDeviceReady)
                .setNeutralButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_yes) { dialog, which -> isThereQRCode }
                .setNegativeButton(R.string.butn_no) { dialog, which -> useHotspot() }
                .show()
        }

    fun startShowing() {
        isOtherDeviceReady
    }

    fun updateFragment(fragment: AvailableFragment) {
        context.sendBroadcast(
            Intent(AddDeviceActivity.ACTION_CHANGE_FRAGMENT)
                .putExtra(AddDeviceActivity.EXTRA_FRAGMENT_ENUM, fragment)
        )
    }
}