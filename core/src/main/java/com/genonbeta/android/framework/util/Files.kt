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

package com.genonbeta.android.framework.util

import android.content.Context
import android.webkit.MimeTypeMap
import com.genonbeta.android.framework.io.DocumentFile
import java.io.File
import java.io.IOException
import java.net.URLConnection
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

/**
 * created by: veli
 * date: 7/31/18 8:14 AM
 */
object Files {
    private const val TAG = "Files"

    @Throws(IOException::class)
    fun fetchDirectories(
        context: Context,
        directory: DocumentFile,
        path: String,
        createIfNeeded: Boolean = true,
    ): DocumentFile {
        var current: DocumentFile? = directory
        val pathArray: Array<String> = path.split(File.separator.toRegex()).toTypedArray()

        for (currentPath in pathArray) {
            if (current == null)
                throw IOException("Failed to create directories: $path")

            val existing = current.findFile(context, currentPath)

            existing?.let {
                if (!it.isDirectory()) {
                    throw IOException("A file exists for of directory name: $currentPath ; $path")
                }
            }

            current = if (existing == null && createIfNeeded) {
                current.createDirectory(context, currentPath)
            } else {
                existing
            }
        }

        return current as DocumentFile
    }

    @Throws(IOException::class)
    fun fetchFile(
        context: Context,
        directory: DocumentFile,
        path: String?,
        mimeType: String,
        fileName: String,
        createIfNeeded: Boolean = true,
    ): DocumentFile {
        val documentFile = if (path == null) directory else fetchDirectories(context, directory, path, createIfNeeded)
        val existing = documentFile.findFile(context, fileName)

        if (existing != null) {
            if (!existing.isFile())
                throw IOException("An entity in the same directory with the same name already exists.")
            return existing
        } else if (createIfNeeded) {
            val createdFile = documentFile.createFile(context, mimeType, fileName)
            if (createdFile != null) {
                return createdFile
            }
        }

        throw IOException("Failed to create file: $path")
    }

    fun formatLength(length: Long, kilo: Boolean = false): String {
        val unit = if (kilo) 1000 else 1024
        if (length < unit) return "$length B"
        val expression = (ln(length.toDouble()) / ln(unit.toDouble())).toInt()
        val prefix = (if (kilo) "kMGTPE" else "KMGTPE")[expression - 1] + if (kilo) "" else "i"
        return String.format(
            Locale.getDefault(), "%.1f %sB", length / unit.toDouble().pow(expression.toDouble()), prefix
        )
    }

    fun getFileContentType(fileUrl: String): String {
        val nameMap = URLConnection.getFileNameMap()
        val fileType = nameMap.getContentTypeFor(fileUrl)
        return fileType ?: "*/*"
    }

    fun getFileExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')

        if (dotIndex >= 0) {
            val extension = fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) return ".$extension"
        }

        return ""
    }

    fun getUniqueFileName(
        context: Context,
        directory: DocumentFile,
        fileName: String,
    ): String {
        if (directory.findFile(context, fileName) == null)
            return fileName

        val pathStartPosition = fileName.lastIndexOf(".")
        var mergedName = if (pathStartPosition != -1) fileName.substring(0, pathStartPosition) else fileName
        var fileExtension = if (pathStartPosition != -1) fileName.substring(pathStartPosition) else ""
        if (mergedName.isEmpty() && fileExtension.isNotEmpty()) {
            mergedName = fileExtension
            fileExtension = ""
        }
        for (exceed in 1..998) {
            val newName = "$mergedName ($exceed)$fileExtension"
            if (directory.findFile(context, newName) == null) return newName
        }
        return fileName
    }
}
