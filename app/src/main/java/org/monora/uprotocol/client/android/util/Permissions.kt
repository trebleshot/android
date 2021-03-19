/*
 * Copyright (C) 2021 Veli Tasalı
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

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.dialog.PermissionRequests
import java.util.ArrayList

object Permissions {
    fun checkRunningConditions(context: Context): Boolean {
        for (request in getRequiredPermissions(context)) {
            if (ActivityCompat.checkSelfPermission(context, request.permission) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    fun getRequiredPermissions(context: Context): List<PermissionRequests.PermissionRequest> {
        val permissionRequests: MutableList<PermissionRequests.PermissionRequest> = ArrayList()
        if (Build.VERSION.SDK_INT >= 16) {
            permissionRequests.add(
                PermissionRequests.PermissionRequest(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    R.string.text_requestPermissionStorage,
                    R.string.text_requestPermissionStorageSummary
                )
            )
        }
        return permissionRequests
    }
}