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

package org.monora.uprotocol.client.android.protocol

import android.util.Log
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.runBlocking
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.content.scan
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.io.DocumentFileStreamDescriptor
import org.monora.uprotocol.client.android.service.backgroundservice.Task
import org.monora.uprotocol.client.android.task.transfer.TransferParams
import org.monora.uprotocol.core.io.StreamDescriptor
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.TransferOperation

class MainTransferOperation(
    private val backend: Backend,
    private val transferRepository: TransferRepository,
    private val transferParams: TransferParams,
    private val state: MutableLiveData<Task.State>,
    private val cancellationCallback: () -> Unit,
) : TransferOperation {
    private var speedCalcTime = 0L

    private var bytesIncreaseInSec = 0L

    override fun clearBytesOngoing() {
        transferParams.bytesOngoing = 0
    }

    override fun clearOngoing() {
        transferParams.ongoing = null
    }

    override fun finishOperation() {
        if (count > 0) {
            backend.services.notifications.notifyFileReceived(transferParams)
        }
    }

    override fun getBytesOngoing(): Long = transferParams.bytesOngoing

    override fun getBytesTotal(): Long = transferParams.bytesSessionTotal

    override fun getCount(): Int = transferParams.count

    override fun getOngoing(): TransferItem? = transferParams.ongoing

    override fun installReceivedContent(descriptor: StreamDescriptor) {
        val ongoing = ongoing

        when {
            ongoing == null -> Log.d(TAG, "installReceivedContent: Ongoing item was empty!")
            descriptor is DocumentFileStreamDescriptor -> {
                val savedFile = transferRepository.saveReceivedFile(
                    transferParams.transfer, descriptor.documentFile, ongoing
                )
                if (ongoing is UTransferItem) runBlocking {
                    transferRepository.update(ongoing)
                }
                backend.services.mediaScannerConnection.scan(savedFile)
            }
            else -> Log.d(TAG, "installReceivedContent: Unknown descriptor type to save: $descriptor")
        }
    }

    override fun onCancelOperation() {
        Log.d(TAG, "Operation cancelled by one of the two sides")
    }

    override fun onUnhandledException(e: Exception) {
        state.postValue(Task.State.Error(e))
    }

    override fun publishProgress() {
        val total = transferParams.bytesTotal.takeIf { it > 0 } ?: return
        val transferred = (bytesTotal + bytesOngoing).takeIf { it > 0 } ?: return
        val itemName = ongoing?.itemName ?: return
        val progress = Task.State.Progress(itemName, 1000, ((transferred.toDouble() / total) * 1000).toInt())

        state.postValue(progress)

        transferParams.job?.let {
            if (it.isCancelled) cancellationCallback()
        }
    }

    override fun setBytesOngoing(bytes: Long, bytesIncrease: Long) {
        transferParams.bytesOngoing = bytes

        if (System.nanoTime() - speedCalcTime > 1e9) {
            transferParams.averageSpeed = bytesIncreaseInSec
            bytesIncreaseInSec = 0
            speedCalcTime = System.nanoTime()
        } else {
            bytesIncreaseInSec += bytesIncrease
        }
    }

    override fun setBytesTotal(bytes: Long) {
        transferParams.bytesSessionTotal = bytes
    }

    override fun setCount(count: Int) {
        transferParams.count = count
    }

    override fun setOngoing(transferItem: TransferItem) {
        transferParams.ongoing = transferItem
    }

    companion object {
        private const val TAG = "MainTransferOperation"
    }
}
