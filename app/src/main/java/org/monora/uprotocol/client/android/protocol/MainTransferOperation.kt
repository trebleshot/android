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
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.io.DocumentFileStreamDescriptor
import org.monora.uprotocol.client.android.util.TAG
import org.monora.uprotocol.core.io.StreamDescriptor
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.TransferOperation

class MainTransferOperation(val backend: Backend) : TransferOperation {
    private var ongoing: TransferItem? = null

    private var bytesOngoing: Long = 0

    private var bytesTotal: Long = 0

    private var count = 0

    override fun clearBytesOngoing() {
        bytesOngoing = 0
    }

    override fun clearOngoing() {
        ongoing = null
    }

    override fun finishOperation() {
        Log.d(TAG, "finishOperation: ")
        if (count > 0) {
            // TODO: 7/16/21 Fix transfer completed notification
            //backend.notifications.notifyFileReceived(task, Files.getSavePath(task.context, task.transfer))
        }
    }

    override fun getBytesOngoing(): Long = bytesOngoing

    override fun getBytesTotal(): Long = bytesTotal

    override fun getCount(): Int = count

    override fun getOngoing(): TransferItem? = ongoing

    override fun installReceivedContent(descriptor: StreamDescriptor) {
        Log.d(TAG, "installReceivedContent: $descriptor")
        if (descriptor is DocumentFileStreamDescriptor) {

        }
    }

    override fun onCancelOperation() {
        Log.d(TAG, "onCancelOperation: ")
    }

    override fun onUnhandledException(e: Exception) {
        e.printStackTrace()
    }

    override fun publishProgress() {

    }

    override fun setBytesOngoing(bytes: Long, bytesIncrease: Long) {
        bytesOngoing = bytes
    }

    override fun setBytesTotal(bytes: Long) {
        bytesTotal = bytes
    }

    override fun setCount(count: Int) {
        this.count = count
    }

    override fun setOngoing(transferItem: TransferItem) {
        ongoing = transferItem
    }
}