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
import android.os.Build
import android.provider.DocumentsContract
import androidx.annotation.RequiresApi
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException

/**
 * created by: Veli
 * date: 17.02.2018 22:36
 */
abstract class DocumentFile(private val parent: DocumentFile?, val originalUri: Uri) {
    abstract fun canRead(): Boolean

    abstract fun canWrite(): Boolean

    abstract fun delete(): Boolean

    override fun equals(other: Any?): Boolean {
        return other is DocumentFile && hashCode() == other.hashCode()
    }

    abstract fun exists(): Boolean

    abstract fun createDirectory(displayName: String): DocumentFile?

    abstract fun createFile(mimeType: String, displayName: String): DocumentFile?

    open fun findFile(displayName: String): DocumentFile? {
        for (doc in listFiles()) {
            if (displayName == doc.getName()) {
                return doc
            }
        }
        return null
    }

    override fun hashCode(): Int {
        return originalUri.hashCode()
    }

    abstract fun getName(): String

    open fun getParentFile(): DocumentFile? {
        return parent
    }

    abstract fun getType(): String

    abstract fun getUri(): Uri

    abstract fun isDirectory(): Boolean

    abstract fun isFile(): Boolean

    abstract fun isVirtual(): Boolean

    abstract fun getLastModified(): Long

    abstract fun getLength(): Long

    abstract fun listFiles(): Array<DocumentFile>

    abstract fun renameTo(displayName: String): DocumentFile?

    @Throws(Exception::class)
    abstract fun sync()

    companion object {
        val TAG = DocumentFile::class.java.simpleName

        fun fromFile(file: File): DocumentFile {
            return LocalDocumentFile(null, file)
        }

        @Throws(FileNotFoundException::class)
        fun fromUri(context: Context, uri: Uri, prepareTree: Boolean): DocumentFile {
            if (Build.VERSION.SDK_INT >= 21)
                try {
                    return TreeDocumentFile.from(null, context, if (prepareTree) prepareUri(uri) else uri, uri)
                } catch (ignored: Exception) {
                    // expected because it might not be TreeDocumentFile
                }

            try {
                return StreamDocumentFile(StreamInfo.from(context, uri), uri)
            } catch (ignored: Exception) {
            }
            throw FileNotFoundException("Failed to encapsulate the given Uri $uri")
        }

        @RequiresApi(21)
        protected fun prepareUri(treeUri: Uri): Uri {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        }
    }
}