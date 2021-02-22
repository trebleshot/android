package org.monora.uprotocol.client.android.task

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.Kuick
import org.monora.uprotocol.client.android.exception.ConnectionNotFoundException
import org.monora.uprotocol.client.android.exception.DeviceNotFoundException
import org.monora.uprotocol.client.android.exception.MemberNotFoundException
import org.monora.uprotocol.client.android.exception.TransferNotFoundException
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.util.CommonErrorHelper
import org.monora.uprotocol.client.android.util.CommunicationBridge
import org.monora.uprotocol.client.android.util.CommunicationBridge.Companion.receiveResult
import org.monora.uprotocol.client.android.util.Transfers
import com.genonbeta.android.database.exception.ReconstructionFailedException
import org.monora.uprotocol.client.android.model.*

class FileTransferStarterTask(
    val transfer: Transfer,
    val device: Device,
    val member: TransferMember,
    val index: TransferIndex,
    val type: TransferItem.Type,
    val addressList: List<DeviceAddress>,
) : AttachableAsyncTask<AttachedTaskListener>() {
    override fun onRun() {
        try {
            CommunicationBridge.connect(kuick, addressList, device, 0).use { bridge ->
                bridge.requestFileTransferStart(transfer.id, type)
                if (bridge.receiveResult()) {
                    app.attach(
                        FileTransferTask(bridge.activeConnection, transfer, device, member, index, type)
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            post(CommonErrorHelper.messageOf(context, e))
        }
    }

    override fun getName(context: Context): String {
        return context.getString(R.string.text_transfer)
    }

    companion object {
        @Throws(
            TransferNotFoundException::class,
            DeviceNotFoundException::class,
            ConnectionNotFoundException::class,
            MemberNotFoundException::class
        )
        fun createFrom(
            kuick: Kuick, transferId: Long, deviceId: String, type: TransferItem.Type,
        ): FileTransferStarterTask {
            val db: SQLiteDatabase = kuick.readableDatabase
            val device = Device(deviceId)
            try {
                kuick.reconstruct(db, device)
            } catch (e: ReconstructionFailedException) {
                throw DeviceNotFoundException(device)
            }
            val transfer = Transfer(transferId)
            try {
                kuick.reconstruct(db, transfer)
            } catch (e: ReconstructionFailedException) {
                throw TransferNotFoundException(transfer)
            }
            return createFrom(kuick, transfer, device, type)
        }

        @Throws(MemberNotFoundException::class, ConnectionNotFoundException::class)
        fun createFrom(
            kuick: Kuick, transfer: Transfer, device: Device, type: TransferItem.Type,
        ): FileTransferStarterTask {
            val db: SQLiteDatabase = kuick.readableDatabase
            val member = TransferMember(transfer, device, type)
            try {
                kuick.reconstruct(db, member)
            } catch (e: ReconstructionFailedException) {
                throw MemberNotFoundException(member)
            }
            val addressList: List<DeviceAddress> = Transfers.getAddressListFor(kuick, device.uid)
            Log.d(FileTransferTask.TAG, "createFrom: deviceId=" + device.uid + " transferId=" + transfer.id)

            return FileTransferStarterTask(transfer, device, member, TransferIndex(transfer), type, addressList)
        }
    }
}