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
package org.monora.uprotocol.client.android.dialogimport

import android.app.Activity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.UClient

class RemoveDeviceDialog(activity: Activity, client: UClient) : AlertDialog.Builder(activity) {
    init {
        setTitle(R.string.ques_removeDevice)
        setMessage(R.string.text_removeDeviceNotice)
        setNegativeButton(R.string.butn_cancel, null)
        setPositiveButton(R.string.butn_proceed) { dialog: DialogInterface?, which: Int ->
            // TODO: 2/25/21 remove the device
        }
    }
}