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
import java.io.File

/**
 * created by: Veli
 * date: 18.02.2018 00:24
 */
class StreamDocumentFile(val stream: StreamInfo, uri: Uri) : DocumentFile(null, uri) {
    override fun canRead(): Boolean {
        return true
    }

    override fun canWrite(): Boolean {
        return true
    }

    override fun createFile(mimeType: String, displayName: String): DocumentFile? {
        return null
    }

    override fun createDirectory(displayName: String): DocumentFile? {
        return null
    }

    override fun delete(): Boolean {
        return false
    }

    override fun exists(): Boolean {
        return true
    }


    override fun getUri(): Uri {
        return stream.uri
    }

    fun getFile(): File? {
        return stream.file
    }

    override fun getLastModified(): Long {
        return 0
    }

    override fun getLength(): Long {
        return stream.size
    }

    override fun getName(): String {
        return stream.friendlyName
    }

    override fun getType(): String {
        return stream.mimeType
    }

    override fun isDirectory(): Boolean {
        return false
    }

    override fun isFile(): Boolean {
        return true
    }

    override fun isVirtual(): Boolean {
        return false
    }

    override fun listFiles(): Array<DocumentFile> {
        return emptyArray()
    }

    override fun renameTo(displayName: String): DocumentFile? {
        return null
    }

    override fun sync() {}
}