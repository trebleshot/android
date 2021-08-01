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
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.io.OpenableContent
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import java.net.URLConnection
import java.util.*
import kotlin.math.ln
import kotlin.math.pow

/**
 * created by: veli
 * date: 7/31/18 8:14 AM
 */
object Files {
    private val TAG = Files::class.simpleName

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

    fun getActionTypeToView(type: String?): String {
        return if ("application/vnd.android.package-archive" == type && Build.VERSION.SDK_INT >= 14) {
            Intent.ACTION_INSTALL_PACKAGE
        } else {
            Intent.ACTION_VIEW
        }
    }

    fun getFileContentType(fileUrl: String): String {
        val nameMap = URLConnection.getFileNameMap()
        val fileType = nameMap.getContentTypeFor(fileUrl)
        return fileType ?: "*/*"
    }

    fun getFileExtension(fileName: String): String {
        val dotIndex = fileName.lastIndexOf('.')

        if (dotIndex >= 0) {
            val extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT)
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
            if (mime != null) return ".$extension"
        }

        return ""
    }

    fun getOpenIntent(context: Context, file: DocumentFile): Intent {
        return if (Build.VERSION.SDK_INT >= 24 || Build.VERSION.SDK_INT == 23
            && Intent.ACTION_INSTALL_PACKAGE != getActionTypeToView(file.getType())
        ) {
            getOpenIntent(getSecureUriSilently(context, file), file.getType())
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else
            getOpenIntent(file.originalUri, file.getType())
    }

    fun getOpenIntent(url: Uri?, type: String?): Intent {
        return Intent(getActionTypeToView(type)).setDataAndType(url, type)
    }

    fun getSecureUriSilently(context: Context, documentFile: DocumentFile): Uri {
        try {
            return documentFile.getSecureUri(context)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return documentFile.getUri()
    }

    fun getSecureUri(context: Context, openable: OpenableContent): Uri {
        return if (openable.file != null) getSelfProviderFile(context, openable.file) else openable.uri
    }

    fun getSelfProviderFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(context, "${context.applicationContext.packageName}.fileprovider", file)
    }

    fun getUniqueFileName(
        context: Context,
        directory: DocumentFile,
        fileName: String,
        tryOriginalFirst: Boolean
    ): String {
        if (tryOriginalFirst && directory.findFile(context, fileName) == null)
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

    fun openUri(context: Context, file: DocumentFile): Boolean {
        return openUri(context, getOpenIntent(context, file))
    }

    fun openUri(context: Context, uri: Uri): Boolean {
        return openUri(context, getOpenIntent(uri, context.contentResolver.getType(uri)))
    }

    fun openUri(context: Context, intent: Intent): Boolean {
        try {
            context.startActivity(intent)
            return true
        } catch (e: Throwable) {
            Log.d(TAG, String.format(Locale.US, "Open uri request failed with error message '%s'", e.message))
        }
        return false
    }
}