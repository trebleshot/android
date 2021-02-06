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

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import android.content.Context
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.text.TextUtils
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.io.FileNotFoundException
import java.lang.Exception

/**
 * created by: Veli
 * date: 17.02.2018 22:01
 */
@RequiresApi(21)
class TreeDocumentFile : DocumentFile {
    private var mContext: Context?
    private var mUri: Uri? = null
    private var mId: String? = null
    private var mName: String? = null
    private var mType: String? = null
    private var mLength: Long = 0
    private var mFlags: Long = 0
    private var mLastModified: Long = 0
    private var mExists = false

    constructor(parent: DocumentFile?, context: Context?, uri: Uri?, original: Uri?) : super(parent, original) {
        mContext = context
        mUri = uri
        sync()
    }

    constructor(parent: DocumentFile?, context: Context?, cursor: Cursor?) : super(parent, null) {
        mContext = context
        if (loadFrom(cursor)) mUri = DocumentsContract.buildDocumentUriUsingTree(parent.getUri(), mId)
        originalUri = mUri
    }

    override fun createFile(mimeType: String?, displayName: String?): DocumentFile? {
        try {
            val newFile = DocumentsContract.createDocument(mContext.getContentResolver(), mUri, mimeType, displayName)
            if (newFile != null) return TreeDocumentFile(this, mContext, newFile, newFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun createDirectory(displayName: String?): DocumentFile? {
        return createFile(DocumentsContract.Document.MIME_TYPE_DIR, displayName)
    }

    fun getFlags(): Long {
        return mFlags
    }

    override fun getUri(): Uri? {
        return mUri
    }

    override fun getName(): String? {
        return mName
    }

    override fun getType(): String? {
        return mType
    }

    override fun isDirectory(): Boolean {
        return DocumentsContract.Document.MIME_TYPE_DIR == mType
    }

    override fun isFile(): Boolean {
        return DocumentsContract.Document.MIME_TYPE_DIR != mType || TextUtils.isEmpty(mType)
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    override fun isVirtual(): Boolean {
        return getFlags() and DocumentsContract.Document.FLAG_VIRTUAL_DOCUMENT != 0L
    }

    override fun lastModified(): Long {
        return mLastModified
    }

    override fun length(): Long {
        return mLength
    }

    protected fun loadFrom(cursor: Cursor?): Boolean {
        if (cursor == null) return false
        val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
        val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        val sizeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
        val typeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
        val flagIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_FLAGS)
        val modifiedIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_LAST_MODIFIED)
        if (idIndex == -1 || nameIndex == -1 || sizeIndex == -1 || typeIndex == -1 || flagIndex == -1 || modifiedIndex == -1) return false
        mId = cursor.getString(idIndex)
        mName = cursor.getString(nameIndex)
        mLastModified = cursor.getLong(modifiedIndex)
        mLength = cursor.getLong(sizeIndex)
        mType = cursor.getString(typeIndex)
        mFlags = cursor.getLong(flagIndex)
        mExists = true
        return true
    }

    override fun canRead(): Boolean {
        return (mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED)
    }

    override fun canWrite(): Boolean {
        return (mContext.checkCallingOrSelfUriPermission(mUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                == PackageManager.PERMISSION_GRANTED)
    }

    override fun delete(): Boolean {
        try {
            return DocumentsContract.deleteDocument(mContext.getContentResolver(), mUri)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    override fun exists(): Boolean {
        return mExists
    }

    override fun listFiles(): Array<DocumentFile?>? {
        try {
            val treeUri = DocumentsContract.buildChildDocumentsUriUsingTree(mUri, DocumentsContract.getDocumentId(mUri))
            val cursor = mContext.getContentResolver().query(
                treeUri, null, null, null,
                null, null
            )
            if (cursor == null || !cursor.moveToFirst()) return arrayOfNulls<DocumentFile?>(0)
            val resultFiles = arrayOfNulls<DocumentFile?>(cursor.count)
            do {
                resultFiles[cursor.position] = TreeDocumentFile(this, mContext, cursor)
            } while (cursor.moveToNext())
            DocumentFile.Companion.closeQuietly(cursor)
            return resultFiles
        } catch (e: Exception) {
        }
        return arrayOf()
    }

    override fun renameTo(displayName: String?): Boolean {
        try {
            return DocumentsContract.renameDocument(mContext.getContentResolver(), mUri, displayName) != null
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        }
        return false
    }

    @Throws(Exception::class)
    override fun sync() {
        mExists = false
        val resolver = mContext.getContentResolver()
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(mUri, null, null, null, null)
            if (cursor != null && cursor.moveToFirst() && loadFrom(cursor)) return
        } catch (e: Exception) {
            Log.w(DocumentFile.Companion.TAG, "Failed query: $e")
            throw e
        } finally {
            DocumentFile.Companion.closeQuietly(cursor)
        }
        throw Exception("Failed to sync()")
    }
}