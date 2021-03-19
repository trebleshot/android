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

package org.monora.uprotocol.client.android.model

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import java.io.File


class AppPackageModel(
    val packageInfo: PackageInfo,
    val applicationInfo: ApplicationInfo,
    private val name: String,
) : ContentModel {
    private val file: File = File(applicationInfo.sourceDir)

    private var selected = false

    override fun canCopy(): Boolean = true

    override fun canMove(): Boolean = false

    override fun canShare(): Boolean = true

    override fun canRemove(): Boolean = false // TODO: 2/20/21 Implement package removal support

    override fun canRename(): Boolean = false

    override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
        TODO("Not yet implemented")
    }

    override fun dateCreated(): Long = packageInfo.firstInstallTime

    override fun dateModified(): Long = packageInfo.lastUpdateTime

    override fun dateSupported(): Boolean = true

    override fun filter(charSequence: CharSequence): Boolean = name.contains(charSequence)

    override fun id(): Long = packageInfo.packageName.hashCode().toLong()

    override fun length(): Long = file.length()

    override fun lengthSupported(): Boolean = true

    override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
        throw UnsupportedOperationException()
    }

    override fun name(): String = name

    override fun remove(operationBackend: OperationBackend): Boolean {
        throw UnsupportedOperationException("Removing packages is not supported yet.")
    }

    override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
        // FIXME: 2/20/21 Implement package insertion
        if (Build.VERSION.SDK_INT >= 21) applicationInfo.splitSourceDirs?.let {
            val fileList: MutableList<Uri> = ArrayList()
            for (location in it) fileList.add(Uri.fromFile(File(location)))
        }
        return true
    }

    override fun canSelect(): Boolean = true

    override fun selected(): Boolean = selected

    override fun select(selected: Boolean) {
        this.selected = selected
    }
}

