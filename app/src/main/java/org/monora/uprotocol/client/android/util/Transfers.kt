/*
 * Copyright (C) 2021 Veli Tasalı
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

import android.annotation.SuppressLint
import androidx.core.util.ObjectsCompat
import com.genonbeta.android.framework.io.DocumentFile
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.transfer.TransferItem
import java.io.File.separator

/**
 * created by: veli
 * date: 06.04.2018 17:01
 */
object Transfers {
    private val TAG = Transfers::class.simpleName

    @Throws(TaskStoppedException::class)
    fun createFolderStructure(
        list: MutableList<TransferItem>, transferId: Long, folder: DocumentFile,
        directory: String?, task: AsyncTask,
    ) {
        val files = folder.listFiles()
        if (files.isEmpty()) return
        task.progress.increaseTotalBy(files.size)
        for (file in files) {
            task.throwIfStopped()
            task.ongoingContent = file.getName()
            task.progress.increaseBy(1)
            if (file.isDirectory()) createFolderStructure(
                list, transferId, file, directory?.let { it + separator + file.getName() }, task
            ) else {
                list.add(
                    UTransferItem(
                        0,
                        transferId,
                        file.getName(),
                        file.getType(),
                        file.getLength(),
                        directory,
                        file.getUri().toString(),
                        TransferItem.Type.Outgoing,
                        PersistenceProvider.STATE_PENDING,
                    )
                )
            }
        }
    }

    @SuppressLint("DefaultLocale")
    fun createUniqueTransferId(transferId: Long, deviceId: String, type: TransferItem.Type): Int {
        return ObjectsCompat.hash(transferId, deviceId, type)
    }

    fun pauseTransfer(activity: Activity, transfer: Transfer) {
        pauseTransfer(activity, transfer.id, transfer.clientUid, transfer.type)
    }

    fun pauseTransfer(activity: Activity, transferId: Long, deviceId: String, type: TransferItem.Type) {
        // TODO: 2/26/21 Give this backend, please
        /*App.interruptTasksBy(
            activity, FileTransferTask.identifyWith(transferId, deviceId, type), true
        )*/
    }
}