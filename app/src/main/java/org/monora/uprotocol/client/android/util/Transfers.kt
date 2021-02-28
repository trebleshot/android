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
    val TAG = Transfers::class.java.simpleName

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