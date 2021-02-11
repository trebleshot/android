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
package com.genonbeta.TrebleShot.service

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Intent
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WifiLock
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Service
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.protocol.DeviceVerificationException
import com.genonbeta.TrebleShot.protocol.communication.CommunicationException
import com.genonbeta.TrebleShot.protocol.communication.ContentException
import com.genonbeta.TrebleShot.protocol.communication.NotAllowedException
import com.genonbeta.TrebleShot.service.WebShareServer.BoundRunner
import com.genonbeta.TrebleShot.task.FileTransferTask
import com.genonbeta.TrebleShot.task.IndexTransferTask
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.database.exception.ReconstructionFailedException
import fi.iki.elonen.NanoHTTPD
import org.json.JSONException
import org.json.JSONObject
import org.monora.coolsocket.core.CoolSocket
import org.monora.coolsocket.core.session.ActiveConnection
import java.io.IOException
import java.util.concurrent.Executors

class BackgroundService : Service() {
    private val communicationServer = CommunicationServer()
    private val mBinder = LocalBinder()
    private lateinit var wifiLock: WifiLock

    override fun onBind(intent: Intent): IBinder? {
        return mBinder
    }

    override fun onCreate() {
        super.onCreate()

        wifiLock = app.wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG)

        app.nsdDaemon.registerService()
        app.nsdDaemon.startDiscovering()
        wifiLock.acquire()

