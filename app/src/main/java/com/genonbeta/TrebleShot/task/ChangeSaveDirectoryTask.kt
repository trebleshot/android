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
import android.net.Uri
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.util.*
import java.io.IOException
import java.util.*

class ChangeSaveDirectoryTask(
    private val transfer: Transfer, private val newSavePath: Uri, var skipMoving: Boolean = false,
) : AsyncTask() {
    override fun onRun() {
        app.interruptTasksBy(FileTransferTask.identifyWith(transfer.id, TransferItem.Type.INCOMING), true)
        val checkList = AppUtils.getKuick(context).castQuery(
            Transfers.createIncomingSelection(transfer.id), TransferItem::class.java
        )
        val pseudoGroup = Transfer(transfer.id)
        progress.addToTotal(checkList.size)
        try {
            if (!skipMoving) {
                // Illustrate new change to build the structure accordingly
                kuick.reconstruct(pseudoGroup)
                pseudoGroup.savePath = newSavePath.toString()
                val erredFiles: MutableList<TransferItem> = ArrayList()
                for (transferItem in checkList) {
                    throwIfStopped()
                    progress.addToCurrent(1)
                    ongoingContent = transferItem.name
                    publishStatus()
                    try {
                        val file = Files.getIncomingPseudoFile(
                            context, transferItem, transfer,
                            false
                        )
                        val pseudoFile = Files.getIncomingPseudoFile(
                            context, transferItem,
                            pseudoGroup, true
                        )
                        try {
                            if (file.canWrite()) Files.move(
                                context,
                                file,
                                pseudoFile,
                                this
                            ) else throw IOException("Failed to access: " + file.uri)
                        } catch (e: Exception) {
                            erredFiles.add(transferItem)
                        }
                    } catch (ignored: Exception) {
                    }
                }
                if (erredFiles.size > 0) {
                    val fileNames = StringBuilder("\n")
                    for (item in erredFiles) fileNames.append("\n")
                        .append(item.name)
                    post(
                        TaskMessage.newInstance()
                            .setTitle(name)
                            .setMessage(context.getString(R.string.mesg_errorMoveFile, fileNames.toString()))
                    )
                }
            }
            transfer.savePath = newSavePath.toString()
            kuick.publish(transfer)
            kuick.broadcast()
            context.sendBroadcast(
                Intent(ACTION_SAVE_PATH_CHANGED)
                    .putExtra(EXTRA_TRANSFER, transfer)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.butn_changeSavePath)
    }

    companion object {
        const val ACTION_SAVE_PATH_CHANGED = "org.monora.trebleshot.intent.action.SAVE_PATH_CHANGED"
        const val EXTRA_TRANSFER = "extraTransfer"
    }
}