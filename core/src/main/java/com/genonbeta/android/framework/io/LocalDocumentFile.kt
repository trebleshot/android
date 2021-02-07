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

import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import java.io.File
import java.io.IOException
import java.util.*

/**
 * created by: Veli
 * date: 17.02.2018 23:39
 */
class LocalDocumentFile(parent: DocumentFile?, val file: File) : DocumentFile(parent, Uri.fromFile(file)) {
    override fun createFile(mimeType: String, displayName: String): DocumentFile? {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        val target = File(file, extension?.let { "$displayName.$it" } ?: displayName)

        try {
            target.createNewFile()
            return LocalDocumentFile(this, target)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun createDirectory(displayName: String): DocumentFile? {
        val target = File(file, displayName)

        if (target.isDirectory || (!target.exists() && target.mkdirs()))
            return LocalDocumentFile(this, target)

        return null
    }

    override fun getUri(): Uri {
        return Uri.fromFile(file)
    }

    override fun getName(): String {
        return file.name
    }

    override fun getParentFile(): DocumentFile? {
        val parentFile = file.parentFile
        return if (parentFile == null || File.separator == parentFile.absolutePath) null
        else LocalDocumentFile(null, parentFile)
    }

    override fun getType(): String {
        return if (file.isDirectory) "*/*" else getTypeForName(file.name)
    }

    override fun isDirectory(): Boolean {
        return file.isDirectory
    }

    override fun isFile(): Boolean {
        return file.isFile
    }

    override fun isVirtual(): Boolean {
        return false
    }

    override fun getLastModified(): Long {
        return file.lastModified()
    }

    override fun getLength(): Long {
        return file.length()
    }

    override fun canRead(): Boolean {
        return file.canRead()
    }

    override fun canWrite(): Boolean {
        return file.canWrite()
    }

    override fun delete(): Boolean {
        deleteContents(file)
        return file.delete()
    }

    override fun exists(): Boolean {
        return file.exists()
    }

    override fun findFile(displayName: String): DocumentFile? {
        val file = File(file, displayName)
        return if (file.exists()) LocalDocumentFile(this, file) else null
    }

    override fun listFiles(): Array<DocumentFile> {
        val results: MutableList<DocumentFile> = ArrayList()
        val files = file.listFiles()
        if (files != null) for (file in files) results.add(LocalDocumentFile(this, file))
        return results.toTypedArray()
    }

    override fun renameTo(displayName: String): DocumentFile? {
        val target = File(file.parentFile, displayName)

        if (file.renameTo(target))
            return fromFile(target)

        return null
    }

    override fun sync() {}

    companion object {
        private fun getTypeForName(name: String): String {
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1).toLowerCase(Locale.getDefault())
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let {
                    return it
                }
            }
            return "application/octet-stream"
        }

        private fun deleteContents(dir: File): Boolean {
            val files = dir.listFiles()
            var success = true

            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        success = success and deleteContents(file)
                    }
                    if (!file.delete()) {
                        Log.w(TAG, "Failed to delete $file")
                        success = false
                    }
                }
            }
            return success
        }
    }
}