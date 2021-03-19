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

package org.monora.uprotocol.client.android.backend

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.PickClientActivity
import org.monora.uprotocol.client.android.activity.TransferDetailActivity
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.data.TransferRepository
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.receiver.BgBroadcastReceiver
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.WebShareServer
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.util.*
import org.monora.uprotocol.core.TransportSession
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackgroundBackend @Inject constructor(
    @ApplicationContext val context: Context,
    private val transferRepository: TransferRepository,
    private val transportSession: TransportSession,
    private val nsdDaemon: NsdDaemon,
    private val webShareServer: WebShareServer,
) {
    private val bgIntent = Intent(context, BackgroundService::class.java)

    private val bgStopIntent = Intent(context, BackgroundService::class.java).also {
        it.action = BackgroundService.ACTION_STOP_BG_SERVICE
    }

    val bgNotification
        get() = taskNotification?.takeIf { hasTasks() } ?: notificationHelper.foregroundNotification

    private val executor = Executors.newFixedThreadPool(10)

    private var foregroundActivitiesCount = 0

    private var foregroundActivity: Activity? = null

    private var hotspotManager = HotspotManager.newInstance(context)

    var mediaScanner: MediaScannerConnection = MediaScannerConnection(context, null)

    var notificationHelper = NotificationHelper(Notifications(context))

    private val taskList: MutableList<AsyncTask> = ArrayList()

    private var taskNotification: DynamicNotification? = null

    private var taskNotificationTime: Long = 0

    private var wifiLock: WifiManager.WifiLock? = null

    private val wifiManager: WifiManager
        get() = hotspotManager.wifiManager

    fun attach(task: AsyncTask) {
        runInternal(task)
    }

    fun findTaskBy(identity: Identity): AsyncTask? {
        val taskList = findTasksBy(identity)
        return if (taskList.isNotEmpty()) taskList[0] else null
    }

    @Synchronized
    fun findTasksBy(identity: Identity): List<AsyncTask> {
        synchronized(taskList) { return findTasksBy(taskList, identity) }
    }

    fun getHotspotConfig(): WifiConfiguration? {
        return hotspotManager.configuration
    }

    fun <T : AsyncTask> getTaskListOf(clazz: Class<T>): List<T> {
        synchronized(taskList) { return getTaskListOf(taskList, clazz) }
    }

    fun hasTaskOf(clazz: Class<out AsyncTask>): Boolean {
        synchronized(taskList) { return hasTaskOf(taskList, clazz) }
    }

    fun hasTasks(): Boolean {
        return taskList.size > 0
    }

    fun interruptTasksBy(identity: Identity, userAction: Boolean) {
        synchronized(taskList) { for (task in findTasksBy(identity)) task.interrupt(userAction) }
    }

    fun interruptAllTasks() {
        synchronized(taskList) {
            for (task in taskList) {
                task.interrupt(false)
                Log.d(TAG, "interruptAllTasks(): Ongoing task stopped: " + task.getName(context))
            }
        }
    }

    @Synchronized
    fun notifyActivityInForeground(activity: Activity, inForeground: Boolean) {
        if (!inForeground && foregroundActivitiesCount == 0) return
        val wasInForeground = foregroundActivitiesCount > 0
        foregroundActivitiesCount += if (inForeground) 1 else -1
        val inBg = wasInForeground && foregroundActivitiesCount == 0
        val newlyInFg = !wasInForeground && foregroundActivitiesCount == 1

        if (Permissions.checkRunningConditions(context)) {
            takeBgServiceFgIfNeeded(newlyInFg, inBg)
        }

        foregroundActivity = if (inBg) null else if (inForeground) activity else foregroundActivity
        Log.d(TAG, "notifyActivityInForeground: Count: $foregroundActivitiesCount")
    }

    fun notifyFileRequest(device: UClient, transfer: Transfer, itemList: List<UTransferItem>) {
        // Don't show when in the Add Device activity
        if (foregroundActivity is PickClientActivity) return

        val activity = foregroundActivity
        val numberOfFiles = itemList.size
        val acceptIntent: Intent = Intent(context, BackgroundService::class.java)
            .setAction(BgBroadcastReceiver.ACTION_FILE_TRANSFER)
            .putExtra(BgBroadcastReceiver.EXTRA_DEVICE, device)
            .putExtra(BgBroadcastReceiver.EXTRA_TRANSFER, transfer)
            .putExtra(BgBroadcastReceiver.EXTRA_ACCEPTED, true)
        val rejectIntent = (acceptIntent.clone() as Intent)
            .putExtra(BgBroadcastReceiver.EXTRA_ACCEPTED, false)
        val transferDetail: Intent = Intent(context, TransferDetailActivity::class.java)
            .setAction(TransferDetailActivity.ACTION_LIST_TRANSFERS)
            .putExtra(TransferDetailActivity.EXTRA_TRANSFER, transfer)
        val message = if (numberOfFiles > 1) context.resources.getQuantityString(
            R.plurals.ques_receiveMultipleFiles,
            numberOfFiles, numberOfFiles
        ) else itemList[0].name

        if (activity == null) {
            notificationHelper.notifyTransferRequest(
                device, transfer, acceptIntent, rejectIntent, transferDetail, message
            )
        } else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
                .setTitle(context.getString(R.string.text_deviceFileTransferRequest, device.clientNickname))
                .setMessage(message)
                .setCancelable(false)
                .setNeutralButton(R.string.butn_show) { _: DialogInterface?, _: Int ->
                    activity.startActivity(transferDetail)
                }
                .setNegativeButton(R.string.butn_reject) { _: DialogInterface?, _: Int ->
                    ContextCompat.startForegroundService(activity, rejectIntent)
                }
                .setPositiveButton(R.string.butn_accept) { _: DialogInterface?, _: Int ->
                    ContextCompat.startForegroundService(activity, acceptIntent)
                }
            activity.runOnUiThread(Runnable { builder.show() })
        }
    }

    fun publishTaskNotifications(force: Boolean): Boolean {
        val notified = System.nanoTime()
        if (notified <= taskNotificationTime && !force) return false
        if (!hasTasks()) {
            takeBgServiceFgIfNeeded(newlyInFg = false, newlyInBg = false, byOthers = true)
            return false
        }

        var taskList: List<AsyncTask>
        synchronized(this.taskList) { taskList = ArrayList(this.taskList) }
        taskNotificationTime = System.nanoTime() + AppConfig.DELAY_DEFAULT_NOTIFICATION * 1e6.toLong()
        taskNotification = notificationHelper.notifyTasksNotification(taskList, taskNotification)
        return true
    }

    @Synchronized
    private fun <T : AsyncTask> registerWork(task: T) {
        synchronized(taskList) { taskList.add(task) }
        Log.d(TAG, "registerWork: " + task.javaClass.simpleName)
        context.sendBroadcast(Intent(ACTION_TASK_CHANGE))
    }

    fun run(runningTask: AsyncTask) {
        executor.submit { attach(runningTask) }
    }

    private fun runInternal(runningTask: AsyncTask) {
        registerWork(runningTask)
        try {
            runningTask.run(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        unregisterWork(runningTask)
        publishTaskNotifications(true)
    }

    private fun start() {
        val webServerRunning = webShareServer.isAlive
        val commServerRunning = transportSession.isListening

        if (webServerRunning && commServerRunning) {
            Log.d(TAG, "start: Services are already up")
            return
        }

        try {
            if (!Permissions.checkRunningConditions(context)) throw Exception(
                "The app doesn't have the satisfactory permissions to start the services."
            )

            wifiLock = wifiManager.createWifiLock(WIFI_MODE_FULL_HIGH_PERF, TAG).also {
                it.acquire()
            }

            if (!commServerRunning) {
                transportSession.start()
            }

            if (!webServerRunning) {
                // TODO: 2/26/21 Fix bound runner
                /*backend.webShareServer.setAsyncRunner(
                    BoundRunner(Executors.newFixedThreadPool(AppConfig.WEB_SHARE_CONNECTION_MAX))
                )*/
                webShareServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            }

            nsdDaemon.registerService()
            nsdDaemon.startDiscovering()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        try {
            transportSession.stop()
        } catch (ignored: Exception) {
        }

        wifiLock?.takeIf { it.isHeld }?.let {
            it.release()
            wifiLock = null

            Log.d(TAG, "onDestroy: Releasing Wi-Fi lock")
        }

        nsdDaemon.unregisterService()
        nsdDaemon.stopDiscovering()
        webShareServer.stop()

        GlobalScope.launch(Dispatchers.IO) {
            transferRepository.hideTransfersFromWeb()
        }

        if (hotspotManager.unloadPreviousConfig()) {
            Log.d(TAG, "onDestroy: Stopping hotspot (previously started)=" + hotspotManager.disable())
        }

        interruptAllTasks()
    }

    fun toggleHotspot() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O && !Settings.System.canWrite(context)) return

        if (hotspotManager.enabled) {
            hotspotManager.disable()
        } else {
            val result = hotspotManager.enableConfigured(context.getString(R.string.text_appName), null)
            Log.d(TAG, "toggleHotspot: Enabling=$result")
        }
    }

    fun takeBgServiceFgIfNeeded(
        newlyInFg: Boolean,
        newlyInBg: Boolean,
        byOthers: Boolean = false,
        forceStop: Boolean = false,
    ) {
        // Do not try to tweak this!!!
        val hasTasks = hasTasks() && !forceStop
        val hasServices = (hotspotManager.started || webShareServer.hadClients) && !forceStop
        val inFgNow = foregroundActivitiesCount > 0
        val inBgNow = !inFgNow

        if (newlyInFg) {
            start()
        } else if (inBgNow && !hasServices && !hasTasks) {
            stop()
        } else if (newlyInBg && (hasServices || hasTasks)) {
            ContextCompat.startForegroundService(context, bgIntent)
        }

        // Fg checking is for avoiding unnecessary invocation by activities in fg
        if (newlyInFg || (inFgNow && byOthers) || (inBgNow && !hasServices && !hasTasks)) {
            ContextCompat.startForegroundService(context, bgStopIntent)
        }

        if (!hasTasks) {
            if (hasServices && inBgNow) {
                notificationHelper.foregroundNotification.show()
            } else {
                notificationHelper.foregroundNotification.cancel()
            }
        }
    }

    @Synchronized
    private fun unregisterWork(task: AsyncTask) {
        synchronized(taskList) { taskList.remove(task) }
        Log.d(TAG, "unregisterWork: " + task.javaClass.simpleName)
        context.sendBroadcast(Intent(ACTION_TASK_CHANGE))
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private inner class SecondaryHotspotCallback : WifiManager.LocalOnlyHotspotCallback() {
        override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
            super.onStarted(reservation)
            context.sendBroadcast(
                Intent(ACTION_OREO_HOTSPOT_STARTED).putExtra(EXTRA_HOTSPOT_CONFIG, reservation.wifiConfiguration)
            )
        }
    }

    companion object {
        private val TAG = App::class.simpleName

        const val ACTION_OREO_HOTSPOT_STARTED = "org.monora.trebleshot.intent.action.HOTSPOT_STARTED"

        const val ACTION_TASK_CHANGE = "org.monora.uprotocol.client.android.transaction.action.TASK_STATUS_CHANGE"

        const val EXTRA_HOTSPOT_CONFIG = "hotspotConfig"

        fun <T : AsyncTask> findTasksBy(taskList: List<T>, identity: Identity): List<T> {
            val foundList: MutableList<T> = ArrayList()
            for (task in taskList) if (identity == task.identity) foundList.add(task)
            return foundList
        }

        fun <T : AsyncTask> getTaskListOf(taskList: List<AsyncTask>, clazz: Class<T>): List<T> {
            val foundList: MutableList<T> = ArrayList()
            for (task in taskList) if (clazz.isInstance(task)) foundList.add(task as T)
            return foundList
        }

        fun hasTaskOf(taskList: List<AsyncTask>, clazz: Class<out AsyncTask>): Boolean {
            for (task in taskList) if (clazz.isInstance(task)) return true
            return false
        }

        fun hasTaskWith(taskList: List<AsyncTask>, identity: Identity): Boolean {
            for (task in taskList) if (identity == task.identity) return true
            return false
        }
    }

    init {
        mediaScanner.connect()
        if (Build.VERSION.SDK_INT >= 26) hotspotManager.secondaryCallback = SecondaryHotspotCallback()
    }
}