package org.monora.uprotocol.client.android.task

import android.content.Context
import android.util.Log
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.TransferTarget
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.exception.ConnectionNotFoundException
import org.monora.uprotocol.client.android.exception.DeviceNotFoundException
import org.monora.uprotocol.client.android.exception.TargetNotFoundException
import org.monora.uprotocol.client.android.exception.TransferNotFoundException
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.util.CommonErrorHelper
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.transfer.TransferItem
import java.net.InetAddress

class FileTransferStarterTask(
    val connectionFactory: ConnectionFactory,
    val persistenceProvider: PersistenceProvider,
    val appDatabase: AppDatabase,
    val transfer: Transfer,
    val client: UClient,
    val target: TransferTarget,
    val type: TransferItem.Type,
    val addressList: List<InetAddress>,
) : AttachableAsyncTask<AttachedTaskListener>() {
    override fun onRun() {
        try {
            CommunicationBridge.connect(
                connectionFactory, persistenceProvider, addressList, client.clientUid, 0
            ).use { bridge ->
                bridge.requestFileTransferStart(transfer.id, type)
                if (bridge.receiveResult()) {
                    app.attach(FileTransferTask(bridge, transfer, client, target, type))
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
            TargetNotFoundException::class
        )

        suspend fun createFrom(
            connectionFactory: ConnectionFactory,
            persistenceProvider: PersistenceProvider,
            appDatabase: AppDatabase,
            transferId: Long,
            clientUid: String,
            type: TransferItem.Type,
        ): FileTransferStarterTask {
            val client = appDatabase.clientDao().get(clientUid) ?: throw DeviceNotFoundException(clientUid)
            val transfer = appDatabase.transferDao().get(transferId) ?: throw TransferNotFoundException(transferId)
            return createFrom(connectionFactory, persistenceProvider, appDatabase, transfer, client, type)
        }

        @Throws(TargetNotFoundException::class, ConnectionNotFoundException::class)
        suspend fun createFrom(
            connectionFactory: ConnectionFactory,
            persistenceProvider: PersistenceProvider,
            appDatabase: AppDatabase,
            transfer: Transfer,
            client: UClient,
            type: TransferItem.Type,
        ): FileTransferStarterTask {
            val target = appDatabase.transferTargetDao().get(
                client.clientUid, transfer.id
            ) ?: throw TargetNotFoundException(client.clientUid, transfer.id)

            Log.d(FileTransferTask.TAG, "createFrom: deviceId=" + client.uid + " transferId=" + transfer.id)

            val inetAddresses = appDatabase.clientAddressDao().getAll(client.clientUid).map { it.clientAddress }

            return FileTransferStarterTask(
                connectionFactory, persistenceProvider, appDatabase, transfer, client, target, type, inetAddresses
            )
        }
    }
}