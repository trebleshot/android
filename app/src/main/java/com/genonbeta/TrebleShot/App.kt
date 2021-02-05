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
package com.genonbeta.TrebleShot

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.os.*
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.configimport.Keyword
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.service.BackgroundService
import com.genonbeta.TrebleShot.service.WebShareServer
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.HotspotManager
import com.genonbeta.TrebleShot.util.NotificationHelper
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.utilimport.DynamicNotification
import com.genonbeta.TrebleShot.utilimport.NsdDaemon
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */
class App : MultiDexApplication(), Thread.UncaughtExceptionHandler {
    private val mExecutor = Executors.newFixedThreadPool(10)
    private val mTaskList: MutableList<AsyncTask> = ArrayList()
    private var mForegroundActivitiesCount = 0
    private lateinit var mDefaultExceptionHandler: Thread.UncaughtExceptionHandler
    private lateinit var mCrashLogFile: File
    private var mNsdDaemon: NsdDaemon? = null
    private var mHotspotManager: HotspotManager? = null
    private var mMediaScanner: MediaScannerConnection? = null
    private var mNotificationHelper: NotificationHelper? = null
    private var mTasksNotification: DynamicNotification? = null
    private var mForegroundActivity: Activity? = null
    private var mWebShareServer: WebShareServer? = null
    private var mTaskNotificationTime: Long = 0
    override fun onCreate() {
        super.onCreate()
        mCrashLogFile = applicationContext.getFileStreamPath(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG)
        Thread.setDefaultUncaughtExceptionHandler(this)
        initializeSettings()
        mNsdDaemon = NsdDaemon(
            applicationContext, AppUtils.getKuick(this),
            AppUtils.getDefaultPreferences(applicationContext)
        )
        mDefaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        mHotspotManager = HotspotManager.Companion.newInstance(this)
        mMediaScanner = MediaScannerConnection(this, null)
        mNotificationHelper = NotificationHelper(
            NotificationUtils(
                getApplicationContext(),
                AppUtils.getKuick(getApplicationContext()), AppUtils.getDefaultPreferences(getApplicationContext())
            )
        )
        mWebShareServer = WebShareServer(this, AppConfig.SERVER_PORT_WEBSHARE)
        mMediaScanner.connect()
        if (Build.VERSION.SDK_INT >= 26) mHotspotManager.setSecondaryCallback(SecondaryHotspotCallback())
        if (Flavor.googlePlay != AppUtils.getBuildFlavor()
            && !Updates.hasNewVersion(getApplicationContext())
            && System.currentTimeMillis() - Updates.getLastTimeCheckedForUpdates(
                getApplicationContext()
            ) >= AppConfig.DELAY_CHECK_FOR_UPDATES
        ) {
            val updater: GitHubUpdater = Updates.getDefaultUpdater(getApplicationContext())
            Updates.checkForUpdates(getApplicationContext(), updater, false, null)
        }
    }

    fun attach(task: AsyncTask) {
        runInternal(task)
    }

    fun canStopService(): Boolean {
        return !hasTasks() && !getHotspotManager().isStarted() && !getWebShareServer().hadClients()
    }

    fun getDefaultPreferences(): SharedPreferences? {
        return AppUtils.getDefaultPreferences(getApplicationContext())
    }

    fun findTaskBy(identity: Identity): AsyncTask? {
        val taskList = findTasksBy(identity)
        return if (taskList.size > 0) taskList[0] else null
    }

    @Synchronized
    fun findTasksBy(identity: Identity): List<AsyncTask> {
        synchronized(mTaskList) { return findTasksBy(mTaskList, identity) }
    }

    fun getHotspotManager(): HotspotManager? {
        return mHotspotManager
    }

    fun getHotspotConfig(): WifiConfiguration {
        return getHotspotManager().getConfiguration()
    }

    fun getMediaScanner(): MediaScannerConnection? {
        return mMediaScanner
    }

