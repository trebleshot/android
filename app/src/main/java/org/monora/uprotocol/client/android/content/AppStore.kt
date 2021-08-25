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
package org.monora.uprotocol.client.android.content

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Parcelable
import androidx.lifecycle.liveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.text.Collator
import javax.inject.Inject

class AppStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    @SuppressLint("QueryPermissionsNeeded")
    fun getAll() = liveData(Dispatchers.IO) {
        val packages = context.packageManager.getInstalledPackages(PackageManager.GET_META_DATA)
        if (packages.isEmpty()) {
            emit(emptyList())
        }

        val list = mutableListOf<App>()

        for (info in packages) {
            if (info.applicationInfo.flags and ApplicationInfo.FLAG_SYSTEM != 0
                && info.applicationInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP == 0
            ) continue

            try {
                list.add(
                    App(
                        info.applicationInfo.loadLabel(context.packageManager).toString(),
                        info.packageName,
                        info.versionName,
                        info.applicationInfo,
                    )
                )
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }

        val collator = Collator.getInstance()
        collator.strength = Collator.TERTIARY

        emit(list.sortedWith(compareBy(collator) { it.label }))
    }
}

@Parcelize
data class App(
    val label: String,
    val packageName: String,
    val versionName: String,
    val info: ApplicationInfo,
) : Parcelable {
    @IgnoredOnParcel
    var isSelected = false

    @IgnoredOnParcel
    val isSplit = Build.VERSION.SDK_INT >= 21 && info.splitSourceDirs != null

    override fun equals(other: Any?): Boolean {
        return other is App && packageName == other.packageName
    }
}
