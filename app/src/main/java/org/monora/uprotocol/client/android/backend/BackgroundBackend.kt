package org.monora.uprotocol.client.android.backend

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.AddDeviceActivity
import org.monora.uprotocol.client.android.activity.TransferDetailActivity
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.WebShareServer
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.util.*
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject

class BackgroundBackend @Inject constructor(
    @ApplicationContext val context: Context,
    val nsdDaemon: NsdDaemon,
    val webShareServer: WebShareServer
) {
    private val executor = Executors.newFixedThreadPool(10)

    private var foregroundActivitiesCount = 0

    private var foregroundActivity: Activity? = null

    var hotspotManager = HotspotManager.newInstance(context)

    var mediaScanner: MediaScannerConnection = MediaScannerConnection(context, null)

    var notificationHelper = NotificationHelper(Notifications(context))

    private val taskList: MutableList<AsyncTask> = ArrayList()

    private var taskNotification: DynamicNotification? = null

    private var taskNotificationTime: Long = 0

    private var preferences = PreferenceManager.getDefaultSharedPreferences(context)

    val wifiManager: WifiManager
        get() = hotspotManager.wifiManager

    fun attach(task: AsyncTask) {
        runInternal(task)
    }

    fun canStopService(): Boolean {
        return !hasTasks() && !hotspotManager.started && !webShareServer.hadClients()
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
                Log.d(App.TAG, "interruptAllTasks(): Ongoing task stopped: " + task.getName(context))
            }
        }
    }

    @Synchronized
    fun notifyActivityInForeground(activity: Activity?, inForeground: Boolean) {
        if (!inForeground && foregroundActivitiesCount == 0) return
        foregroundActivitiesCount += if (inForeground) 1 else -1
        val inBg = foregroundActivitiesCount == 0
        val newlyInFg = foregroundActivitiesCount == 1
        val intent = Intent(context, BackgroundService::class.java)
        if (AppUtils.checkRunningConditions(context)) {
            if (newlyInFg) {
                ContextCompat.startForegroundService(context, intent)
            } else if (inBg) {
                tryStoppingBgService()
            }
        }
        foregroundActivity = if (inBg) null else if (inForeground) activity else foregroundActivity
        Log.d(App.TAG, "notifyActivityInForeground: Count: $foregroundActivitiesCount")
    }

    fun notifyFileRequest(device: UClient, transfer: Transfer, itemList: List<UTransferItem>) {
        // Don't show when in the Add Device activity
        if (foregroundActivity is AddDeviceActivity) return
        val activity = foregroundActivity
        val numberOfFiles = itemList.size
        val acceptIntent: Intent = Intent(context, BackgroundService::class.java)
            .setAction(BackgroundService.ACTION_FILE_TRANSFER)
            .putExtra(BackgroundService.EXTRA_DEVICE, device)
            .putExtra(BackgroundService.EXTRA_TRANSFER, transfer)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, true)
        val rejectIntent = (acceptIntent.clone() as Intent)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, false)
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
                .setPositiveButton(R.string.butn_accept) { dialog: DialogInterface?, which: Int ->
                    ContextCompat.startForegroundService(activity, acceptIntent)
                }
            activity.runOnUiThread(Runnable { builder.show() })
        }
    }

    fun publishTaskNotifications(force: Boolean): Boolean {
        val notified = System.nanoTime()
        if (notified <= taskNotificationTime && !force) return false
        if (!hasTasks()) {
            if (foregroundActivitiesCount > 0 || !tryStoppingBgService()) {
                notificationHelper.foregroundNotification.show()
            }
            return false
        }
        var taskList: List<AsyncTask>
        synchronized(this.taskList) { taskList = ArrayList(this.taskList) }
        taskNotificationTime = System.nanoTime() + AppConfig.DELAY_DEFAULT_NOTIFICATION * 1e6.toLong()
        taskNotification = notificationHelper.notifyTasksNotification(taskList, taskNotification)
        return true
    }

    @Synchronized
    protected fun <T : AsyncTask> registerWork(task: T) {
        synchronized(taskList) { taskList.add(task) }
        Log.d(App.TAG, "registerWork: " + task.javaClass.getSimpleName())
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

    fun toggleHotspot() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(context)) return

        if (hotspotManager.enabled) {
            hotspotManager.disable()
        } else {
            val result = hotspotManager.enableConfigured(context.getString(R.string.app_name), null)
            Log.d(App.TAG, "toggleHotspot: Enabling=$result")
        }
    }

    private fun tryStoppingBgService(): Boolean {
        val killOnExit = preferences.getBoolean("kill_service_on_exit", true)
        if (canStopService() && killOnExit) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, BackgroundService::class.java).setAction(
                    BackgroundService.ACTION_END_SESSION
                )
            )
            return true
        }
        return false
    }

    @Synchronized
    protected fun unregisterWork(task: AsyncTask) {
        synchronized(taskList) { taskList.remove(task) }
        Log.d(App.TAG, "unregisterWork: " + task.javaClass.simpleName)
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
        val TAG = App::class.java.simpleName

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