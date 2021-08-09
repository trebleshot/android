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
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import androidx.annotation.StringRes
import androidx.core.app.ActivityCompat.*
import org.monora.uprotocol.client.android.R

object Permissions {
    fun checkRunningConditions(context: Context): Boolean {
        for (permission in getAll()) {
            if (checkSelfPermission(context, permission.id) != PERMISSION_GRANTED && permission.isRequired) {
                return false
            }
        }
        return true
    }

    fun getAll(): List<Permission> {
        val permissions: MutableList<Permission> = ArrayList()
        if (Build.VERSION.SDK_INT >= 23) {
            permissions.add(
                Permission(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    R.string.text_requestPermissionStorage,
                    R.string.text_requestPermissionStorageSummary
                )
            )
        }
        return permissions
    }

    data class Permission(
        val id: String,
        @StringRes val title: Int,
        @StringRes val description: Int,
        val isRequired: Boolean = true
    )
}
