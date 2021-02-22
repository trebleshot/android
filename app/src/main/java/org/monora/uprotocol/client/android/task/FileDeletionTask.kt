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
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.adapter.FileListAdapter.FileHolder
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import com.genonbeta.android.framework.io.DocumentFile
import java.util.*

class FileDeletionTask(list: List<FileHolder>) : AsyncTask() {
    private val list: List<FileHolder> = ArrayList(list)

    @Throws(TaskStoppedException::class)
    override fun onRun() {
        progress.increaseTotalBy(list.size)

        val successfulList: MutableList<DocumentFile> = ArrayList()

        for (holder in list) {
            throwIfStopped()
            // FIXME: 2/21/21 FileHolder does not contain the file field. Also file deletion should be generalized class
            /*val file = holder.file
                if (file != null) {
                    if (file.isFile()) deleteFile(file, successfulList) else deleteDirectory(file, successfulList)
                }*/
        }
        RenameMultipleFilesTask.notifyFileChanges(context, successfulList)
    }

    @Throws(TaskStoppedException::class)
    fun deleteDirectory(folder: DocumentFile, successfulList: MutableList<DocumentFile>) {
        val files = folder.listFiles()
        progress.increaseTotalBy(files.size)
        for (file in files) {
            throwIfStopped()
            ongoingContent = file.getName()
            if (file.isFile()) deleteFile(file, successfulList) else if (file.isDirectory()) deleteDirectory(
                file,
                successfulList
            )
        }
        deleteFile(folder, successfulList)
    }

    fun deleteFile(file: DocumentFile, successfulList: MutableList<DocumentFile>) {
        progress.increaseBy(1)
        if (file.delete()) successfulList.add(file)
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_deletingFilesOngoing)
    }
}