package com.genonbeta.TrebleShot.task

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException
import com.genonbeta.TrebleShot.exception.MemberNotFoundException
import com.genonbeta.TrebleShot.exception.TransferNotFoundException
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.util.CommonErrorHelper
import com.genonbeta.TrebleShot.util.CommunicationBridge
import com.genonbeta.TrebleShot.util.CommunicationBridge.Companion.receiveResult
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.android.database.exception.ReconstructionFailedException

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