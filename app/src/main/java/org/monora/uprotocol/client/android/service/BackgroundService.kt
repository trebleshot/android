/*
 * Copyright (C) 2019 Veli Tasalı
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
package org.monora.uprotocol.client.android.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.coroutineScope
import dagger.hilt.android.AndroidEntryPoint
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.model.*
import org.monora.uprotocol.client.android.task.FileTransferStarterTask
import org.monora.uprotocol.client.android.task.FileTransferTask
import org.monora.uprotocol.client.android.util.*
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.TransportSession
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.transfer.TransferItem
import javax.inject.Inject

@AndroidEntryPoint
class BackgroundService : LifecycleService() {
    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var backend: BackgroundBackend

    private val binder = LocalBinder()

    @Inject
    lateinit var transportSession: TransportSession

    @Inject
    lateinit var persistenceProvider: PersistenceProvider

    @Inject
    lateinit var connectionFactory: ConnectionFactory

    private lateinit var wifiLock: WifiLock

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        wifiLock = backend.wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG)

        backend.nsdDaemon.registerService()
        backend.nsdDaemon.startDiscovering()
        wifiLock.acquire()

        startForeground(NotificationHelper.ID_BG_SERVICE, backend.notificationHelper.foregroundNotification.build())
        tryStartingOrStopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d(TAG, "onStart() : action = " + intent?.action)

        if (!Permissions.checkRunningConditions(this) || intent == null) {
            return START_NOT_STICKY
        }

        if (ACTION_FILE_TRANSFER == intent.action) {
            val device: UClient? = intent.getParcelableExtra(EXTRA_DEVICE)
            val transfer: Transfer? = intent.getParcelableExtra(EXTRA_TRANSFER)
            val notificationId = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, -1)
            val isAccepted = intent.getBooleanExtra(EXTRA_ACCEPTED, false)

            backend.notificationHelper.utils.cancel(notificationId)

            if (device != null && transfer != null) try {
                lifecycle.coroutineScope.launch(Dispatchers.IO) {
                    val task = FileTransferStarterTask.createFrom(
                        connectionFactory,
                        persistenceProvider,
                        appDatabase,
                        transfer,
                        device,
                        TransferItem.Type.Incoming
                    )

                    try {
                        CommunicationBridge.connect(
                            connectionFactory, persistenceProvider, task.addressList, device.clientUid, 0
                        ).use { bridge ->
                            bridge.requestNotifyTransferState(transfer.id, isAccepted)
                        }
                    } catch (ignored: Exception) {
                    }

                    if (isAccepted) {
                        backend.run(task)
                    } else {
                        // TODO: 2/25/21 Remove the transfer and its items altogether
                        //kuick.removeAsynchronous(app, task.transfer, task.device)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (isAccepted) {
                    backend.notificationHelper.showToast(R.string.mesg_somethingWentWrong)
                }
            }
        } else if (ACTION_DEVICE_KEY_CHANGE_APPROVAL == intent.action) {
            val client: UClient? = intent.getParcelableExtra(EXTRA_DEVICE)
            val notificationId = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, -1)

            backend.notificationHelper.utils.cancel(notificationId)

            if (client != null && intent.getBooleanExtra(EXTRA_ACCEPTED, false)) {
                persistenceProvider.approveInvalidationOfCredentials(client)
            }
        } else if (ACTION_CLIPBOARD == intent.action && intent.hasExtra(EXTRA_TEXT_ACCEPTED)) {
            val notificationId = intent.getIntExtra(Notifications.EXTRA_NOTIFICATION_ID, -1)
            val sharedText: SharedText? = intent.getParcelableExtra(EXTRA_TEXT_MODEL)
            val accepted = intent.getBooleanExtra(EXTRA_TEXT_ACCEPTED, false)

            backend.notificationHelper.utils.cancel(notificationId)

            if (accepted && sharedText != null) {
                val cbManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                cbManager.setPrimaryClip(ClipData.newPlainText("receivedText", sharedText.text))
                Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
            }
        } else if (ACTION_END_SESSION == intent.action) {
            stopSelf()
        } else if (ACTION_START_TRANSFER == intent.action && intent.hasExtra(EXTRA_TRANSFER)
            && intent.hasExtra(EXTRA_DEVICE) && intent.hasExtra(EXTRA_TRANSFER_TYPE)
        ) {
            val client: UClient? = intent.getParcelableExtra(EXTRA_DEVICE)
            val transfer: Transfer? = intent.getParcelableExtra(EXTRA_TRANSFER)
            val type = intent.getSerializableExtra(EXTRA_TRANSFER_TYPE) as TransferItem.Type
            if (client != null && transfer != null) try {
                val task = backend.findTaskBy(
                    FileTransferTask.identifyWith(transfer.id, client.uid, type)
                ) as FileTransferTask?

                if (task == null) lifecycle.coroutineScope.launch {
                    backend.run(
                        FileTransferStarterTask.createFrom(
                            connectionFactory, persistenceProvider, appDatabase, transfer, client, type
                        )
                    )
                } else task.operation.ongoing?.let {
                    Toast.makeText(
                        this, getString(R.string.mesg_groupOngoingNotice, it.itemName), Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (ACTION_STOP_ALL_TASKS == intent.action) {
            backend.interruptAllTasks()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        try {
            transportSession.stop()
        } catch (ignored: Exception) {
        }

        backend.nsdDaemon.unregisterService()
        backend.nsdDaemon.stopDiscovering()
        backend.webShareServer.stop()

        lifecycle.coroutineScope.launch {
            appDatabase.transferDao().hideTransfersFromWeb()
        }

        if (backend.hotspotManager.unloadPreviousConfig()) {
            Log.d(TAG, "onDestroy: Stopping hotspot (previously started)=" + backend.hotspotManager.disable())
        }

        backend.interruptAllTasks()

        if (wifiLock.isHeld) {
            wifiLock.release()
            Log.d(TAG, "onDestroy: Releasing Wi-Fi lock")
        }
    }

    private fun isProcessRunning(transferId: Long, deviceId: String, type: TransferItem.Type): Boolean {
        return backend.findTaskBy(FileTransferTask.identifyWith(transferId, deviceId, type)) != null
    }

    /**
     * Some services like file transfer server, web share portal server involve writing and reading data.
     * So, it is best to avoid starting them when the app doesn't have the right permissions.
     */
    fun tryStartingOrStopSelf() {
        val webServerRunning = backend.webShareServer.isAlive
        val commServerRunning = transportSession.isListening
        if (webServerRunning && commServerRunning) return
        try {
            if (!Permissions.checkRunningConditions(this)) throw Exception(
                "The app doesn't have the satisfactory permissions to start the services."
            )

            if (!commServerRunning) {
                transportSession.start()
            }

            if (!webServerRunning) {
                // TODO: 2/26/21 Fix bound runner
                /*backend.webShareServer.setAsyncRunner(
                    BoundRunner(Executors.newFixedThreadPool(AppConfig.WEB_SHARE_CONNECTION_MAX))
                )*/
                backend.webShareServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    inner class LocalBinder : Binder() {
        val service: BackgroundService
            get() = this@BackgroundService
    }

    companion object {
        val TAG = BackgroundService::class.java.simpleName

        const val ACTION_CLIPBOARD = "org.monora.uprotocol.client.android.action.CLIPBOARD"

        const val ACTION_DEVICE_ACQUAINTANCE = "org.monora.uprotocol.client.android.action.DEVICE_ACQUAINTANCE"

        const val ACTION_DEVICE_KEY_CHANGE_APPROVAL = "org.monora.uprotocol.client.android.action.DEVICE_APPROVAL"

        const val ACTION_END_SESSION = "org.monora.uprotocol.client.android.action.END_SESSION"

        const val ACTION_FILE_TRANSFER = "org.monora.uprotocol.client.android.action.FILE_TRANSFER"

        const val ACTION_INCOMING_TRANSFER_READY = "org.monora.uprotocol.client.android.action.INCOMING_TRANSFER_READY"

        const val ACTION_PIN_USED = "org.monora.uprotocol.client.android.transaction.action.PIN_USED"

        const val ACTION_START_TRANSFER = "com.genonbeta.intent.action.START_TRANSFER"

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