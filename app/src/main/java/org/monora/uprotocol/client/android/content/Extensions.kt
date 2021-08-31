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

import android.media.MediaScannerConnection
import android.net.Uri
import com.genonbeta.android.framework.io.DocumentFile

/**
 * @See android.content.ContentUris#removeId
 */
fun Uri.removeId(): Uri {
    // Verify that we have a valid ID to actually remove
    val last: String? = lastPathSegment
    last?.toLong() ?: throw IllegalArgumentException("No path segments to remove")

    val segments: List<String> = pathSegments
    val builder: Uri.Builder = buildUpon()
    builder.path(null)
    for (i in 0 until segments.size - 1) {
        builder.appendPath(segments[i])
    }
    return builder.build()
}

fun MediaScannerConnection.scan(documentFile: DocumentFile) {
    val path = documentFile.filePath
    if (path != null && isConnected) {
        scanFile(path, documentFile.getType())
    }
}
