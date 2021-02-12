/*
 * Copyright (C) 2019 Veli Tasalı
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
package com.genonbeta.TrebleShot.util

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.Files.fetchFile
import com.genonbeta.android.framework.util.Files.fromUri
import com.genonbeta.android.framework.util.Files.getUniqueFileName
import com.genonbeta.android.framework.util.Stoppable
import java.io.File
import java.io.IOException

object Files {
    @Throws(Exception::class)
    fun copy(context: Context, source: DocumentFile, destination: DocumentFile, stoppable: Stoppable) = Files.copy(
        context, source, destination, stoppable, AppConfig.BUFFER_LENGTH_DEFAULT, AppConfig.DEFAULT_TIMEOUT_SOCKET
    )

    fun getApplicationDirectory(context: Context): DocumentFile {
        val defaultPath = getDefaultApplicationDirectoryPath(context)
        val defaultPreferences = AppUtils.getDefaultPreferences(context)
        if (defaultPreferences.contains("storage_path")) {
            try {
                val savePath = fromUri(
                    context, Uri.parse(
                        defaultPreferences.getString(
                            "storage_path",
                            null
                        )
                    )
                )
                if (savePath.isDirectory() && savePath.canWrite()) return savePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (defaultPath.isFile) defaultPath.delete()
        if (!defaultPath.isDirectory) defaultPath.mkdirs()
        return DocumentFile.fromFile(defaultPath)
    }

    fun getDefaultApplicationDirectoryPath(context: Context): File {
        if (Build.VERSION.SDK_INT >= 29)
            return context.externalCacheDir!!

        var primaryDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!primaryDir.isDirectory && !primaryDir.mkdirs() || !primaryDir.canWrite()) primaryDir =
            Environment.getExternalStorageDirectory()
        return File(primaryDir.toString() + File.separator + context!!.getString(R.string.text_appName))
    }

    fun getFileFormat(fileName: String): String? {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) fileName.substring(lastDot + 1).toLowerCase() else null
    }

    @Throws(IOException::class)
    fun getIncomingPseudoFile(
        context: Context, item: TransferItem, transfer: Transfer, createIfNeeded: Boolean,
    ): DocumentFile {
        return fetchFile(getSavePath(context, transfer), item.directory, item.mimeType, item.file, createIfNeeded)
    }

    @Throws(IOException::class)
    fun getIncomingFile(context: Context, transferItem: TransferItem, transfer: Transfer): DocumentFile {
        val pseudoFile = getIncomingPseudoFile(context, transferItem, transfer, true)
        if (!pseudoFile.canWrite()) throw IOException("File cannot be created or you don't have permission write on it")
        return pseudoFile
    }

    fun getReadableUri(uri: String): String {
        return getReadableUri(Uri.parse(uri), uri)
    }

    fun getReadableUri(uri: Uri): String {
        return getReadableUri(uri, uri.toString())
    }

    fun getReadableUri(uri: Uri, defaultValue: String): String {
        return uri.path ?: defaultValue
    }

    @Throws(Exception::class)
    fun move(
        context: Context, targetFile: DocumentFile, destinationFile: DocumentFile,
        stoppable: Stoppable?,
    ): Boolean = Files.move(
        context, targetFile, destinationFile, stoppable, AppConfig.BUFFER_LENGTH_DEFAULT,
        AppConfig.DEFAULT_TIMEOUT_SOCKET
    )

    fun getSavePath(context: Context, transfer: Transfer): DocumentFile {
        val defaultFolder = getApplicationDirectory(context)
        if (transfer.savePath != null) {
            try {
                val savePath = fromUri(context, Uri.parse(transfer.savePath))
                if (savePath.isDirectory() && savePath.canWrite()) return savePath
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            transfer.savePath = defaultFolder.getUri().toString()
            AppUtils.getKuick(context).publish(transfer)
        }
        return defaultFolder
    }

    fun openUriForeground(context: Context, file: DocumentFile): Boolean {
        if (!Files.openUri(context, file)) {
            Toast.makeText(
                context, context.getString(R.string.mesg_openFailure, file.getName()), Toast.LENGTH_SHORT
            ).show()
            return false
        }
        return true
    }

    /**
     * When the transfer is done, this saves the uniquely named file to its actual name held in [TransferItem].
     *
     * @param savePath     The save path that contains currentFile
     * @param currentFile  The file that should be renamed
     * @param transferItem The transfer request
     * @return File moved to its actual name
     * @throws IOException Thrown when rename fails
     */
    @Throws(Exception::class)
    fun saveReceivedFile(
        savePath: DocumentFile,
        currentFile: DocumentFile,
        transferItem: TransferItem,
    ): DocumentFile {
        val uniqueName = getUniqueFileName(savePath, transferItem.name, true)
        val renamedFile = currentFile.renameTo(uniqueName) ?: throw IOException("Failed to rename object: $currentFile")

        // FIXME: 7/30/19 The rename always fails when renaming TreeDocumentFile (changed the rename method, did it fix?)
        // also don't forget to use moveDocument functions.

        transferItem.file = uniqueName
        return renamedFile
    }
}