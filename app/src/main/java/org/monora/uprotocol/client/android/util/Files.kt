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
package org.monora.uprotocol.client.android.util

import android.app.ActivityManager
import android.app.Service
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.Files.fetchFile
import com.genonbeta.android.framework.util.Files.fromUri
import com.genonbeta.android.framework.util.Files.getUniqueFileName
import com.genonbeta.android.framework.util.Stoppable
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig.BUFFER_LENGTH_DEFAULT
import org.monora.uprotocol.client.android.config.AppConfig.DEFAULT_TIMEOUT_SOCKET
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.core.transfer.TransferItem
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

object Files {
    @Throws(Exception::class)
    fun copy(context: Context, source: DocumentFile, destination: DocumentFile, stoppable: Stoppable) = Files.copy(
        context, source, destination, stoppable, BUFFER_LENGTH_DEFAULT, DEFAULT_TIMEOUT_SOCKET
    )

    fun createLog(context: Context): DocumentFile? {
        val saveDirectory = getApplicationDirectory(context)
        val logFile = saveDirectory.createFile("text/plain", "trebleshot_log") ?: return null
        val activityManager = context.getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager

        if (logFile.exists()) logFile.delete()

        try {
            val processList = activityManager.runningAppProcesses
            val command = "logcat -d -v threadtime *:*"
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val outputStream = context.contentResolver
                .openOutputStream(logFile.getUri(), "w") ?: throw IOException("Open failed " + logFile.getName())
            var readLine: String
            while (reader.readLine().also { readLine = it } != null)
                for (processInfo in processList) if (readLine.contains(processInfo.pid.toString())) {
                    outputStream.write(readLine.toByteArray())
                    outputStream.flush()
                    break
                }
            outputStream.close()
            reader.close()
            return logFile
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    fun getApplicationDirectory(context: Context): DocumentFile {
        val defaultPath = getDefaultApplicationDirectoryPath(context)
        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (defaultPreferences.contains("storage_path")) {
            try {
                val savePath = fromUri(
                    context, Uri.parse(defaultPreferences.getString("storage_path", null))
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
        if (!primaryDir.isDirectory && !primaryDir.mkdirs() || !primaryDir.canWrite()) {
            primaryDir = Environment.getExternalStorageDirectory()
        }
        return File(primaryDir.toString() + File.separator + context.getString(R.string.text_appName))
    }

    fun getFileFormat(fileName: String): String? {
        val lastDot = fileName.lastIndexOf('.')
        return if (lastDot >= 0) fileName.substring(lastDot + 1).lowercase(Locale.getDefault()) else null
    }

    @Throws(IOException::class)
    fun getIncomingPseudoFile(
        context: Context, item: UTransferItem, transfer: Transfer, createIfNeeded: Boolean,
    ): DocumentFile = fetchFile(
        getSavePath(context, transfer), item.itemDirectory, item.itemMimeType, item.location, createIfNeeded
    )

    @Throws(IOException::class)
    fun getIncomingFile(context: Context, transferItem: UTransferItem, transfer: Transfer): DocumentFile {
        val pseudoFile = getIncomingPseudoFile(context, transferItem, transfer, true)
        if (!pseudoFile.canWrite()) {
            throw IOException("File cannot be created or you don't have permission write to it")
        }
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
        context: Context, targetFile: DocumentFile, destinationFile: DocumentFile, stoppable: Stoppable?,
    ): Boolean = Files.move(
        context, targetFile, destinationFile, stoppable, BUFFER_LENGTH_DEFAULT, DEFAULT_TIMEOUT_SOCKET
    )

    fun getSavePath(context: Context, transfer: Transfer): DocumentFile {
        val defaultFolder = getApplicationDirectory(context)

        try {
            val saveLocation = fromUri(context, Uri.parse(transfer.location))
            if (saveLocation.isDirectory() && saveLocation.canWrite()) {
                return saveLocation
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    @Throws(Exception::class)
    fun saveReceivedFile(
        savePath: DocumentFile,
        currentFile: DocumentFile,
        transferItem: TransferItem,
    ): DocumentFile {
        val name = getUniqueFileName(savePath, transferItem.itemName, true)
        val renamedFile = currentFile.renameTo(name) ?: throw IOException("Failed to rename object: $currentFile")

        // FIXME: 7/30/19 The rename always fails when renaming TreeDocumentFile (changed the rename method, did it fix?)
        // also don't forget to use moveDocument functions.
        if (transferItem is UTransferItem) {
            transferItem.location = renamedFile.getUri().toString()
        }
        return renamedFile
    }
}