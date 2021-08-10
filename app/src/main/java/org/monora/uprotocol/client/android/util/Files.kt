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
import androidx.preference.PreferenceManager
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files.fetchFile
import com.genonbeta.android.framework.util.Files.getUniqueFileName
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.core.transfer.TransferItem
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

object Files {
    fun createLog(context: Context): DocumentFile? {
        val target = getAppDirectory(context)
        val logFile = target.createFile(context, "text/plain", "trebleshot_log") ?: return null
        val activityManager = context.getSystemService(Service.ACTIVITY_SERVICE) as ActivityManager

        if (logFile.exists()) logFile.delete(context)

        try {
            val processList = activityManager.runningAppProcesses
            val command = "logcat -d -v threadtime *:*"
            val process = Runtime.getRuntime().exec(command)
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val outputStream = context.contentResolver.openOutputStream(
                logFile.getUri(), "w"
            ) ?: throw IOException("Open failed " + logFile.getName())
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

    fun getAppDirectory(context: Context): DocumentFile {
        val defaultPath = getDefaultApplicationDirectoryPath(context)
        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        if (defaultPreferences.contains("storage_path")) {
            try {
                val savePath = DocumentFile.fromUri(
                    context, Uri.parse(defaultPreferences.getString("storage_path", null)), false
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
        context, getSavePath(context, transfer), item.itemDirectory, item.itemMimeType, item.location, createIfNeeded
    )

    @Throws(IOException::class)
    fun getIncomingFile(context: Context, transferItem: UTransferItem, transfer: Transfer): DocumentFile {
        val pseudoFile = getIncomingPseudoFile(context, transferItem, transfer, true)
        if (!pseudoFile.canWrite()) {
            throw IOException("File cannot be created or you don't have permission write to it")
        }
        return pseudoFile
    }

    fun getSavePath(context: Context, transfer: Transfer): DocumentFile {
        val defaultFolder = getAppDirectory(context)

        try {
            val saveLocation = DocumentFile.fromUri(context, Uri.parse(transfer.location), false)
            if (saveLocation.isDirectory() && saveLocation.canWrite()) {
                return saveLocation
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return defaultFolder
    }

    @Throws(Exception::class)
    fun saveReceivedFile(
        context: Context,
        savePath: DocumentFile,
        currentFile: DocumentFile,
        transferItem: TransferItem,
    ): DocumentFile {
        val name = getUniqueFileName(context, savePath, transferItem.itemName)
        val renamedFile = currentFile.renameTo(context, name) ?: throw IOException("Failed to rename: $currentFile")

        // FIXME: 7/30/19 The rename always fails when renaming TreeDocumentFile (changed the rename method, did it fix?)
        // also don't forget to use moveDocument functions.
        if (transferItem is UTransferItem) {
            transferItem.location = renamedFile.getUri().toString()
        }
        return renamedFile
    }
}