    fun getNotificationHelper(): NotificationHelper? {
        return mNotificationHelper
    }

    fun getNsdDaemon(): NsdDaemon? {
        return mNsdDaemon
    }

    private fun getSelfExecutor(): ExecutorService {
        return mExecutor
    }

    protected fun getTaskList(): List<AsyncTask> {
        return mTaskList
    }

    fun <T : AsyncTask?> getTaskListOf(clazz: Class<T>): List<T> {
        synchronized(mTaskList) { return getTaskListOf(mTaskList, clazz) }
    }

    fun getWebShareServer(): WebShareServer? {
        return mWebShareServer
    }

    fun hasTaskOf(clazz: Class<out AsyncTask?>): Boolean {
        synchronized(mTaskList) { return hasTaskOf(mTaskList, clazz) }
    }

    fun hasTasks(): Boolean {
        return mTaskList.size > 0
    }

    private fun initializeSettings() {
        //SharedPreferences defaultPreferences = AppUtils.getDefaultLocalPreferences(this);
        val defaultPreferences = AppUtils.getDefaultPreferences(this)
        val localDevice = AppUtils.getLocalDevice(getApplicationContext())
        val nsdDefined = defaultPreferences!!.contains("nsd_enabled")
        val refVersion = defaultPreferences.contains("referral_version")
        PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false)
        if (!refVersion) defaultPreferences.edit()
            .putInt("referral_version", localDevice!!.versionCode)
            .apply()

