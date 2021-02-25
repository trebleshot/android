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
package org.monora.uprotocol.client.android

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import org.monora.uprotocol.client.android.activity.AddDeviceActivity
import org.monora.uprotocol.client.android.activity.TransferDetailActivity
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.WebShareServer
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.util.*
import com.genonbeta.android.updatewithgithub.GitHubUpdater
import dagger.hilt.android.HiltAndroidApp
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Executors

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */
@HiltAndroidApp
class App : MultiDexApplication(), Thread.UncaughtExceptionHandler {
    private lateinit var crashFile: File

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    private val executor = Executors.newFixedThreadPool(10)

    private var foregroundActivitiesCount = 0

    lateinit var hotspotManager: HotspotManager

    lateinit var mediaScanner: MediaScannerConnection

    lateinit var notificationHelper: NotificationHelper

    lateinit var nsdDaemon: NsdDaemon

    val taskList: MutableList<AsyncTask> = ArrayList()

    var taskNotification: DynamicNotification? = null

    lateinit var webShareServer: WebShareServer

    private var foregroundActivity: Activity? = null

    private var taskNotificationTime: Long = 0

    val wifiManager: WifiManager
        get() = hotspotManager.wifiManager

    override fun onCreate() {
        super.onCreate()
        crashFile = applicationContext.getFileStreamPath(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG)
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        initializeSettings()

        hotspotManager = HotspotManager.newInstance(this)
        mediaScanner = MediaScannerConnection(this, null)
        notificationHelper = NotificationHelper(Notifications(applicationContext))
        webShareServer = WebShareServer(this, AppConfig.SERVER_PORT_WEBSHARE)

        mediaScanner.connect()

        if (Build.VERSION.SDK_INT >= 26)
            hotspotManager.secondaryCallback = SecondaryHotspotCallback()

        if (Keyword.Flavor.googlePlay != AppUtils.buildFlavor && !Updates.hasNewVersion(applicationContext)
            && System.currentTimeMillis() - Updates.getCheckTime(applicationContext) >= AppConfig.DELAY_UPDATE_CHECK
        ) {
            val updater: GitHubUpdater = Updates.getDefaultUpdater(applicationContext)
            Updates.checkForUpdates(applicationContext, updater, false, null)
        }
    }

    fun attach(task: AsyncTask) {
        runInternal(task)
    }

    fun canStopService(): Boolean {
        return !hasTasks() && !hotspotManager.started && !webShareServer.hadClients()
    }

    fun getDefaultPreferences(): SharedPreferences {
        return AppUtils.getDefaultPreferences(getApplicationContext())
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

    private fun initializeSettings() {
        //SharedPreferences defaultPreferences = AppUtils.getDefaultLocalPreferences(this);
        val defaultPreferences = AppUtils.getDefaultPreferences(this)
        val nsdDefined = defaultPreferences.contains("nsd_enabled")
        val refVersion = defaultPreferences.contains("referral_version")

        PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false)

        if (!refVersion)
            defaultPreferences.edit()
                .putInt("referral_version", BuildConfig.VERSION_CODE)
                .apply()

        // Some pre-kitkat devices were soft rebooting when this feature was turned on by default.
        // So we will disable it for them and it will still remain as an option for the user.
        if (!nsdDefined)
            defaultPreferences.edit()
                .putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
                .apply()

        if (defaultPreferences.contains("migrated_version")) {
            val migratedVersion = defaultPreferences.getInt("migrated_version", BuildConfig.VERSION_CODE)
            if (migratedVersion < BuildConfig.VERSION_CODE) {
                defaultPreferences.edit()
                    .putInt("migrated_version", BuildConfig.VERSION_CODE)
                    .putInt("previously_migrated_version", migratedVersion)
                    .apply()
            }
        } else
            defaultPreferences.edit()
                .putInt("migrated_version", BuildConfig.VERSION_CODE)
                .apply()
    }

    fun interruptTasksBy(identity: Identity, userAction: Boolean) {
        synchronized(taskList) { for (task in findTasksBy(identity)) task.interrupt(userAction) }
    }

    fun interruptAllTasks() {
        synchronized(taskList) {
            for (task in taskList) {
                task.interrupt(false)
                Log.d(TAG, "interruptAllTasks(): Ongoing task stopped: " + task.getName(getApplicationContext()))
            }
        }
    }

    @Synchronized
    fun notifyActivityInForeground(activity: Activity?, inForeground: Boolean) {
        if (!inForeground && foregroundActivitiesCount == 0) return
        foregroundActivitiesCount += if (inForeground) 1 else -1
        val inBg = foregroundActivitiesCount == 0
        val newlyInFg = foregroundActivitiesCount == 1
        val intent = Intent(this, BackgroundService::class.java)
        if (AppUtils.checkRunningConditions(getApplicationContext())) {
            if (newlyInFg)
                ContextCompat.startForegroundService(applicationContext, intent)
            else if (inBg)
                tryStoppingBgService()
        }
        foregroundActivity = if (inBg) null else if (inForeground) activity else foregroundActivity
        Log.d(TAG, "notifyActivityInForeground: Count: $foregroundActivitiesCount")
    }

