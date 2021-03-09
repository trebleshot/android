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
package org.monora.uprotocol.client.android.app

import android.content.*
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.ChangelogActivity
import org.monora.uprotocol.client.android.activity.HomeActivity
import org.monora.uprotocol.client.android.activity.WelcomeActivity
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.dialog.PermissionRequests
import org.monora.uprotocol.client.android.model.Identifier
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.model.Identity.Companion.withORs
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.util.Permissions
import org.monora.uprotocol.client.android.util.Updates
import java.io.FileReader
import java.io.IOException
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
abstract class Activity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    @Inject
    lateinit var backgroundBackend: BackgroundBackend

    private var amoledThemeState = false

    private val attachedTaskList: MutableList<BaseAttachableAsyncTask> = ArrayList()

    private var customFontsState = false

    private var darkThemeState = false

    protected val defaultPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    open val identity: Identity
        get() = withORs(Identifier.from(AsyncTask.Id.HashCode, AsyncTask.hashIntent(intent)))

    private val intentFilter = IntentFilter()

    private val amoledThemeEnabled: Boolean
        get() = defaultPreferences.getBoolean("amoled_theme", false)

    private val customFontsEnabled: Boolean
        get() = defaultPreferences.getBoolean("custom_fonts", false) && Build.VERSION.SDK_INT >= 16

    private val darkThemeEnabled: Boolean
        get() {
            val value = defaultPreferences.getString("theme", "light")
            val systemWideTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return ("dark" == value || "system" == value && systemWideTheme == Configuration.UI_MODE_NIGHT_YES
                    || "battery" == value && powerSaveEnabled)
        }

    private val powerSaveEnabled: Boolean
        get() {
            if (Build.VERSION.SDK_INT < 23) return false
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            return powerManager.isPowerSaveMode
        }

    private var ongoingRequest: AlertDialog? = null

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED == intent.action)
                checkForThemeChange()
            else if (BackgroundBackend.ACTION_TASK_CHANGE == intent.action)
                attachTasks()
        }
    }

    var skipPermissionRequest = false
        protected set

    private var themeLoadingFailed = false

    private val uiTaskList: MutableList<AsyncTask> = ArrayList()

    var welcomePageDisallowed = false
        protected set

    override fun onCreate(savedInstanceState: Bundle?) {
        darkThemeState = darkThemeEnabled
        amoledThemeState = amoledThemeEnabled
        customFontsState = customFontsEnabled

        intentFilter.addAction(ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED)
        intentFilter.addAction(BackgroundBackend.ACTION_TASK_CHANGE)

        if (darkThemeState) try {
            @StyleRes
            val currentThemeRes = packageManager.getActivityInfo(componentName, 0).theme.let {
                Log.d(Activity::class.simpleName, "ActivityTheme=$it AppTheme=" + applicationInfo.theme)
                return@let if (it == 0) applicationInfo.theme else it
            }

            @StyleRes
            val appliedRes = when (currentThemeRes) {
                R.style.Theme_TrebleShot -> R.style.Theme_TrebleShot_Dark
                R.style.Theme_TrebleShot_NoActionBar -> R.style.Theme_TrebleShot_Dark_NoActionBar
                R.style.Theme_TrebleShot_NoActionBar_StaticStatusBar -> {
                    R.style.Theme_TrebleShot_Dark_NoActionBar_StaticStatusBar
                }
                else -> {
                    Log.e(Activity::class.simpleName, "The requested theme ${javaClass.simpleName} is unknown.")
                    0
                }
            }

            themeLoadingFailed = appliedRes == 0

            if (!themeLoadingFailed) {
                setTheme(appliedRes)

                if (amoledThemeState) {
                    theme.applyStyle(R.style.BlackPatch, true)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        // Apply the Preferred Font Family as a patch if enabled
        if (customFontsState) {
            Log.d(Activity::class.simpleName, "Custom fonts have been applied")
            theme.applyStyle(R.style.Roundies, true)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onStart() {
        super.onStart()
        attachTasks()
        registerReceiver(receiver, intentFilter)
        backgroundBackend.notifyActivityInForeground(this, true)
    }

    override fun onResume() {
        super.onResume()
        checkForThemeChange()
        defaultPreferences.registerOnSharedPreferenceChangeListener(this)

        if (!hasIntroductionShown() && !welcomePageDisallowed) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        } else if (!Permissions.checkRunningConditions(this) && !skipPermissionRequest) {
            requestRequiredPermissions(true)
        }

        if (this is HomeActivity && hasIntroductionShown()) {
            checkAndShowCrashReport()
            checkAndShowChangelog()
        }
    }

    override fun onPause() {
        super.onPause()
        defaultPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
        backgroundBackend.notifyActivityInForeground(this, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUiTasks()
        detachTasks()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if ("custom_fonts" == key || "theme" == key || "amoled_theme" == key) {
            checkForThemeChange()
        }
    }

    protected open fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!Permissions.checkRunningConditions(this)) {
            requestRequiredPermissions(!skipPermissionRequest)
        }
    }

    fun attachTask(task: BaseAttachableAsyncTask) {
        synchronized(attachedTaskList) { if (!attachedTaskList.add(task)) return }
        if (task.activityIntent == null) task.setContentIntent(this, intent)
    }

    @Synchronized
    private fun attachTasks() {
        val concurrentTaskList = backgroundBackend.findTasksBy(identity)
        val attachableBgTaskList: MutableList<BaseAttachableAsyncTask> = ArrayList(attachedTaskList)
        val checkIfExists = attachableBgTaskList.size > 0

        // If this call is in between of onStart and onStop, it means there could be some tasks held in the
        // attached task list. To avoid duplicates, we check them using 'checkIfExists'.
        if (concurrentTaskList.isNotEmpty()) {
            for (task in concurrentTaskList) {
                if (task is BaseAttachableAsyncTask) {
                    if (!checkIfExists || !attachableBgTaskList.contains(task)) {
                        attachTask(task)
                        attachableBgTaskList.add(task)
                    }
                }
            }
        }

        // In this phase, we remove the tasks that are no longer known to the background service.
        if (checkIfExists && attachableBgTaskList.size > 0) {
            if (concurrentTaskList.isEmpty()) {
                detachTasks()
            } else {
                val unrefreshedTaskList: List<BaseAttachableAsyncTask> = ArrayList(attachableBgTaskList)
                for (task in unrefreshedTaskList) {
                    if (!concurrentTaskList.contains(task)) detachTask(task)
                }
            }
        }
        onAttachTasks(attachableBgTaskList)
        for (bgTask in attachableBgTaskList) if (!bgTask.hasAnchor()) throw RuntimeException(
            "The task " + bgTask.javaClass.simpleName + " owner "
                    + javaClass.simpleName + " did not provide the anchor."
        )
    }

    fun attachUiTask(task: AsyncTask) {
        synchronized(uiTaskList) { uiTaskList.add(task) }
    }

    private fun checkAndShowCrashReport() {
        try {
            val log = getFileStreamPath(App.FILENAME_UNHANDLED_CRASH_LOG)
            val report = FileReader(log).use { it.readText() }
            val streamObject = SharedText(0, report, log.lastModified())

            Log.d(TAG, "checkAndShowCrashReport: $report")

            log.delete()

            AlertDialog.Builder(this)
                .setTitle(R.string.text_crashReport)
                .setMessage(R.string.text_crashInfo)
                .setNegativeButton(R.string.butn_dismiss, null)
                .setNeutralButton(android.R.string.copy) { _: DialogInterface?, _: Int ->
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.setPrimaryClip(
                        ClipData.newPlainText(getString(R.string.text_crashReport), report)
                    )
                    Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
                }.setPositiveButton(R.string.butn_save) { _: DialogInterface?, _: Int ->
                    lifecycleScope.launch {
                        backgroundBackend.appDatabase.sharedTextDao().insert(streamObject)
                    }

                    Toast.makeText(this, R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show()
                }.show()
        } catch (ignored: IOException) {
        }
    }

    private fun checkAndShowChangelog() {
        if (!Updates.isLatestChangelogShown(this)) {
            AlertDialog.Builder(this)
                .setMessage(R.string.mesg_versionUpdatedChangelog)
                .setPositiveButton(R.string.butn_yes) { _: DialogInterface?, _: Int ->
                    Updates.declareLatestChangelogAsShown(this@Activity)
                    startActivity(Intent(this@Activity, ChangelogActivity::class.java))
                }
                .setNeutralButton(R.string.butn_never) { _: DialogInterface?, _: Int ->
                    defaultPreferences.edit()
                        .putBoolean("show_changelog_dialog", false)
                        .apply()
                }
                .setNegativeButton(R.string.butn_no) { _: DialogInterface?, _: Int ->
                    Updates.declareLatestChangelogAsShown(this@Activity)
                    Toast.makeText(
                        this@Activity, R.string.mesg_versionUpdatedChangelogRejected,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }
    }

    fun checkForThemeChange() {
        if ((darkThemeState != darkThemeEnabled || (darkThemeEnabled && amoledThemeState != amoledThemeEnabled))
            && !themeLoadingFailed || customFontsState != customFontsEnabled
        ) recreate()
    }

    fun checkUiTasks() {
        if (uiTaskList.size > 0) synchronized(uiTaskList) {
            val uiTaskList: MutableList<AsyncTask> = ArrayList()
            for (task in this.uiTaskList) if (!task.finished) uiTaskList.add(task)
            this.uiTaskList.clear()
            this.uiTaskList.addAll(uiTaskList)
        }
    }

    fun detachTask(task: BaseAttachableAsyncTask) {
        synchronized(attachedTaskList) {
            task.removeAnchor()
            attachedTaskList.remove(task)
        }
    }

    private fun detachTasks() {
        val taskList: List<BaseAttachableAsyncTask> = ArrayList(attachedTaskList)
        for (task in taskList) detachTask(task)
    }

    fun findTasksWith(identity: Identity): List<BaseAttachableAsyncTask> {
        synchronized(attachedTaskList) {
            return BackgroundBackend.findTasksBy(
                attachedTaskList,
                identity
            )
        }
    }

    fun <T : BaseAttachableAsyncTask> getTaskListOf(clazz: Class<T>): List<T> {
        synchronized(attachedTaskList) { return BackgroundBackend.getTaskListOf(attachedTaskList, clazz) }
    }

    fun hasIntroductionShown(): Boolean {
        return defaultPreferences.getBoolean("introduction_shown", false)
    }

    fun hasTaskOf(clazz: Class<out AsyncTask?>): Boolean {
        synchronized(attachedTaskList) { return BackgroundBackend.hasTaskOf(attachedTaskList, clazz) }
    }

    fun hasTaskWith(identity: Identity): Boolean {
        synchronized(attachedTaskList) { return BackgroundBackend.hasTaskWith(attachedTaskList, identity) }
    }

    fun interruptAllTasks(userAction: Boolean) {
        if (attachedTaskList.size <= 0) return
        synchronized(attachedTaskList) { for (task in attachedTaskList) task.interrupt(userAction) }
    }

    fun requestRequiredPermissions(finishIfOtherwise: Boolean) {
        if (ongoingRequest?.isShowing == true) return
        for (request in Permissions.getRequiredPermissions(this)) {
            if (PermissionRequests.requestIfNecessary(this, request, finishIfOtherwise).also {
                    ongoingRequest = it
                } != null
            ) break
        }
    }

    fun run(task: AsyncTask) {
        task.setContentIntent(this, intent)
        backgroundBackend.run(task)
    }

    /**
     * Run a task that will be stopped if the user leaves.
     *
     * @param task to be stopped when user leaves
     */
    fun runUiTask(task: AsyncTask) {
        attachUiTask(task)
        run(task)
    }

    fun <T : AttachedTaskListener, V : AttachableAsyncTask<T>> runUiTask(task: V, anchor: T) {
        task.anchor = anchor
        runUiTask(task)
    }

    protected fun stopUiTasks() {
        if (uiTaskList.size <= 0) return
        synchronized(uiTaskList) {
            for (task in uiTaskList) task.interrupt(true)
            uiTaskList.clear()
        }
    }

    interface OnBackPressedListener {
        fun onBackPressed(): Boolean
    }

    companion object {
        private val TAG = Activity::class.simpleName

        const val ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED"
    }
}