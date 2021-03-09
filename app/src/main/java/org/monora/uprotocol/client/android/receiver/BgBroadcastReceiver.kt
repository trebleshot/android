package org.monora.uprotocol.client.android.receiver

import android.content.*
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.task.FileTransferStarterTask
import org.monora.uprotocol.client.android.task.FileTransferTask
import org.monora.uprotocol.client.android.util.Notifications
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject

@AndroidEntryPoint
class BgBroadcastReceiver : BroadcastReceiver() {
    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var backend: BackgroundBackend

    @Inject
    lateinit var persistenceProvider: PersistenceProvider

    @Inject
    lateinit var connectionFactory: ConnectionFactory

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_FILE_TRANSFER -> {
                val device: UClient? = intent.getParcelableExtra(EXTRA_DEVICE)
                val transfer: Transfer? = intent.getParcelableExtra(EXTRA_TRANSFER)
                val notificationId = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, -1)
                val isAccepted = intent.getBooleanExtra(EXTRA_ACCEPTED, false)

                backend.notificationHelper.utils.cancel(notificationId)

                if (device != null && transfer != null) try {
                    GlobalScope.launch(Dispatchers.IO) {
                        val task = FileTransferStarterTask.createFrom(
                            connectionFactory,
                            persistenceProvider,
                            appDatabase,
                            transfer,
                            device,
                            TransferItem.Type.Incoming
                        )

                        try {
                            CommunicationBridge.Builder(
                                connectionFactory, persistenceProvider, task.addressList
                            ).apply {
                                setClientUid(device.clientUid)
                            }.connect().use { bridge ->
                                bridge.requestNotifyTransferState(transfer.id, isAccepted)
                            }
                        } catch (ignored: Exception) {
                        }

                        if (isAccepted) {
                            backend.run(task)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isAccepted) {
                        backend.notificationHelper.showToast(R.string.mesg_somethingWentWrong)
                    }
                }
            }
            ACTION_DEVICE_KEY_CHANGE_APPROVAL -> {
                val client: UClient? = intent.getParcelableExtra(EXTRA_DEVICE)
                val notificationId = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, -1)

                backend.notificationHelper.utils.cancel(notificationId)

                if (client != null && intent.getBooleanExtra(EXTRA_ACCEPTED, false)) {
                    persistenceProvider.approveInvalidationOfCredentials(client)
                }
            }
            ACTION_CLIPBOARD -> {
                val notificationId = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, -1)
                val sharedText: SharedText? = intent.getParcelableExtra(EXTRA_TEXT_MODEL)
                val accepted = intent.getBooleanExtra(EXTRA_TEXT_ACCEPTED, false)

                backend.notificationHelper.utils.cancel(notificationId)

                if (accepted && sharedText != null) {
                    val cbManager = context.applicationContext.getSystemService(
                        LifecycleService.CLIPBOARD_SERVICE
                    ) as ClipboardManager
                    cbManager.setPrimaryClip(ClipData.newPlainText("receivedText", sharedText.text))
                    Toast.makeText(context, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
                }
            }
            ACTION_START_TRANSFER -> {
                val client: UClient? = intent.getParcelableExtra(EXTRA_DEVICE)
                val transfer: Transfer? = intent.getParcelableExtra(EXTRA_TRANSFER)
                val type = intent.getSerializableExtra(EXTRA_TRANSFER_TYPE) as TransferItem.Type?

                if (client != null && transfer != null && type != null) try {
                    val task = backend.findTaskBy(
                        FileTransferTask.identifyWith(transfer.id, client.uid, type)
                    ) as FileTransferTask?

                    if (task == null) GlobalScope.launch {
                        backend.run(
                            FileTransferStarterTask.createFrom(
                                connectionFactory, persistenceProvider, appDatabase, transfer, client, type
                            )
                        )
                    } else task.operation.ongoing?.let {
                        Toast.makeText(
                            context,
                            context.getString(R.string.mesg_groupOngoingNotice, it.itemName),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            ACTION_STOP_ALL_TASKS -> backend.interruptAllTasks()
        }
    }

    companion object {
        const val ACTION_CLIPBOARD = "org.monora.uprotocol.client.android.action.CLIPBOARD"

        const val ACTION_DEVICE_ACQUAINTANCE = "org.monora.uprotocol.client.android.action.DEVICE_ACQUAINTANCE"

        const val ACTION_DEVICE_KEY_CHANGE_APPROVAL = "org.monora.uprotocol.client.android.action.DEVICE_APPROVAL"

        const val ACTION_FILE_TRANSFER = "org.monora.uprotocol.client.android.action.FILE_TRANSFER"

        const val ACTION_INCOMING_TRANSFER_READY = "org.monora.uprotocol.client.android.action.INCOMING_TRANSFER_READY"

        const val ACTION_PIN_USED = "org.monora.uprotocol.client.android.transaction.action.PIN_USED"

        const val ACTION_START_TRANSFER = "org.monora.uprotocol.client.android.transaction.action.START_TRANSFER"

        const val ACTION_STOP_ALL_TASKS = "org.monora.uprotocol.client.android.transaction.action.STOP_ALL_TASKS"

        const val EXTRA_TEXT_ACCEPTED = "extraTextAccepted"

        const val EXTRA_TEXT_MODEL = "extraText"

        const val EXTRA_DEVICE_ADDRESS = "extraDeviceAddress"

        const val EXTRA_DEVICE = "extraDevice"

        const val EXTRA_TRANSFER = "extraTransfer"

        const val EXTRA_ACCEPTED = "extraAccepted"

        const val EXTRA_TRANSFER_TYPE = "extraTransferType"
    }
}