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

package org.monora.uprotocol.client.android.data

import android.content.Context
import com.genonbeta.android.framework.io.DocumentFile
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.model.FileModel
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepository @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val context = WeakReference(context)

    fun getFileList(file: DocumentFile): List<FileModel> {
        val context = context.get() ?: return emptyList()

        check(file.isDirectory()) {
            "${file.originalUri} is not a directory."
        }

        return file.listFiles(context).map {
            FileModel(it, it.takeIf { it.isDirectory() }?.listFiles(context)?.size ?: 0)
        }
    }
}