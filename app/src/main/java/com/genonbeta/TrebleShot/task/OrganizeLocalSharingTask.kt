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

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.TransferMemberActivity
import com.genonbeta.TrebleShot.activityimport.WebShareActivity
import com.genonbeta.TrebleShot.adapter.FileListAdapter
import com.genonbeta.TrebleShot.dataobject.Container
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage
import com.genonbeta.TrebleShot.service.backgroundservice.TaskStoppedException
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.Stoppable
import java.io.FileNotFoundException
import java.util.*

class OrganizeLocalSharingTask(
    var list: List<Shareable>,
    private val flagAddNewDevice: Boolean,
    private val flagWebShare: Boolean,
) : AttachableAsyncTask<AttachedTaskListener>() {
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        if (list.isEmpty())
            return

        val db: SQLiteDatabase = kuick.writableDatabase
        val transfer = Transfer(AppUtils.uniqueNumber.toLong())
        val list: MutableList<TransferItem> = ArrayList()

        progress.increaseTotalBy(this.list.size)

        for (shareable in this.list) {
            throwIfStopped()
            ongoingContent = shareable.fileName
            progress.increaseBy(1)

            val containable = if (shareable is Container) (shareable as Container).expand() else null

            if (shareable is FileListAdapter.FileHolder) {
                if (shareable.file.isDirectory()) Transfers.createFolderStructure(
                    list,
                    transfer.id,
                    shareable.file,
                    shareable.fileName,
                    this
                ) else
                    list.add(from(shareable.file, transfer.id, null))
            } else
                list.add(from(shareable, transfer.id, if (containable == null) null else shareable.friendlyName))
            if (containable != null) {
                progress.increaseTotalBy(containable.children.size)

                for (uri in containable.children) {
                    progress.increaseBy(1)
                    try {
                        list.add(
                            from(
                                Files.fromUri(context, uri),
                                transfer.id,
                                shareable.friendlyName
                            )
                        )
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }
        }
        if (list.size <= 0) {
            post(
                TaskMessage.newInstance(
                    context.getString(R.string.text_error),
                    context.getString(R.string.text_errorNoFileSelected)
                )
            )
            Log.d(TAG, "onRun: No content is located")
            return
        }
        addCloser(object : Stoppable.Closer {
            override fun onClose(userAction: Boolean) {
                kuick.remove(db, transfer, null, null)
            }
        })
        kuick.insert(db, list, transfer, progress)
        if (flagWebShare) {
            transfer.isServedOnWeb = true
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    R.string.text_transferSharedOnBrowser, Toast.LENGTH_SHORT
                ).show()
            }
        }
        kuick.insert(db, transfer, null, progress)
        if (flagWebShare) context.startActivity(
            Intent(context, WebShareActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        ) else
            TransferMemberActivity.startInstance(context, transfer, flagAddNewDevice)
        kuick.broadcast()
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.mesg_organizingFiles)
    }

    companion object {
        val TAG = OrganizeLocalSharingTask::class.java.simpleName
    }
}