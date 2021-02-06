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
import android.util.Log
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.io.File
import java.io.IOException
import java.util.ArrayList

/**
 * created by: Veli
 * date: 17.02.2018 23:39
 */
class LocalDocumentFile(parent: DocumentFile?, private var mFile: File?) : DocumentFile(
    parent, Uri.fromFile(
        mFile
    )
) {
    override fun createFile(mimeType: String?, displayName: String?): DocumentFile? {
        var displayName = displayName
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (extension != null) displayName += ".$extension"
        val target = File(mFile, displayName)
        try {
            target.createNewFile()
            return LocalDocumentFile(this, target)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }

    override fun createDirectory(displayName: String?): DocumentFile? {
        val target = File(mFile, displayName)
        return if (target.isDirectory || target.mkdir()) LocalDocumentFile(this, target) else null
    }

    fun getFile(): File? {
        return mFile
    }

    override fun getUri(): Uri? {
        return Uri.fromFile(mFile)
    }

    override fun getName(): String? {
        return mFile.getName()
    }

    override fun getParentFile(): DocumentFile? {
        val parentFile = mFile.getParentFile()
        return if (parentFile == null || File.separator == parentFile.absolutePath // hide root
        ) null else LocalDocumentFile(null, parentFile)
    }

    override fun getType(): String? {
        return if (mFile.isDirectory()) "*/*" else getTypeForName(mFile.getName())
    }

    override fun isDirectory(): Boolean {
        return mFile.isDirectory()
    }

    override fun isFile(): Boolean {
        return mFile.isFile()
    }

    override fun isVirtual(): Boolean {
        return false
    }

    override fun lastModified(): Long {
        return mFile.lastModified()
    }

    override fun length(): Long {
        return mFile.length()
    }

    override fun canRead(): Boolean {
        return mFile.canRead()
    }

    override fun canWrite(): Boolean {
        return mFile.canWrite()
    }

    override fun delete(): Boolean {
        deleteContents(mFile)
        return mFile.delete()
    }

    override fun exists(): Boolean {
        return mFile.exists()
    }

    override fun findFile(displayName: String?): DocumentFile? {
        val file = File(mFile, displayName)
        return if (file.exists()) LocalDocumentFile(this, file) else null
    }

    override fun listFiles(): Array<DocumentFile?>? {
        val results: MutableList<DocumentFile?> = ArrayList()
        val files = mFile.listFiles()
        if (files != null) for (file in files) results.add(LocalDocumentFile(this, file))
        return results.toTypedArray()
    }

    override fun renameTo(displayName: String?): Boolean {
        val target = File(mFile.getParentFile(), displayName)
        if (mFile.renameTo(target)) {
            mFile = target
            return true
        }
        return false
    }

    override fun sync() {}

    companion object {
        private fun getTypeForName(name: String?): String? {
            val lastDot = name.lastIndexOf('.')
            if (lastDot >= 0) {
                val extension = name.substring(lastDot + 1).toLowerCase()
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                if (mime != null) return mime
            }
            return "application/octet-stream"
        }

        private fun deleteContents(dir: File?): Boolean {
            val files = dir.listFiles()
            var success = true
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        success = success and deleteContents(file)
                    }
                    if (!file.delete()) {
                        Log.w(DocumentFile.Companion.TAG, "Failed to delete $file")
                        success = false
                    }
                }
            }
            return success
        }
    }
}