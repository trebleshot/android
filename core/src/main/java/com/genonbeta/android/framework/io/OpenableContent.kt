/*
 * Copyright (C) 2020 Veli Tasalı
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
package com.genonbeta.android.framework.io

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.genonbeta.android.framework.util.Files
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.URI
import java.util.*

/**
 * created by: Veli
 * date: 4.10.2017 12:36
 */
class OpenableContent private constructor(
    val name: String,
    val mimeType: String,
    val uri: Uri,
    val size: Long = 0,
    val file: File? = null,
) {
    override fun hashCode(): Int {
        return uri.hashCode()
    }

    @Throws(FileNotFoundException::class)
    fun openOutputStream(context: Context): OutputStream = if (file == null) {
        context.contentResolver.openOutputStream(uri, "wa") ?: throw IOException()
    } else {
        FileOutputStream(file, true)
    }

    @Throws(FileNotFoundException::class)
    fun openInputStream(context: Context): InputStream = if (file == null) {
        context.contentResolver.openInputStream(uri) ?: throw IOException()
    } else {
        FileInputStream(file)
    }

    companion object {
        @Throws(IOException::class, FileNotFoundException::class)
        fun from(context: Context, uri: Uri): OpenableContent {
            val uriAsString = uri.toString()
            if (uriAsString.startsWith("file")) {
                val file = File(URI.create(uriAsString))
                if (file.canRead()) {
                    if (file.isDirectory) {
                        throw IOException("Tried to encapsulate a directory.")
                    }

                    return OpenableContent(file.name, Files.getFileContentType(file.name), uri, file.length(), file)
                }
            } else if (uriAsString.startsWith("content")) {
                context.contentResolver.query(
                    uri, null, null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

                        if (nameIndex != -1 && sizeIndex != -1) {
                            val name = cursor.getString(nameIndex) ?: UUID.randomUUID().toString()
                            val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

                            return OpenableContent(name, mimeType, uri, cursor.getLong(sizeIndex))
                        }
                    }
                }
            }

            throw IOException("Could not encapsulate the given Uri.")
        }
    }
}