        // Some pre-kitkat devices were soft rebooting when this feature was turned on by default.
        // So we will disable it for them and it will still remain as an option for the user.
        if (!nsdDefined) defaultPreferences.edit()
            .putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
            .apply()
        if (defaultPreferences.contains("migrated_version")) {
            val migratedVersion = defaultPreferences.getInt("migrated_version", localDevice!!.versionCode)
            if (migratedVersion < localDevice.versionCode) {
                // migrating to a new version
                if (migratedVersion <= 67) AppUtils.getViewingPreferences(getApplicationContext())!!.edit()
                    .clear()
                    .apply()
                defaultPreferences.edit()
                    .putInt("migrated_version", localDevice.versionCode)
                    .putInt("previously_migrated_version", migratedVersion)
                    .apply()
            }
        } else defaultPreferences.edit()
            .putInt("migrated_version", localDevice!!.versionCode)
            .apply()
    }

    fun interruptTasksBy(identity: Identity, userAction: Boolean) {
        synchronized(mTaskList) { for (task in findTasksBy(identity)) task.interrupt(userAction) }
    }

    fun interruptAllTasks() {
        synchronized(mTaskList) {
            for (task in mTaskList) {
                task.interrupt(false)
                Log.d(TAG, "interruptAllTasks(): Ongoing task stopped: " + task.getName(getApplicationContext()))
            }
        }
    }

    @Synchronized
    fun notifyActivityInForeground(activity: Activity?, inForeground: Boolean) {
        if (!inForeground && mForegroundActivitiesCount == 0) return
        mForegroundActivitiesCount += if (inForeground) 1 else -1
        val inBg = mForegroundActivitiesCount == 0
        val newlyInFg = mForegroundActivitiesCount == 1
        val intent = Intent(this, BackgroundService::class.java)
        if (AppUtils.checkRunningConditions(getApplicationContext())) {
            if (newlyInFg) ContextCompat.startForegroundService(
                getApplicationContext(),
                intent
            ) else if (inBg) tryStoppingBgService()
        }
        mForegroundActivity = if (inBg) null else if (inForeground) activity else mForegroundActivity
        Log.d(TAG, "notifyActivityInForeground: Count: $mForegroundActivitiesCount")
    }

    fun notifyFileRequest(device: Device, transfer: Transfer?, itemList: List<TransferItem>) {
        // Don't show when in the Add Device activity
        if (mForegroundActivity is AddDeviceActivity) return
        val activity = mForegroundActivity
        val numberOfFiles = itemList.size
        val acceptIntent: Intent = Intent(this, BackgroundService::class.java)
            .setAction(BackgroundService.ACTION_FILE_TRANSFER)
            .putExtra(BackgroundService.EXTRA_DEVICE, device)
            .putExtra(BackgroundService.EXTRA_TRANSFER, transfer)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, true)
        val rejectIntent = (acceptIntent.clone() as Intent)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, false)
        val transferDetail: Intent = Intent(this, TransferDetailActivity::class.java)
            .setAction(TransferDetailActivity.Companion.ACTION_LIST_TRANSFERS)
            .putExtra(TransferDetailActivity.Companion.EXTRA_TRANSFER, transfer)
        val message = if (numberOfFiles > 1) getResources().getQuantityString(
            R.plurals.ques_receiveMultipleFiles,
            numberOfFiles, numberOfFiles
        ) else itemList[0].name!!
        if (activity == null) getNotificationHelper().notifyTransferRequest(
            device, transfer, acceptIntent, rejectIntent, transferDetail,
            message
        ) else {
            val builder: AlertDialog.Builder = AlertDialog.Builder(activity)
                .setTitle(getString(R.string.text_deviceFileTransferRequest, device.username))
                .setMessage(message)
                .setCancelable(false)
                .setNeutralButton(
                    R.string.butn_show,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        activity.startActivity(transferDetail)
                    })
                .setNegativeButton(
                    R.string.butn_reject,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        ContextCompat.startForegroundService(
                            activity, rejectIntent
                        )
                    })
                .setPositiveButton(
                    R.string.butn_accept,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                        ContextCompat.startForegroundService(
                            activity, acceptIntent
                        )
                    })
            activity.runOnUiThread(Runnable { builder.show() })
        }
    }

    fun publishTaskNotifications(force: Boolean): Boolean {
        val notified = System.nanoTime()
        if (notified <= mTaskNotificationTime && !force) return false
        if (!hasTasks()) {
            if (mForegroundActivitiesCount > 0 || !tryStoppingBgService()) mNotificationHelper.getForegroundNotification()
                .show()
            return false
        }
        var taskList: List<AsyncTask>
        synchronized(mTaskList) { taskList = ArrayList(mTaskList) }
        mTaskNotificationTime = System.nanoTime() + AppConfig.DELAY_DEFAULT_NOTIFICATION * 1e6.toLong()
        mTasksNotification = mNotificationHelper.notifyTasksNotification(taskList, mTasksNotification)
        return true
    }

    @Synchronized
    protected fun <T : AsyncTask?> registerWork(task: T) {
        synchronized(mTaskList) { mTaskList.add(task) }
        Log.d(TAG, "registerWork: " + task.javaClass.getSimpleName())
        sendBroadcast(Intent(ACTION_TASK_CHANGE))
    }

    fun run(runningTask: AsyncTask) {
        getSelfExecutor().submit { attach(runningTask) }
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
        val killOnExit = getDefaultPreferences()!!.getBoolean("kill_service_on_exit", true)
        if (canStopService() && killOnExit) {
            ContextCompat.startForegroundService(
                getApplicationContext(),
                Intent(this, BackgroundService::class.java)
                    .setAction(BackgroundService.ACTION_END_SESSION)
            )
            return true
        }
        return false
    }

    fun toggleHotspot() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this)) return
        if (getHotspotManager().isEnabled()) getHotspotManager().disable() else Log.d(
            TAG, "toggleHotspot: Enabling=" + getHotspotManager().enableConfigured(
                AppUtils.getHotspotName(
                    this
                ), null
            )
        )
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            if ((!mCrashLogFile!!.exists() || mCrashLogFile!!.delete()) && mCrashLogFile!!.createNewFile()
                && mCrashLogFile!!.canWrite()
            ) {
                val stringBuilder = StringBuilder()
                val stackTraceElements = e.stackTrace
                stringBuilder.append("--TREBLESHOT-CRASH-LOG--\n")
                    .append("\nException: ")
                    .append(e.javaClass.simpleName)
                    .append("\nMessage: ")
                    .append(e.message)
                    .append("\nCause: ")
                    .append(e.cause).append("\nDate: ")
                    .append(
                        DateFormat.getLongDateFormat(this).format(
                            Date(
                                System.currentTimeMillis()
                            )
                        )
                    )
                    .append("\n\n")
                    .append("--STACKTRACE--\n\n")
                if (stackTraceElements.size > 0) for (element in stackTraceElements) {
                    stringBuilder.append(element.className)
                        .append(".")
                        .append(element.methodName)
                        .append(":")
                        .append(element.lineNumber)
                        .append("\n")
                }
                val outputStream = FileOutputStream(mCrashLogFile)
                val inputStream = ByteArrayInputStream(stringBuilder.toString().toByteArray())
                var len: Int
                val buffer = ByteArray(8096)
                while (inputStream.read(buffer).also { len = it } != -1) {
                    outputStream.write(buffer, 0, len)
                    outputStream.flush()
                }
                outputStream.close()
                inputStream.close()
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        }
        mDefaultExceptionHandler!!.uncaughtException(t, e)
    }

    @Synchronized
    protected fun unregisterWork(task: AsyncTask) {
        synchronized(mTaskList) { mTaskList.remove(task) }
        Log.d(TAG, "unregisterWork: " + task.javaClass.simpleName)
        sendBroadcast(Intent(ACTION_TASK_CHANGE))
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private inner class SecondaryHotspotCallback : LocalOnlyHotspotCallback() {
        override fun onStarted(reservation: LocalOnlyHotspotReservation) {
            super.onStarted(reservation)
            sendBroadcast(
                Intent(ACTION_OREO_HOTSPOT_STARTED)
                    .putExtra(EXTRA_HOTSPOT_CONFIG, reservation.getWifiConfiguration())
            )
        }
    }

    companion object {
        val TAG = App::class.java.simpleName
        const val ACTION_OREO_HOTSPOT_STARTED = "org.monora.trebleshot.intent.action.HOTSPOT_STARTED"
        const val ACTION_TASK_CHANGE = "com.genonbeta.TrebleShot.transaction.action.TASK_STATUS_CHANGE"
        const val EXTRA_HOTSPOT_CONFIG = "hotspotConfig"
        fun <T : AsyncTask?> findTasksBy(taskList: List<T>, identity: Identity): List<T> {
            val foundList: MutableList<T> = ArrayList()
            for (task in taskList) if (identity.equals(task.getIdentity())) foundList.add(task)
            return foundList
        }

        @Throws(IllegalStateException::class)
        fun from(activity: android.app.Activity?): App {
            if (activity!!.application is App) return activity.application as App
            throw IllegalStateException("The app does not have an App instance.")
        }

        fun <T : AsyncTask?> getTaskListOf(taskList: List<AsyncTask>, clazz: Class<T>): List<T> {
            val foundList: MutableList<T> = ArrayList()
            for (task in taskList) if (clazz.isInstance(task)) foundList.add(task as T)
            return foundList
        }

        fun hasTaskOf(taskList: List<AsyncTask>, clazz: Class<out AsyncTask?>): Boolean {
            for (task in taskList) if (clazz.isInstance(task)) return true
            return false
        }

        fun hasTaskWith(taskList: List<AsyncTask>, identity: Identity): Boolean {
            for (task in taskList) if (identity.equals(task.getIdentity())) return true
            return false
        }

        fun interruptTasksBy(activity: android.app.Activity?, identity: Identity, userAction: Boolean) {
            try {
                from(activity).interruptTasksBy(identity, userAction)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }

        fun <T : AsyncTask?> run(activity: android.app.Activity?, task: T) {
            try {
                from(activity).run(task)
            } catch (e: IllegalStateException) {
                e.printStackTrace()
            }
        }
    }
}