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
package com.genonbeta.TrebleShot.app

import android.content.*
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.ImageView
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Identifier
import com.genonbeta.TrebleShot.dataobject.Identity
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.BackgroundService
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import com.genonbeta.TrebleShot.util.AppUtils
import java.io.FileNotFoundException
import java.util.*

abstract class Activity : AppCompatActivity() {
    private var amoledDarkThemeRequested = false

    private val attachedTaskList: MutableList<BaseAttachableAsyncTask> = ArrayList()

    private var customFontsEnabled = false

    private var darkThemeRequested = false

    private val intentFilter = IntentFilter()

    private var ongoingRequest: AlertDialog? = null

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED == intent.action)
                checkForThemeChange()
            else if (App.ACTION_TASK_CHANGE == intent.action)
                attachTasks()
        }
    }

    lateinit var selfApplication: App
        private set

    var skipPermissionRequest = false
        protected set

    private var themeLoadingFailed = false

    private val uiTaskList: MutableList<AsyncTask> = ArrayList()

    var welcomePageDisallowed = false
        protected set

    override fun onCreate(savedInstanceState: Bundle?) {
        if (application is App)
            selfApplication = application as App
        else
            throw IllegalStateException("This activity cannot run a different Application class.")

        darkThemeRequested = isDarkThemeRequested
        amoledDarkThemeRequested = isAmoledDarkThemeRequested
        customFontsEnabled = isUsingCustomFonts

        intentFilter.addAction(ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED)
        intentFilter.addAction(App.ACTION_TASK_CHANGE)

        if (darkThemeRequested) try {
            @StyleRes var currentThemeRes = packageManager.getActivityInfo(componentName, 0).theme
            Log.d(Activity::class.java.simpleName, "Activity theme id: $currentThemeRes")
            if (currentThemeRes == 0) currentThemeRes = applicationInfo.theme
            Log.d(Activity::class.java.simpleName, "After change theme: $currentThemeRes")
            @StyleRes var appliedRes = 0
            when (currentThemeRes) {
                R.style.Theme_TrebleShot -> appliedRes = R.style.Theme_TrebleShot_Dark
                R.style.Theme_TrebleShot_NoActionBar -> appliedRes = R.style.Theme_TrebleShot_Dark_NoActionBar
                R.style.Theme_TrebleShot_NoActionBar_StaticStatusBar -> appliedRes =
                    R.style.Theme_TrebleShot_Dark_NoActionBar_StaticStatusBar
                else -> Log.e(
                    Activity::class.java.simpleName, "The theme in use for " + javaClass.simpleName
                            + " is unknown. To change the theme to what user requested, it needs to be defined."
                )
            }
            themeLoadingFailed = appliedRes == 0
            if (!themeLoadingFailed) {
                setTheme(appliedRes)
                if (amoledDarkThemeRequested) theme.applyStyle(R.style.BlackPatch, true)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        // Apply the Preferred Font Family as a patch if enabled
        if (customFontsEnabled) {
            Log.d(Activity::class.java.simpleName, "Custom fonts have been applied")
            theme.applyStyle(R.style.Roundies, true)
        }

        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        checkForThemeChange()
        if (!hasIntroductionShown() && !welcomePageDisallowed) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        } else if (!AppUtils.checkRunningConditions(this) && !skipPermissionRequest) {
            requestRequiredPermissions(true)
        }
    }

    override fun onStart() {
        super.onStart()
        attachTasks()
        registerReceiver(receiver, intentFilter)
        selfApplication.notifyActivityInForeground(this, true)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(receiver)
        selfApplication.notifyActivityInForeground(this, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUiTasks()
        detachTasks()
    }

    protected open fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!AppUtils.checkRunningConditions(this))
            requestRequiredPermissions(!skipPermissionRequest)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_PROFILE_PHOTO) if (resultCode == RESULT_OK && data != null) {
            val chosenImageUri = data.data
            if (chosenImageUri != null) {
                GlideApp.with(this)
                    .load(chosenImageUri)
                    .centerCrop()
                    .override(200, 200)
                    .into(object : CustomTarget<Drawable?>() {
                        override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
                            try {
                                val bitmap = Bitmap.createBitmap(
                                    AppConfig.PHOTO_SCALE_FACTOR,
                                    AppConfig.PHOTO_SCALE_FACTOR,
                                    Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(bitmap)
                                val outputStream = openFileOutput("profilePicture", MODE_PRIVATE)

                                resource.setBounds(0, 0, canvas.width, canvas.height)
                                resource.draw(canvas)
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                                outputStream.close()
                                notifyUserProfileChanged()
                            } catch (error: Exception) {
                                error.printStackTrace()
                            }
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    open fun onUserProfileUpdated() {}

    fun attachTask(task: BaseAttachableAsyncTask) {
        synchronized(attachedTaskList) { if (!attachedTaskList.add(task)) return }
        if (task.activityIntent == null)
            task.setContentIntent(this, intent)
    }

    @Synchronized
    private fun attachTasks() {
        val concurrentTaskList = selfApplication.findTasksBy(identity)
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
            if (concurrentTaskList.isEmpty())
                detachTasks()
            else {
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

    fun checkForThemeChange() {
        if ((darkThemeRequested != isDarkThemeRequested || (isDarkThemeRequested
                    && amoledDarkThemeRequested != isAmoledDarkThemeRequested)) && !themeLoadingFailed
            || customFontsEnabled != isUsingCustomFonts
        ) recreate()
    }

    fun checkUiTasks() {
        if (uiTaskList.size <= 0)
            return
        synchronized(uiTaskList) {
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

    /**
     * Exits app closing all the active services and connections.
     */
    fun exitApp() {
        stopService(Intent(this, BackgroundService::class.java))
        finish()
    }

    fun findTasksWith(identity: Identity): List<BaseAttachableAsyncTask> {
        synchronized(attachedTaskList) {
            return App.findTasksBy(
                attachedTaskList,
                identity
            )
        }
    }

    val database: Kuick
        get() = AppUtils.getKuick(this)

    protected val defaultPreferences: SharedPreferences
        get() = AppUtils.getDefaultPreferences(this)

    open val identity: Identity
        get() = withORs(Identifier.from(AsyncTask.Id.HashCode, AsyncTask.hashIntent(intent)))

    fun <T : BaseAttachableAsyncTask?> getTaskListOf(clazz: Class<T>): List<T> {
        synchronized(attachedTaskList) { return App.getTaskListOf(attachedTaskList, clazz) }
    }

    fun hasIntroductionShown(): Boolean {
        return defaultPreferences.getBoolean("introduction_shown", false)
    }

    fun hasTaskOf(clazz: Class<out AsyncTask?>): Boolean {
        synchronized(attachedTaskList) { return App.hasTaskOf(attachedTaskList, clazz) }
    }

    fun hasTaskWith(identity: Identity): Boolean {
        synchronized(attachedTaskList) { return App.hasTaskWith(attachedTaskList, identity) }
    }

    val isPowerSaveMode: Boolean
        get() {
            if (Build.VERSION.SDK_INT < 23) return false
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            return powerManager.isPowerSaveMode
        }

    fun interruptAllTasks(userAction: Boolean) {
        if (attachedTaskList.size <= 0) return
        synchronized(attachedTaskList) { for (task in attachedTaskList) task.interrupt(userAction) }
    }

    val isAmoledDarkThemeRequested: Boolean
        get() = defaultPreferences.getBoolean("amoled_theme", false)

    val isDarkThemeRequested: Boolean
        get() {
            val value = defaultPreferences.getString("theme", "light")
            val systemWideTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return ("dark" == value || "system" == value && systemWideTheme == Configuration.UI_MODE_NIGHT_YES
                    || "battery" == value && isPowerSaveMode)
        }
    val isUsingCustomFonts: Boolean
        get() = defaultPreferences.getBoolean("custom_fonts", false) && Build.VERSION.SDK_INT >= 16

    fun loadProfilePictureInto(deviceName: String, imageView: ImageView) {
        try {
            val inputStream = openFileInput("profilePicture")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            GlideApp.with(this)
                .load(bitmap)
                .circleCrop()
                .into(imageView)
        } catch (e: FileNotFoundException) {
            imageView.setImageDrawable(AppUtils.getDefaultIconBuilder(this).buildRound(deviceName))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun notifyUserProfileChanged() {
        if (!isFinishing) runOnUiThread { onUserProfileUpdated() }
    }

    fun requestProfilePictureChange() {
        startActivityForResult(Intent(Intent.ACTION_PICK).setType("image/*"), REQUEST_PICK_PROFILE_PHOTO)
    }

    fun requestRequiredPermissions(finishIfOtherwise: Boolean) {
        if (ongoingRequest != null && ongoingRequest!!.isShowing) return
        for (request in AppUtils.getRequiredPermissions(this))
            if (RationalePermissionRequest.requestIfNecessary(this, request, finishIfOtherwise).also {
                    ongoingRequest = it
                } != null
            ) break
    }

    fun run(task: AsyncTask) {
        task.setContentIntent(this, intent)
        App.run(this, task)
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

    fun startProfileEditor() {
        ProfileEditorDialog(this).show()
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
        val TAG = Activity::class.java.simpleName
        const val ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED"
        const val REQUEST_PICK_PROFILE_PHOTO = 1000
    }
}