    fun notifyFileRequest(device: UClient, transfer: Transfer, itemList: List<UTransferItem>) {
        // Don't show when in the Add Device activity
        if (foregroundActivity is AddDeviceActivity) return
        val activity = foregroundActivity
        val numberOfFiles = itemList.size
        val acceptIntent: Intent = Intent(this, BackgroundService::class.java)
            .setAction(BackgroundService.ACTION_FILE_TRANSFER)
            .putExtra(BackgroundService.EXTRA_DEVICE, device)
            .putExtra(BackgroundService.EXTRA_TRANSFER, transfer)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, true)
        val rejectIntent = (acceptIntent.clone() as Intent)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, false)
        val transferDetail: Intent = Intent(this, TransferDetailActivity::class.java)
            .setAction(TransferDetailActivity.ACTION_LIST_TRANSFERS)
            .putExtra(TransferDetailActivity.EXTRA_TRANSFER, transfer)
        val message = if (numberOfFiles > 1) getResources().getQuantityString(
            R.plurals.ques_receiveMultipleFiles,
            numberOfFiles, numberOfFiles
        ) else
            itemList[0].name

        if (activity == null)
            notificationHelper.notifyTransferRequest(
                device, transfer, acceptIntent, rejectIntent, transferDetail, message
            ) else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
                .setTitle(getString(R.string.text_deviceFileTransferRequest, device.clientNickname))
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
            if (foregroundActivitiesCount > 0 || !tryStoppingBgService())
                notificationHelper.foregroundNotification.show()
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
        Log.d(TAG, "registerWork: " + task.javaClass.getSimpleName())
        sendBroadcast(Intent(ACTION_TASK_CHANGE))
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

    private fun tryStoppingBgService(): Boolean {
        val killOnExit = getDefaultPreferences().getBoolean("kill_service_on_exit", true)
        if (canStopService() && killOnExit) {
            ContextCompat.startForegroundService(
                applicationContext,
                Intent(this, BackgroundService::class.java).setAction(BackgroundService.ACTION_END_SESSION)
            )
            return true
        }
        return false
    }

    fun toggleHotspot() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this))
            return

        if (hotspotManager.enabled)
            hotspotManager.disable()
        else
            Log.d(
                TAG, "toggleHotspot: Enabling=" + hotspotManager.enableConfigured(
                    AppUtils.getHotspotName(this),
                    null
                )
            )
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            if ((!crashFile.exists() || crashFile.delete()) && crashFile.createNewFile() && crashFile.canWrite()) {
                return
            }

            PrintWriter(FileOutputStream(crashFile)).use { printWriter ->
                val stackTrace = e.stackTrace

                printWriter.append("--TREBLESHOT-CRASH-LOG--\n")
                    .append("\nException: ${e.javaClass.simpleName}")
                    .append("\nMessage: ${e.message}")
                    .append("\nCause: ${e.cause}")
                    .append("\nDate: ")
                    .append(DateFormat.getLongDateFormat(this).format(Date()))
                    .append("\n\n--STACKTRACE--\n\n")

                if (stackTrace.isNotEmpty()) for (element in stackTrace) {
                    printWriter.append("${element.className}.${element.methodName}:${element.lineNumber}\n")
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            defaultExceptionHandler?.uncaughtException(t, e)
        }
    }

    @Synchronized
    protected fun unregisterWork(task: AsyncTask) {
        synchronized(taskList) { taskList.remove(task) }
        Log.d(TAG, "unregisterWork: " + task.javaClass.simpleName)
        sendBroadcast(Intent(ACTION_TASK_CHANGE))
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private inner class SecondaryHotspotCallback : LocalOnlyHotspotCallback() {
        override fun onStarted(reservation: LocalOnlyHotspotReservation) {
            super.onStarted(reservation)
            sendBroadcast(
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
            for (task in taskList)
                if (identity == task.identity)
                    foundList.add(task)
            return foundList
        }

        @Throws(IllegalStateException::class)
        fun from(activity: android.app.Activity): App {
            if (activity.application is App) return activity.application as App
            throw IllegalStateException("The app does not have an App instance.")
        }

        @Throws(IllegalStateException::class)
        fun from(service: android.app.Service): App {
            if (service.application is App) return service.application as App
            throw IllegalStateException("The app does not have an App instance.")
        }


        fun <T : AsyncTask> getTaskListOf(taskList: List<AsyncTask>, clazz: Class<T>): List<T> {
            val foundList: MutableList<T> = ArrayList()
            for (task in taskList)
                if (clazz.isInstance(task))
                    foundList.add(task as T)
            return foundList
        }

        fun hasTaskOf(taskList: List<AsyncTask>, clazz: Class<out AsyncTask>): Boolean {
            for (task in taskList) if (clazz.isInstance(task)) return true
            return false
        }

        fun hasTaskWith(taskList: List<AsyncTask>, identity: Identity): Boolean {
            for (task in taskList)
                if (identity == task.identity)
                    return true
            return false
        }

        fun interruptTasksBy(activity: android.app.Activity, identity: Identity, userAction: Boolean) {
            try {
                from(activity).interruptTasksBy(identity, userAction)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        fun <T : AsyncTask> run(activity: android.app.Activity, task: T) {
            try {
                from(activity).run(task)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}