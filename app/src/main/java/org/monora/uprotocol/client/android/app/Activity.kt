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
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.WelcomeActivity
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.dialog.ProfileEditorDialog
import org.monora.uprotocol.client.android.dialog.RationalePermissionRequest
import org.monora.uprotocol.client.android.model.Identifier
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.model.Identity.Companion.withORs
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.BackgroundService.Companion.ACTION_STOP_ALL
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.util.Graphics
import org.monora.uprotocol.client.android.util.Permissions
import java.io.FileNotFoundException
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
abstract class Activity : AppCompatActivity() {
    @Inject
    lateinit var backgroundBackend: BackgroundBackend

    private var amoledDarkThemeRequested = false

    private val attachedTaskList: MutableList<BaseAttachableAsyncTask> = ArrayList()

    private var customFontsEnabled = false

    private var darkThemeRequested = false

    protected val defaultPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

    open val identity: Identity
        get() = withORs(Identifier.from(AsyncTask.Id.HashCode, AsyncTask.hashIntent(intent)))

    private val intentFilter = IntentFilter()

    private val isAmoledDarkThemeRequested: Boolean
        get() = defaultPreferences.getBoolean("amoled_theme", false)

    private val isDarkThemeRequested: Boolean
        get() {
            val value = defaultPreferences.getString("theme", "light")
            val systemWideTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return ("dark" == value || "system" == value && systemWideTheme == Configuration.UI_MODE_NIGHT_YES
                    || "battery" == value && isPowerSaveMode)
        }

    private val isUsingCustomFonts: Boolean
        get() = defaultPreferences.getBoolean("custom_fonts", false) && Build.VERSION.SDK_INT >= 16

    private val isPowerSaveMode: Boolean
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
        darkThemeRequested = isDarkThemeRequested
        amoledDarkThemeRequested = isAmoledDarkThemeRequested
        customFontsEnabled = isUsingCustomFonts

        intentFilter.addAction(ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED)
        intentFilter.addAction(BackgroundBackend.ACTION_TASK_CHANGE)

        if (darkThemeRequested) try {
            @StyleRes var currentThemeRes = packageManager.getActivityInfo(componentName, 0).theme
            @StyleRes var appliedRes = 0

            Log.d(Activity::class.simpleName, "Activity theme id: $currentThemeRes")

            if (currentThemeRes == 0) {
                currentThemeRes = applicationInfo.theme
            }

            Log.d(Activity::class.simpleName, "After change theme: $currentThemeRes")

            when (currentThemeRes) {
                R.style.Theme_TrebleShot -> appliedRes = R.style.Theme_TrebleShot_Dark
                R.style.Theme_TrebleShot_NoActionBar -> appliedRes = R.style.Theme_TrebleShot_Dark_NoActionBar
                R.style.Theme_TrebleShot_NoActionBar_StaticStatusBar -> appliedRes =
                    R.style.Theme_TrebleShot_Dark_NoActionBar_StaticStatusBar
                else -> Log.e(
                    Activity::class.simpleName, "The requested theme ${javaClass.simpleName} is unknown."
                )
            }

            themeLoadingFailed = appliedRes == 0

            if (!themeLoadingFailed) {
                setTheme(appliedRes)

                if (amoledDarkThemeRequested) {
                    theme.applyStyle(R.style.BlackPatch, true)
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        // Apply the Preferred Font Family as a patch if enabled
        if (customFontsEnabled) {
            Log.d(Activity::class.simpleName, "Custom fonts have been applied")
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
        } else if (!Permissions.checkRunningConditions(this) && !skipPermissionRequest) {
            requestRequiredPermissions(true)
        }
    }

    override fun onStart() {
        super.onStart()
        attachTasks()
        registerReceiver(receiver, intentFilter)
        backgroundBackend.notifyActivityInForeground(this, true)
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

    protected open fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!Permissions.checkRunningConditions(this)) {
            requestRequiredPermissions(!skipPermissionRequest)
        }
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

    fun loadProfilePictureInto(deviceName: String, imageView: ImageView) {
        try {
            val inputStream = openFileInput("profilePicture")
            val bitmap = BitmapFactory.decodeStream(inputStream)
            GlideApp.with(this)
                .load(bitmap)
                .circleCrop()
                .into(imageView)
        } catch (e: FileNotFoundException) {
            imageView.setImageDrawable(Graphics.getDefaultIconBuilder(this).buildRound(deviceName))
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
        if (ongoingRequest?.isShowing == true) return
        for (request in Permissions.getRequiredPermissions(this)) {
            if (RationalePermissionRequest.requestIfNecessary(this, request, finishIfOtherwise).also {
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
        private val TAG = Activity::class.simpleName

        const val ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED"

        const val REQUEST_PICK_PROFILE_PHOTO = 1000
    }
}