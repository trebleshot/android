package org.monora.uprotocol.client.android.task.transfer

import org.monora.uprotocol.core.io.StreamDescriptor
import org.monora.uprotocol.core.transfer.TransferItem
import org.monora.uprotocol.core.transfer.TransferOperation
import java.lang.Exception

class DefaultTransferOperation : TransferOperation {
    override fun clearBytesOngoing() {
        TODO("Not yet implemented")
    }

    override fun clearOngoing() {
        TODO("Not yet implemented")
    }

    override fun finishOperation() {
        TODO("Not yet implemented")
    }

    override fun getBytesOngoing(): Long {
        TODO("Not yet implemented")
    }

    override fun getBytesTotal(): Long {
        TODO("Not yet implemented")
    }

    override fun getCount(): Int {
        TODO("Not yet implemented")
    }

    override fun getOngoing(): TransferItem {
        TODO("Not yet implemented")
    }

    override fun installReceivedContent(descriptor: StreamDescriptor) {
        TODO("Not yet implemented")
    }

    override fun onCancelOperation() {
        TODO("Not yet implemented")
    }

    override fun onUnhandledException(e: Exception) {
        TODO("Not yet implemented")
    }

    override fun publishProgress() {
        TODO("Not yet implemented")
    }

    override fun setBytesOngoing(bytes: Long, bytesIncrease: Long) {
        TODO("Not yet implemented")
    }

    override fun setBytesTotal(bytes: Long) {
        TODO("Not yet implemented")
    }

    override fun setCount(count: Int) {
        TODO("Not yet implemented")
    }

    override fun setOngoing(transferItem: TransferItem) {
        TODO("Not yet implemented")
    }
}