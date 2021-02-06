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
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.RuntimeException

/**
 * created by: Veli
 * date: 17.02.2018 22:36
 */
abstract class DocumentFile(private val mParent: DocumentFile?, originalUri: Uri?) {
    private var mOriginalUri: Uri? = null
    override fun equals(obj: Any?): Boolean {
        return obj is DocumentFile && getUri() != null && getUri() == (obj as DocumentFile?).getUri()
    }

    fun getOriginalUri(): Uri? {
        return mOriginalUri
    }

    protected fun setOriginalUri(uri: Uri?) {
        mOriginalUri = uri
    }

    abstract fun createFile(mimeType: String?, displayName: String?): DocumentFile?
    abstract fun createDirectory(displayName: String?): DocumentFile?
    abstract fun getUri(): Uri?
    abstract fun getName(): String?
    abstract fun getType(): String?
    open fun getParentFile(): DocumentFile? {
        return mParent
    }

    abstract fun isDirectory(): Boolean
    abstract fun isFile(): Boolean
    abstract fun isVirtual(): Boolean
    abstract fun lastModified(): Long
    abstract fun length(): Long
    abstract fun canRead(): Boolean
    abstract fun canWrite(): Boolean
    abstract fun delete(): Boolean
    abstract fun exists(): Boolean
    abstract fun listFiles(): Array<DocumentFile?>?
    open fun findFile(displayName: String?): DocumentFile? {
        for (doc in listFiles()) {
            if (displayName == doc.getName()) {
                return doc
            }
        }
        return null
    }

    abstract fun renameTo(displayName: String?): Boolean
    @Throws(Exception::class)
    abstract fun sync()

    companion object {
        val TAG: String? = DocumentFile::class.java.simpleName
        fun fromFile(file: File?): DocumentFile? {
            return LocalDocumentFile(null, file)
        }

        @Throws(FileNotFoundException::class)
        fun fromUri(context: Context?, uri: Uri?, prepareTree: Boolean): DocumentFile? {
            if (Build.VERSION.SDK_INT >= 21) try {
                return TreeDocumentFile(null, context, if (prepareTree) prepareUri(uri) else uri, uri)
            } catch (e: Exception) {
                // it was expected it might not be TreeDocumentFile
            }
            try {
                return StreamDocumentFile(StreamInfo(context, uri), uri)
            } catch (e: Exception) {
                // Now something is wrong
            }
            throw FileNotFoundException("Failed to create right connection for " + uri.toString())
        }

        protected fun closeQuietly(closeable: Closeable?) {
            if (closeable != null) {
                try {
                    closeable.close()
                } catch (rethrown: RuntimeException) {
                    throw rethrown
                } catch (ignored: Exception) {
                }
            }
        }

        @RequiresApi(21)
        protected fun prepareUri(treeUri: Uri?): Uri? {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri))
        }
    }

    init {
        setOriginalUri(originalUri)
    }
}