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
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.io.File

/**
 * created by: Veli
 * date: 18.02.2018 00:24
 */
class StreamDocumentFile(private val mStream: StreamInfo?, original: Uri?) : DocumentFile(null, original) {
    override fun createFile(mimeType: String?, displayName: String?): DocumentFile? {
        return null
    }

    override fun createDirectory(displayName: String?): DocumentFile? {
        return null
    }

    override fun getUri(): Uri? {
        return mStream.uri
    }

    fun getFile(): File? {
        return mStream.file
    }

    override fun getName(): String? {
        return mStream.friendlyName
    }

    fun getStream(): StreamInfo? {
        return mStream
    }

    override fun getType(): String? {
        return mStream.mimeType
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

    override fun lastModified(): Long {
        return 0
    }

    override fun length(): Long {
        return mStream.size
    }

    override fun canRead(): Boolean {
        return true
    }

    override fun canWrite(): Boolean {
        return true
    }

    override fun delete(): Boolean {
        return false
    }

    override fun exists(): Boolean {
        return true
    }

    override fun listFiles(): Array<DocumentFile?>? {
        return arrayOfNulls<DocumentFile?>(0)
    }

    override fun renameTo(displayName: String?): Boolean {
        return false
    }

    override fun sync() {}
}