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
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.HomeActivity
import org.monora.uprotocol.client.android.util.AppUtils

/**
 * created by: Veli
 * date: 18.11.2017 20:16
 */
class RationalePermissionRequest(
    activity: Activity,
    var permissionQueue: PermissionRequest,
    killActivityOtherwise: Boolean
) : AlertDialog.Builder(activity) {

    data class PermissionRequest(
        val permission: String,
        val title: String,
        val message: String,
        val required: Boolean = true
    ) {
        constructor(context: Context, permission: String, titleRes: Int, messageRes: Int) : this(
            permission,
            context.getString(titleRes),
            context.getString(messageRes)
        )
    }

    companion object {
        fun requestIfNecessary(
            activity: Activity,
            permissionQueue: PermissionRequest,
            killActivityOtherwise: Boolean
        ): AlertDialog? = if (ActivityCompat.checkSelfPermission(activity, permissionQueue.permission)
            == PackageManager.PERMISSION_GRANTED
        ) null
        else
            RationalePermissionRequest(activity, permissionQueue, killActivityOtherwise).show()
    }

    init {
        setCancelable(false)
        setTitle(permissionQueue.title)
        setMessage(permissionQueue.message)
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permissionQueue.permission
            )
        ) setNeutralButton(R.string.butn_settings) { dialogInterface: DialogInterface?, _: Int ->
            AppUtils.startApplicationDetails(activity)
        }
        setPositiveButton(R.string.butn_ask) { _: DialogInterface?, _: Int ->
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(permissionQueue.permission),
                HomeActivity.REQUEST_PERMISSION_ALL
            )
        }
        if (killActivityOtherwise)
            setNegativeButton(R.string.butn_reject) { _: DialogInterface?, _: Int -> activity.finish() }
        else
            setNegativeButton(R.string.butn_close, null)
    }
}