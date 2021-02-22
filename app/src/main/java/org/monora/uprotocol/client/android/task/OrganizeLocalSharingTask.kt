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
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.TransferMemberActivity
import org.monora.uprotocol.client.android.activityimport.WebShareActivity
import org.monora.uprotocol.client.android.model.Transfer
import org.monora.uprotocol.client.android.model.TransferItem
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.monora.uprotocol.client.android.util.AppUtils
import com.genonbeta.android.framework.util.Stoppable
import org.monora.uprotocol.client.android.model.ContentModel
import java.util.*

class OrganizeLocalSharingTask(
    var list: List<ContentModel>, private val addNewDevice: Boolean, private val webShare: Boolean,
) : AttachableAsyncTask<AttachedTaskListener>() {
    @Throws(TaskStoppedException::class)
    override fun onRun() {
        if (list.isEmpty())
            return

        val db: SQLiteDatabase = kuick.writableDatabase
        val transfer = Transfer(AppUtils.uniqueNumber.toLong())
        val list: MutableList<TransferItem> = ArrayList()

        progress.increaseTotalBy(this.list.size)

        for (model in this.list) {
            throwIfStopped()
            ongoingContent = model.name()
            progress.increaseBy(1)

            // FIXME: 2/21/21 The items should be ready to share
            /*
            if (model is FileListAdapter.FileHolder) {
                if (model.file.isDirectory()) Transfers.createFolderStructure(
                    list,
                    transfer.id,
                    model.file,
                    model.fileName,
                    this
                ) else
                    list.add(from(model.file, transfer.id, null))
            } else
                list.add(from(model, transfer.id, if (containable == null) null else model.friendlyName))
            if (containable != null) {
                progress.increaseTotalBy(containable.children.size)

                for (uri in containable.children) {
                    progress.increaseBy(1)
                    try {
                        list.add(
                            from(
                                Files.fromUri(context, uri),
                                transfer.id,
                                model.friendlyName
                            )
                        )
                    } catch (e: FileNotFoundException) {
                        e.printStackTrace()
                    }
                }
            }*/
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
        if (webShare) {
            transfer.isServedOnWeb = true
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    R.string.text_transferSharedOnBrowser, Toast.LENGTH_SHORT
                ).show()
            }
        }
        kuick.insert(db, transfer, null, progress)
        if (webShare) context.startActivity(
            Intent(context, WebShareActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        ) else
            TransferMemberActivity.startInstance(context, transfer, addNewDevice)
        kuick.broadcast()
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.mesg_organizingFiles)
    }

    companion object {
        val TAG = OrganizeLocalSharingTask::class.java.simpleName
    }
}