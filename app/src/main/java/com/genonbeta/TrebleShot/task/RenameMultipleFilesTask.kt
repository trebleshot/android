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
package com.genonbeta.TrebleShot.task

import android.content.*
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.FileListAdapter
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.util.Files
import com.genonbeta.android.framework.io.DocumentFile
import java.util.*

class RenameMultipleFilesTask(fileList: List<FileHolder>, renameTo: String) : AsyncTask() {
    private val mList: List<FileHolder>
    private val mNewName: String
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        if (mList.size <= 0) return
        progress().addToTotal(mList.size)
        val scannerList: MutableList<DocumentFile> = ArrayList()
        for (i in mList.indices) {
            throwIfStopped()
            val fileHolder: FileHolder = mList[i]
            ongoingContent = fileHolder.friendlyName
            progress().addToCurrent(1)
            publishStatus()
            if (fileHolder.file == null) continue
            var ext = Files.getFileFormat(fileHolder.file.getName())
            ext = if (ext != null) String.format(".%s", ext) else ""
            renameFile(kuick(), fileHolder, String.format("%s%s", String.format(mNewName, i), ext), scannerList)
        }
        notifyFileChanges(context, scannerList)
    }

    override fun getName(context: Context?): String? {
        return getContext().getString(R.string.text_renameMultipleItems)
    }

    companion object {
        fun notifyFileChanges(context: Context?, scannerList: List<DocumentFile>) {
            if (scannerList.size < 1) return
            val paths = arrayOfNulls<String>(scannerList.size)
            val mimeTypes = arrayOfNulls<String>(scannerList.size)
            for (i in scannerList.indices) {
                val file = scannerList[i]
                paths[i] = file.originalUri.toString()
                mimeTypes[i] = file.type
            }
            MediaScannerConnection.scanFile(context, paths, mimeTypes, null)
            context!!.sendBroadcast(Intent(FileListFragment.Companion.ACTION_FILE_RENAME_COMPLETED))
        }

        fun renameFile(
            kuick: Kuick?, holder: FileHolder, renameTo: String,
            scannerList: MutableList<DocumentFile>
        ): Boolean {
            try {
                if (FileListAdapter.FileHolder.Type.Bookmarked == holder.getType() || FileListAdapter.FileHolder.Type.Mounted == holder.getType()) {
                    holder.friendlyName = renameTo
                    kuick.publish<Any, FileHolder>(holder)
                    kuick.broadcast()
                    return true
                } else if (holder.file != null && holder.file.canWrite() && holder.file.renameTo(renameTo)) {
                    scannerList.add(holder.file)
                    return true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return false
        }
    }

    init {
        mList = fileList
        mNewName = renameTo
    }
}