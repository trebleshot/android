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
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.util.FileUtils
import java.io.*
import java.lang.Exception
import java.net.URI

/**
 * created by: Veli
 * date: 4.10.2017 12:36
 */
class StreamInfo {
    var friendlyName: String? = null
    var mimeType: String? = null
    var uri: Uri? = null
    var type: Type? = null
    var size: Long = 0
    var file: File? = null
    private var mResolver: ContentResolver? = null

    constructor() {}
    constructor(context: Context?, uri: Uri?) {
        if (!loadStream(
                context,
                uri
            )
        ) throw StreamCorruptedException("Content was not able to route a stream. Empty result is returned")
    }

    @Throws(FolderStateException::class, FileNotFoundException::class)
    private fun loadStream(context: Context?, uri: Uri?): Boolean {
        val uriType = uri.toString()
        this.uri = uri
        if (uriType.startsWith("content")) {
            mResolver = context.getContentResolver()
            val cursor = mResolver.query(uri, null, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIndex != -1 && sizeIndex != -1) {
                        friendlyName = cursor.getString(nameIndex)
                        size = cursor.getLong(sizeIndex)
                        mimeType = mResolver.getType(uri)
                        type = Type.Stream
                        return true
                    }
                }
                cursor.close()
            }
        } else if (uriType.startsWith("file")) {
            val file = File(URI.create(uriType))
            if (file.canRead()) {
                if (file.isDirectory) throw FolderStateException()
                friendlyName = file.name
                size = file.length()
                mimeType = FileUtils.Companion.getFileContentType(file.name)
                type = Type.File
                this.file = file
                return true
            }
        }
        return false
    }

    fun getContentResolver(): ContentResolver? {
        return mResolver
    }

    @Throws(FileNotFoundException::class)
    fun openOutputStream(): OutputStream? {
        return if (file == null) getContentResolver().openOutputStream(uri, "wa") else FileOutputStream(file, true)
    }

    @Throws(FileNotFoundException::class)
    fun openInputStream(): InputStream? {
        return if (file == null) getContentResolver().openInputStream(uri) else FileInputStream(file)
    }

    enum class Type {
        Stream, File
    }

    class FolderStateException : Exception()
    companion object {
        @Throws(FileNotFoundException::class, StreamCorruptedException::class, FolderStateException::class)
        fun getStreamInfo(context: Context?, uri: Uri?): StreamInfo? {
            return StreamInfo(context, uri)
        }
    }
}