        startForeground(NotificationHelper.ID_BG_SERVICE, notificationHelper.foregroundNotification.build())
        tryStartingOrStopSelf()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) Log.d(TAG, "onStart() : action = " + intent.action)
        if (intent != null && AppUtils.checkRunningConditions(this)) {
            if (ACTION_FILE_TRANSFER == intent.action) {
                val device: Device = intent.getParcelableExtra(EXTRA_DEVICE)
                val transfer: Transfer = intent.getParcelableExtra(EXTRA_TRANSFER)
                val notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1)
                val isAccepted = intent.getBooleanExtra(EXTRA_ACCEPTED, false)
                notificationHelper.utils.cancel(notificationId)
                try {
                    val task = FileTransferTask.createFrom(
                        kuick, transfer, device,
                        TransferItem.Type.INCOMING
                    )
                    Thread {
                        try {
                            CommunicationBridge.connect(
                                kuick, task.addressList,
                                task.device, 0
                            ).use { bridge ->
                                bridge.requestNotifyTransferState(transfer.id, isAccepted)
                                bridge.receiveResult()
                            }
                        } catch (ignored: Exception) {
                        }
                    }.start()
                    if (isAccepted) mApp!!.run(task) else kuick.removeAsynchronous(mApp, task.transfer, task.device)
                } catch (e: Exception) {
                    e.printStackTrace()
                    if (isAccepted) notificationHelper.showToast(R.string.mesg_somethingWentWrong)
                }
            } else if (ACTION_DEVICE_KEY_CHANGE_APPROVAL == intent.action) {
                val device: Device = intent.getParcelableExtra(EXTRA_DEVICE)
                val accepted = intent.getBooleanExtra(EXTRA_ACCEPTED, false)
                val notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1)
                val receiveKey = intent.getIntExtra(EXTRA_RECEIVE_KEY, -1)
                val sendKey = intent.getIntExtra(EXTRA_SEND_KEY, -1)
                notificationHelper.utils.cancel(notificationId)
                if (device != null) {
                    device.isBlocked = !accepted
                    if (accepted) {
                        device.receiveKey = receiveKey
                        device.sendKey = sendKey
                    }
                    kuick.update(device)
                    kuick.broadcast()
                }
            } else if (ACTION_CLIPBOARD == intent.action && intent.hasExtra(EXTRA_CLIPBOARD_ACCEPTED)) {
                val notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1)
                val clipboardId = intent.getLongExtra(EXTRA_CLIPBOARD_ID, -1)
                val isAccepted = intent.getBooleanExtra(EXTRA_CLIPBOARD_ACCEPTED, false)
                val textStreamObject = TextStreamObject(clipboardId)
                notificationHelper.utils.cancel(notificationId)
                try {
                    kuick.reconstruct(textStreamObject)
                    if (isAccepted) {
                        val cbManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                        if (cbManager != null) {
                            cbManager.setPrimaryClip(ClipData.newPlainText("receivedText", textStreamObject.text))
                            Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (ACTION_END_SESSION == intent.action) {
                stopSelf()
            } else if (ACTION_START_TRANSFER == intent.action && intent.hasExtra(EXTRA_TRANSFER)
                && intent.hasExtra(EXTRA_DEVICE) && intent.hasExtra(EXTRA_TRANSFER_TYPE)
            ) {
                val device: Device = intent.getParcelableExtra(EXTRA_DEVICE)
                val transfer: Transfer = intent.getParcelableExtra(EXTRA_TRANSFER)
                val type = intent.getSerializableExtra(EXTRA_TRANSFER_TYPE) as TransferItem.Type
                try {
                    val task = app.findTaskBy(
                        FileTransferTask.identifyWith(transfer.id, device.uid, type)
                    ) as FileTransferTask?

                    if (task == null) app.run(
                        FileTransferTask.createFrom(
                            kuick,
                            transfer,
                            device,
                            type
                        )
                    ) else Toast.makeText(
                        this, getString(R.string.mesg_groupOngoingNotice, task.item.name),
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (ACTION_STOP_ALL_TASKS == intent.action) {
                app.interruptAllTasks()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        try {
            communicationServer.stop()
        } catch (ignored: Exception) {
        }

        app.nsdDaemon.unregisterService()
        app.nsdDaemon.stopDiscovering()
        app.webShareServer.stop()

        val values = ContentValues()
        values.put(Kuick.FIELD_TRANSFER_ISSHAREDONWEB, 0)
        kuick.update(
            SQLQuery.Select(Kuick.TABLE_TRANSFER)
                .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFER_ISSHAREDONWEB), 1.toString()), values
        )

        val manager = app.hotspotManager
        if (manager.unloadPreviousConfig()) Log.d(
            TAG,
            "onDestroy: Stopping hotspot (previously started)=" + manager.disable()
        )
        app.interruptAllTasks()

        if (wifiLock.isHeld) {
            wifiLock.release()
            Log.d(TAG, "onDestroy: Releasing Wi-Fi lock")
        }
        AppUtils.generateNetworkPin(this)
        kuick.broadcast()
    }

    private fun isProcessRunning(transferId: Long, deviceId: String, type: TransferItem.Type): Boolean {
        return mApp!!.findTaskBy(FileTransferTask.identifyWith(transferId, deviceId, type)) != null
    }

    /**
     * Some services like file transfer server, web share portal server involve writing and reading data.
     * So, it is best to avoid starting them when the app doesn't have the right permissions.
     */
    fun tryStartingOrStopSelf() {
        val webServerRunning = app.webShareServer.isAlive
        val commServerRunning = communicationServer.isListening
        if (webServerRunning && commServerRunning) return
        try {
            if (!AppUtils.checkRunningConditions(this)) throw Exception(
                "The app doesn't have the satisfactory permissions to start the services."
            )
            if (!commServerRunning) communicationServer.start()
            if (!webServerRunning) {
                app.webShareServer.setAsyncRunner(
                    BoundRunner(Executors.newFixedThreadPool(AppConfig.WEB_SHARE_CONNECTION_MAX))
                )
                app.webShareServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    internal inner class CommunicationServer : CoolSocket(AppConfig.SERVER_PORT_COMMUNICATION) {
        override fun onConnected(activeConnection: ActiveConnection) {
            try {
                activeConnection.reply(AppUtils.getDeviceId(this@BackgroundService))
                var response = activeConnection.receive().asJson
                val activePin = defaultPreferences.getInt(Keyword.NETWORK_PIN, -1)
                val hasPin = (activePin != -1 && response.has(Keyword.DEVICE_PIN)
                        && activePin == response.getInt(Keyword.DEVICE_PIN))
                val device = Device()
                val deviceAddress = DeviceAddress(activeConnection.address)
                var sendInfo = true
                try {
                    DeviceLoader.loadAsServer(kuick, response, device, hasPin)
                } catch (e: DeviceVerificationException) {
                    app.notificationHelper.notifyKeyChanged(device, e.receiveKey, AppUtils.generateKey())
                    throw e
                } catch (e: Exception) {
                    sendInfo = false
                    throw e
                } finally {
                    DeviceLoader.processConnection(kuick, device, deviceAddress)
                    kuick.broadcast()
                    if (sendInfo) CommunicationBridge.sendSecure(
                        activeConnection, true, AppUtils.getLocalDeviceAsJson(
                            this@BackgroundService, device.sendKey, 0
                        )
                    )
                }
                CommunicationBridge.sendResult(activeConnection, true)
                if (hasPin) // pin is known, should be changed. Warn the listeners.
                    sendBroadcast(Intent(ACTION_PIN_USED))
                kuick.broadcast()
                activeConnection.internalCacheLimit = 1073741824 // 1MB
                response = activeConnection.receive().asJson
                handleRequest(activeConnection, device, deviceAddress, hasPin, response)
            } catch (e: Exception) {
                e.printStackTrace()
                try {
                    CommunicationBridge.sendError(activeConnection, e)
                } catch (ignored: Exception) {
                }
            }
        }

        @Throws(
            JSONException::class,
            IOException::class,
            ReconstructionFailedException::class,
            CommunicationException::class
        )
        private fun handleRequest(
            activeConnection: ActiveConnection,
            device: Device,
            deviceAddress: DeviceAddress,
            hasPin: Boolean,
            response: JSONObject,
        ) {
            when (response.getString(Keyword.REQUEST)) {
                Keyword.REQUEST_TRANSFER -> {
                    if (mApp!!.hasTaskOf(IndexTransferTask::class.java)) throw NotAllowedException(device) else {
                        val transferId = response.getLong(Keyword.TRANSFER_ID)
                        val jsonIndex = response.getString(Keyword.INDEX)
                        try {
                            kuick.reconstruct(Transfer(transferId))
                            throw ContentException(ContentException.Error.AlreadyExists)
                        } catch (e: ReconstructionFailedException) {
                            CommunicationBridge.sendResult(activeConnection, true)
                            mApp!!.run(IndexTransferTask(transferId, jsonIndex, device, hasPin))
                        }
                    }
                    return
                }
                Keyword.REQUEST_NOTIFY_TRANSFER_STATE -> {
                    val transferId = response.getInt(Keyword.TRANSFER_ID)
                    val isAccepted = response.getBoolean(Keyword.TRANSFER_IS_ACCEPTED)
                    val transfer = Transfer(transferId)
                    val member = TransferMember(transfer, device, TransferItem.Type.OUTGOING)
                    kuick.reconstruct(transfer)
                    kuick.reconstruct(member)
                    if (!isAccepted) {
                        kuick.remove(member)
                        kuick.broadcast()
                    }
                    CommunicationBridge.sendResult(activeConnection, true)
                    return
                }
                Keyword.REQUEST_CLIPBOARD -> {
                    val textStreamObject = TextStreamObject(
                        AppUtils.uniqueNumber.toLong(),
                        response.getString(Keyword.TRANSFER_TEXT)
                    )
                    kuick.publish(textStreamObject)
                    kuick.broadcast()
                    app.notificationHelper.notifyClipboardRequest(device, textStreamObject)
                    CommunicationBridge.sendResult(activeConnection, true)
                    return
                }
                Keyword.REQUEST_ACQUAINTANCE -> {
                    sendBroadcast(
                        Intent(ACTION_DEVICE_ACQUAINTANCE)
                            .putExtra(EXTRA_DEVICE, device)
                            .putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress)
                    )
                    CommunicationBridge.sendResult(activeConnection, true)
                    return
                }
                Keyword.REQUEST_TRANSFER_JOB -> {
                    val transferId = response.getInt(Keyword.TRANSFER_ID)
                    val typeValue = response.getString(Keyword.TRANSFER_TYPE)
                    var type = TransferItem.Type.valueOf(typeValue)

                    // The type is reversed to match our side
                    if (TransferItem.Type.INCOMING == type) type =
                        TransferItem.Type.OUTGOING else if (TransferItem.Type.OUTGOING == type) type =
                        TransferItem.Type.INCOMING
                    val transfer = Transfer(transferId)
                    kuick.reconstruct(transfer)
                    Log.d(
                        TAG, "CommunicationServer.onConnected(): "
                                + "transferId=" + transferId + " typeValue=" + typeValue
                    )
                    if (TransferItem.Type.INCOMING == type && !device.isTrusted) CommunicationBridge.sendError(
                        activeConnection,
                        Keyword.ERROR_NOT_TRUSTED
                    ) else if (isProcessRunning(transferId.toLong(), device.uid, type)) throw ContentException(
                        ContentException.Error.NotAccessible
                    ) else {
                        val task = FileTransferTask()
                        task.activeConnection = activeConnection
                        task.transfer = transfer
                        task.device = device
                        task.type = type
                        task.member = TransferMember(transfer, device, type)
                        task.index = TransferIndex(transfer)
                        kuick.reconstruct(task.member)
                        CommunicationBridge.sendResult(activeConnection, true)
                        mApp!!.attach(task)
                        return
                    }
                    CommunicationBridge.sendResult(activeConnection, false)
                }
                else -> CommunicationBridge.sendResult(activeConnection, false)
            }
        }

        init {
            configFactory.setReadTimeout(AppConfig.DEFAULT_TIMEOUT_SOCKET)
        }
    }

    inner class LocalBinder : Binder() {
        val service: BackgroundService
            get() = this@BackgroundService
    }

    companion object {
        val TAG = BackgroundService::class.java.simpleName
        const val ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.action.CLIPBOARD"
        const val ACTION_DEVICE_ACQUAINTANCE = "com.genonbeta.TrebleShot.transaction.action.DEVICE_ACQUAINTANCE"
        const val ACTION_DEVICE_KEY_CHANGE_APPROVAL = "com.genonbeta.TrebleShot.action.DEVICE_APPROVAL"
        const val ACTION_END_SESSION = "com.genonbeta.TrebleShot.action.END_SESSION"
        const val ACTION_FILE_TRANSFER = "com.genonbeta.TrebleShot.action.FILE_TRANSFER"
        const val ACTION_INCOMING_TRANSFER_READY = "com.genonbeta.TrebleShot.transaction.action.INCOMING_TRANSFER_READY"
        const val ACTION_PIN_USED = "com.genonbeta.TrebleShot.transaction.action.PIN_USED"
        const val ACTION_START_TRANSFER = "com.genonbeta.intent.action.START_TRANSFER"
        const val ACTION_STOP_ALL_TASKS = "com.genonbeta.TrebleShot.transaction.action.STOP_ALL_TASKS"
        const val EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted"
        const val EXTRA_CLIPBOARD_ID = "extraTextId"
        const val EXTRA_DEVICE_ADDRESS = "extraDeviceAddress"
        const val EXTRA_DEVICE = "extraDevice"
        const val EXTRA_RECEIVE_KEY = "extraReceiveKey"
        const val EXTRA_SEND_KEY = "extraSendKey"
        const val EXTRA_TRANSFER = "extraTransfer"
        const val EXTRA_ACCEPTED = "extraAccepted"
        const val EXTRA_TRANSFER_ITEM_ID = "extraTransferItemId"
        const val EXTRA_TRANSFER_TYPE = "extraTransferType"
    }
}