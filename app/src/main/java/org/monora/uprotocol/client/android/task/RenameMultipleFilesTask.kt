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
package org.monora.uprotocol.client.android.task

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.FileListAdapter.FileHolder
import org.monora.uprotocol.client.android.fragment.FileListFragment
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import com.genonbeta.android.framework.io.DocumentFile
import java.util.*

class RenameMultipleFilesTask(val fileList: List<FileHolder>, val renameTo: String) : AsyncTask() {
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        if (fileList.isEmpty())
            return

        progress.increaseTotalBy(fileList.size)

        val scannerList: MutableList<DocumentFile> = ArrayList()

        for (i in fileList.indices) {
            throwIfStopped()

            // FIXME: 2/21/21 Renaming objects
            /*val fileHolder = fileList[i]
            ongoingContent = fileHolder.friendlyName
            progress.increaseBy(1)

            if (fileHolder.file == null)
                continue
            var ext = Files.getFileFormat(fileHolder.file.getName())
            ext = if (ext != null) String.format(".%s", ext) else ""
            renameFile(kuick, fileHolder, String.format("%s%s", String.format(renameTo, i), ext), scannerList)*/
        }
        notifyFileChanges(context, scannerList)
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_renameMultipleItems)
    }

    companion object {
        fun notifyFileChanges(context: Context, scannerList: List<DocumentFile>) {
            if (scannerList.isEmpty())
                return

            val paths = arrayOfNulls<String>(scannerList.size)
            val mimeTypes = arrayOfNulls<String>(scannerList.size)
            for (i in scannerList.indices) {
                val file = scannerList[i]
                paths[i] = file.originalUri.toString()
                mimeTypes[i] = file.getType()
            }
            MediaScannerConnection.scanFile(context, paths, mimeTypes, null)
            context.sendBroadcast(Intent(FileListFragment.ACTION_FILE_RENAME_COMPLETED))
        }

        fun renameFile(
            holder: FileHolder, renameTo: String, scannerList: MutableList<DocumentFile>,
        ): Boolean {
            try {
                // FIXME: 2/21/21 File renaming
                /*if (FileHolder.Type.Bookmarked == holder.type || FileHolder.Type.Mounted == holder.type) {
                    holder.friendlyName = renameTo
                    kuick.publish(holder)
                    kuick.broadcast()
                    return true
                } else if (holder.file != null && holder.file.canWrite()) {
                    val newTarget = holder.file.renameTo(renameTo)
                    if (newTarget != null) {
                        scannerList.add(newTarget)
                        return true
                    }
                }*/
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    }
}