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
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.text.TextUtils
import androidx.annotation.RequiresApi
import java.io.FileNotFoundException
import java.io.IOException

/**
 * created by: Veli
 * date: 17.02.2018 22:01
 */
// FIXME: 7/30/21 Memory leak caused by the context given to the file.
@RequiresApi(21)
class TreeDocumentFile private constructor(
    val parent: DocumentFile?, val context: Context, original: Uri, private val uri: Uri, val data: Data,
) : DocumentFile(parent, original) {
    override fun canRead(): Boolean {
        return (context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED)
    }

    override fun canWrite(): Boolean {
        return (context.checkCallingOrSelfUriPermission(uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED)
    }

    override fun createDirectory(displayName: String): DocumentFile? {
        return createFile(DocumentsContract.Document.MIME_TYPE_DIR, displayName)
    }

    override fun createFile(mimeType: String, displayName: String): DocumentFile? {
        try {
            DocumentsContract.createDocument(context.contentResolver, originalUri, mimeType, displayName)?.let {
                return from(this, context, it, it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun delete(): Boolean {
        try {
            return DocumentsContract.deleteDocument(context.contentResolver, uri)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    override fun exists(): Boolean = data.exists

    override fun getLastModified(): Long = data.lastModified

    override fun getLength(): Long = data.length

    override fun getName(): String = data.name

    override fun getType(): String = data.type

    override fun getUri(): Uri = uri

    override fun isDirectory(): Boolean {
        return DocumentsContract.Document.MIME_TYPE_DIR == data.type
    }

    override fun isFile(): Boolean {
        return DocumentsContract.Document.MIME_TYPE_DIR != data.type || TextUtils.isEmpty(data.type)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun isVirtual(): Boolean {
        return data.flags and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0
    }

    override fun listFiles(): Array<DocumentFile> {
        try {
            val treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(uri, data.id)

            resolve(context, treeUri)?.use {
                if (it.moveToFirst()) {
                    val resultFiles = ArrayList<DocumentFile>(it.count)
                    val index = CursorIndex(it)

                    do {
                        resultFiles[it.position] = from(this, context, it, index)
                    } while (it.moveToNext())

                    return resultFiles.toTypedArray()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return emptyArray()
    }

    override fun renameTo(displayName: String): TreeDocumentFile? {
        // TODO: 2/8/21 Use [DocumentsContract.Document.FLAG_SUPPORTS_RENAME]
        try {
            DocumentsContract.renameDocument(context.contentResolver, uri, displayName)?.also {
                return from(parent, context, it, it)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    @Throws(Exception::class)
    override fun sync() {
        Companion.sync(context, uri, data)
    }

    data class Data(
        var id: String,
        var name: String,
        var type: String,
        var length: Long,
        var flags: Int,
        var lastModified: Long,
        var exists: Boolean = true,
    ) {
        constructor(cursor: Cursor, index: CursorIndex) : this(
            cursor.getString(index.id),
            cursor.getString(index.name),
            cursor.getString(index.type),
            cursor.getLong(index.size),
            cursor.getInt(index.flags),
            cursor.getLong(index.lastModified),
        )

        fun update(cursor: Cursor, index: CursorIndex) {
            id = cursor.getString(index.id)
            name = cursor.getString(index.name)
            type = cursor.getString(index.type)
            length = cursor.getLong(index.size)
            flags = cursor.getInt(index.flags)
            lastModified = cursor.getLong(index.lastModified)
            exists = true
        }
    }

    data class CursorIndex(
        val id: Int,
        val name: Int,
        val size: Int,
        val type: Int,
        val flags: Int,
        val lastModified: Int,
    ) {
        constructor(cursor: Cursor) : this(
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID),
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE),
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE),
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS),
            cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        )
    }

    companion object {
        fun from(parent: DocumentFile?, context: Context, original: Uri, uri: Uri): TreeDocumentFile {
            resolve(context, uri)?.use {
                if (it.moveToFirst()) {
                    return TreeDocumentFile(parent, context, original, uri, Data(it, CursorIndex(it)))
                }
            }

            throw IOException("Could not encapsulate the data.")
        }

        fun from(parent: DocumentFile, context: Context, cursor: Cursor, index: CursorIndex): TreeDocumentFile {
            val data = Data(cursor, index)
            val uri = DocumentsContract.buildDocumentUriUsingTree(parent.getUri(), data.id)

            return TreeDocumentFile(parent, context, uri, uri, data)
        }

        fun from(
            parent: DocumentFile?,
            context: Context,
            original: Uri,
            uri: Uri,
            cursor: Cursor,
            index: CursorIndex,
        ) = TreeDocumentFile(parent, context, original, uri, Data(cursor, index))

        private fun resolve(context: Context, uri: Uri) = context.contentResolver.query(
            uri, null, null, null, null
        )

        private fun sync(context: Context, uri: Uri, data: Data) {
            data.exists = false

            resolve(context, uri)?.use {
                if (it.moveToFirst()) {
                    return data.update(it, CursorIndex(it))
                }
            }

            throw IOException("Syncing with latest data failed")
        }
    }
}