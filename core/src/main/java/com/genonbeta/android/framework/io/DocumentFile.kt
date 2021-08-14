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
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.os.Parcelable
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.*
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import kotlinx.parcelize.Parcelize
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI

/**
 * created by: Veli
 * date: 17.02.2018 22:36
 */
@Parcelize
class DocumentFile private constructor(
    val originalUri: Uri,
    private val data: Data?,
    private val file: File?,
) : Parcelable {
    val parent: DocumentFile?
        get() = data?.parent ?: file?.parentFile?.let { DocumentFile(it) }

    private constructor(originalUri: Uri, data: Data) : this(originalUri, data = data, file = null)

    private constructor(file: File) : this(Uri.fromFile(file), file = file, data = null)

    fun canRead(): Boolean = file?.canRead() ?: data?.canRead() ?: false

    fun canWrite(): Boolean = file?.canWrite() ?: data?.canWrite() ?: false

    fun createDirectory(context: Context, displayName: String): DocumentFile? {
        if (SDK_INT > 19 && data != null) {
            return createFile(context, MIME_TYPE_DIR, displayName)
        } else if (file != null) {
            val target = File(file, displayName)

            if (target.isDirectory || (!target.exists() && target.mkdirs())) {
                return DocumentFile(target)
            }
        }

        return null
    }

    fun createFile(context: Context, mimeType: String, displayName: String): DocumentFile? {
        if (SDK_INT >= 21 && data != null) {
            DocumentsContract.createDocument(context.contentResolver, data.uri, mimeType, displayName)?.let {
                return from(this, context, it, it)
            }
        } else if (file != null) {
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            val target = File(file, extension?.let { "$displayName.$it" } ?: displayName)

            try {
                if (target.isFile || target.createNewFile()) {
                    return DocumentFile(target)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        return null
    }

    fun delete(context: Context): Boolean {
        if (SDK_INT >= 19 && data != null) {
            try {
                return DocumentsContract.deleteDocument(context.contentResolver, data.uri)
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        } else if (file != null) {
            return deleteContentsIfPossible(file)
        }

        return false
    }

    override fun equals(other: Any?): Boolean {
        return other is DocumentFile && originalUri == other.originalUri
    }

    fun exists(): Boolean = data?.exists ?: file?.exists() ?: false

    fun findFile(context: Context, displayName: String): DocumentFile? {
        for (doc in listFiles(context)) {
            if (displayName == doc.getName()) {
                return doc
            }
        }
        return null
    }

    override fun hashCode(): Int {
        return originalUri.hashCode()
    }

    fun isDirectory(): Boolean = data?.isDirectory() ?: file?.isDirectory ?: throw IllegalStateException()

    fun isFile(): Boolean = data?.isFile() ?: file?.isFile ?: throw IllegalStateException()

    fun isTreeDocument(): Boolean = data != null

    fun isVirtual(): Boolean = data?.isVirtual() ?: false

    fun getLastModified(): Long = data?.lastModified ?: file?.lastModified() ?: throw IllegalStateException()

    fun getLength(): Long = data?.length ?: file?.length() ?: throw IllegalStateException()

    fun getName(): String = data?.name ?: file?.name ?: throw IllegalStateException()

    fun getSecureUri(context: Context, authority: String): Uri {
        if (SDK_INT < Build.VERSION_CODES.M || data != null) {
            return getUri()
        } else if (file != null) {
            return FileProvider.getUriForFile(context, authority, file)
        }

        throw IllegalStateException()
    }

    fun getUri(): Uri = data?.uri ?: originalUri

    fun getType(): String = data?.type ?: file?.let { getMimeType(getName()) } ?: throw IllegalStateException()

    fun listFiles(context: Context): Array<DocumentFile> {
        if (SDK_INT >= 21 && data != null) {
            val treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(data.uri, data.id)

            resolve(context, treeUri)?.use {
                if (it.moveToFirst()) {
                    val resultFiles = ArrayList<DocumentFile>(it.count)
                    val index = CursorIndex(it)

                    do {
                        val id = it.getString(index.id)
                        val uri = DocumentsContract.buildDocumentUriUsingTree(data.uri, id)
                        resultFiles.add(DocumentFile(uri, Data.from(this, uri, it, index)))
                    } while (it.moveToNext())

                    return resultFiles.toTypedArray()
                }
            }
        } else if (file != null) {
            file.listFiles()?.let { list ->
                return list.map { DocumentFile(it) }.toTypedArray()
            }
        }

        return emptyArray()
    }

    fun renameTo(context: Context, displayName: String): DocumentFile? {
        if (SDK_INT >= 21 && data != null) {
            try {
                DocumentsContract.renameDocument(context.contentResolver, data.uri, displayName)?.also {
                    return from(parent, context, it, it)
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        } else if (file != null) {
            val target = File(file.parentFile, displayName)

            if (file.renameTo(target)) {
                return fromFile(target)
            }
        }

        return null
    }

    fun sync(context: Context) {
        if (SDK_INT >= 19 && data != null) {
            data.exists = false

            resolve(context, data.uri)?.use {
                if (it.moveToFirst()) {
                    return data.update(it, CursorIndex(it))
                }
            }

            throw IOException("Syncing with latest data failed")
        }
    }

    @Parcelize
    data class Data(
        val parent: DocumentFile?,
        val uri: Uri,
        var id: String,
        var name: String,
        var type: String,
        var length: Long,
        var flags: Int,
        var lastModified: Long,
        var exists: Boolean = true,
    ) : Parcelable {
        fun canRead(): Boolean = exists

        fun canWrite(): Boolean = SDK_INT >= 19 && (isDirectory() || (flags and FLAG_SUPPORTS_WRITE) != 0)

        fun isDirectory(): Boolean = SDK_INT >= 19 && MIME_TYPE_DIR == type

        fun isFile(): Boolean = !isDirectory() && type.isNotEmpty()

        fun isVirtual(): Boolean = SDK_INT >= 24 && flags and FLAG_VIRTUAL_DOCUMENT != 0

        fun update(cursor: Cursor, index: CursorIndex) {
            id = cursor.getString(index.id)
            name = cursor.getString(index.name)
            type = cursor.getString(index.type)
            length = cursor.getLong(index.size)
            flags = cursor.getInt(index.flags)
            lastModified = cursor.getLong(index.lastModified)
            exists = true
        }

        companion object {
            fun from(parent: DocumentFile?, uri: Uri, cursor: Cursor, index: CursorIndex): Data = Data(
                parent,
                uri,
                cursor.getString(index.id),
                cursor.getString(index.name),
                cursor.getString(index.type),
                cursor.getLong(index.size),
                cursor.getInt(index.flags),
                cursor.getLong(index.lastModified),
            )
        }
    }

    @RequiresApi(19)
    data class CursorIndex(
        val id: Int,
        val name: Int,
        val size: Int,
        val type: Int,
        val flags: Int,
        val lastModified: Int,
    ) {
        constructor(cursor: Cursor) : this(
            cursor.getColumnIndex(COLUMN_DOCUMENT_ID),
            cursor.getColumnIndex(COLUMN_DISPLAY_NAME),
            cursor.getColumnIndex(COLUMN_SIZE),
            cursor.getColumnIndex(COLUMN_MIME_TYPE),
            cursor.getColumnIndex(COLUMN_FLAGS),
            cursor.getColumnIndex(COLUMN_LAST_MODIFIED)
        )
    }

    companion object {
        private const val TAG = "DocumentFile"

        private fun deleteContentsIfPossible(parentFile: File): Boolean {
            if (!parentFile.isDirectory) {
                return parentFile.delete()
            } else if (!parentFile.canWrite()) {
                Log.w(TAG, "Folder is not writable: $parentFile")
                return false
            }

            val files = parentFile.listFiles() ?: return false
            for (file in files) {
                if (!deleteContentsIfPossible(file)) {
                    Log.w(TAG, "Failed to delete $file")
                }
            }
            return true
        }

        private fun getMimeType(name: String): String {
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1).lowercase()
                MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let {
                    return it
                }
            }
            return "application/octet-stream"
        }

        @RequiresApi(19)
        fun from(parent: DocumentFile?, context: Context, original: Uri, uri: Uri): DocumentFile {
            resolve(context, uri)?.use {
                if (it.moveToFirst()) {
                    val index = CursorIndex(it)
                    return DocumentFile(original, Data.from(parent, uri, it, index))
                }
            }

            throw IOException("Could not encapsulate the data.")
        }

        fun fromFile(file: File): DocumentFile {
            return DocumentFile(file)
        }

        @Throws(FileNotFoundException::class)
        fun fromUri(context: Context, uri: Uri, prepareTree: Boolean = false): DocumentFile {
            val uriType = uri.toString()
            if (uriType.startsWith("file")) {
                return fromFile(File(URI.create(uriType)))
            } else if (SDK_INT >= 21) {
                return from(null, context, uri, if (prepareTree) prepareUri(uri) else uri)
            }

            throw FileNotFoundException("Failed to encapsulate the given uri: $uri")
        }

        @RequiresApi(21)
        private fun prepareUri(treeUri: Uri): Uri {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        }

        private fun resolve(context: Context, uri: Uri) = context.contentResolver.query(
            uri, null, null, null, null
        )
    }
}
