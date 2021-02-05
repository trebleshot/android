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
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import android.content.res.Configuration
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.graphics.Canvas
import android.os.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.request.transition.Transition
import com.genonbeta.TrebleShot.dataobject.Identifier
import com.genonbeta.TrebleShot.dataobject.Identity
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.io.FileNotFoundException
import java.lang.Exception
import java.lang.IllegalStateException
import java.lang.RuntimeException
import java.util.ArrayList

abstract class Activity : AppCompatActivity() {
    private val mAttachedTaskList: MutableList<BaseAttachableAsyncTask> = ArrayList()
    private val mUiTaskList: MutableList<AsyncTask> = ArrayList()
    private var mOngoingRequest: AlertDialog? = null
    var selfApplication: App? = null
        private set
    private val mFilter = IntentFilter()
    private var mDarkThemeRequested = false
    private var mAmoledDarkThemeRequested = false
    private var mThemeLoadingFailed = false
    private var mCustomFontsEnabled = false
    private var mSkipPermissionRequest = false
    private var mWelcomePageDisallowed = false
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED == intent.action) checkForThemeChange() else if (App.Companion.ACTION_TASK_CHANGE == intent.action) attachTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        if (application is App) selfApplication =
            application as App else throw IllegalStateException("This activity cannot run a different Application class.")
        mDarkThemeRequested = isDarkThemeRequested
        mAmoledDarkThemeRequested = isAmoledDarkThemeRequested
        mCustomFontsEnabled = isUsingCustomFonts
        mFilter.addAction(ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED)
        mFilter.addAction(App.Companion.ACTION_TASK_CHANGE)
        if (mDarkThemeRequested) {
            try {
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
                mThemeLoadingFailed = appliedRes == 0
                if (!mThemeLoadingFailed) {
                    setTheme(appliedRes)
                    if (mAmoledDarkThemeRequested) theme.applyStyle(R.style.BlackPatch, true)
                }
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
        }

        // Apply the Preferred Font Family as a patch if enabled
        if (mCustomFontsEnabled) {
            Log.d(Activity::class.java.simpleName, "Custom fonts have been applied")
            theme.applyStyle(R.style.Roundies, true)
        }
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        checkForThemeChange()
        if (!hasIntroductionShown() && !mWelcomePageDisallowed) {
            startActivity(Intent(this, WelcomeActivity::class.java))
            finish()
        } else if (!AppUtils.checkRunningConditions(this)) {
            if (!mSkipPermissionRequest) requestRequiredPermissions(true)
        }
    }

    override fun onStart() {
        super.onStart()
        attachTasks()
        registerReceiver(mReceiver, mFilter)
        selfApplication!!.notifyActivityInForeground(this, true)
    }

    override fun onStop() {
        super.onStop()
        unregisterReceiver(mReceiver)
        selfApplication!!.notifyActivityInForeground(this, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopUiTasks()
        detachTasks()
    }

    protected open fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {}
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!AppUtils.checkRunningConditions(this)) requestRequiredPermissions(!mSkipPermissionRequest)
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
                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable>?
                        ) {
                            try {
                                val bitmap = Bitmap.createBitmap(
                                    AppConfig.PHOTO_SCALE_FACTOR,
                                    AppConfig.PHOTO_SCALE_FACTOR, Bitmap.Config.ARGB_8888
                                )
                                val canvas = Canvas(bitmap)
                                val outputStream = openFileOutput(
                                    "profilePicture",
                                    MODE_PRIVATE
                                )
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
        synchronized(mAttachedTaskList) { if (!mAttachedTaskList.add(task)) return }
        if (task.contentIntent == null) task.setContentIntent(this, intent)
    }

    @Synchronized
    private fun attachTasks() {
        val concurrentTaskList = selfApplication!!.findTasksBy(
            identity
        )
        val attachableBgTaskList: MutableList<BaseAttachableAsyncTask> = ArrayList(mAttachedTaskList)
        val checkIfExists = attachableBgTaskList.size > 0

        // If this call is in between of onStart and onStop, it means there could be some tasks held in the
        // attached task list. To avoid duplicates, we check them using 'checkIfExists'.
        if (concurrentTaskList.size > 0) {
            for (task in concurrentTaskList) {
                if (task is BaseAttachableAsyncTask) {
                    val attachableBgTask = task
                    if (!checkIfExists || !attachableBgTaskList.contains(task)) {
                        attachTask(attachableBgTask)
                        attachableBgTaskList.add(attachableBgTask)
                    }
                }
            }
        }

        // In this phase, we remove the tasks that are no longer known to the background service.
        if (checkIfExists && attachableBgTaskList.size > 0) {
            if (concurrentTaskList.size == 0) detachTasks() else {
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
        synchronized(mUiTaskList) { mUiTaskList.add(task) }
    }

    fun checkForThemeChange() {
        if ((mDarkThemeRequested != isDarkThemeRequested || (isDarkThemeRequested
                    && mAmoledDarkThemeRequested != isAmoledDarkThemeRequested)) && !mThemeLoadingFailed
            || mCustomFontsEnabled != isUsingCustomFonts
        ) recreate()
    }

    fun checkUiTasks() {
        if (mUiTaskList.size <= 0) return
        synchronized(mUiTaskList) {
            val uiTaskList: MutableList<AsyncTask> = ArrayList()
            for (task in mUiTaskList) if (!task.isFinished) uiTaskList.add(task)
            mUiTaskList.clear()
            mUiTaskList.addAll(uiTaskList)
        }
    }

    fun detachTask(task: BaseAttachableAsyncTask) {
        synchronized(mAttachedTaskList) {
            task.removeAnchor()
            mAttachedTaskList.remove(task)
        }
    }

    private fun detachTasks() {
        val taskList: List<BaseAttachableAsyncTask> = ArrayList(mAttachedTaskList)
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
        synchronized(mAttachedTaskList) {
            return App.Companion.findTasksBy<BaseAttachableAsyncTask>(
                mAttachedTaskList,
                identity
            )
        }
    }

    val database: Kuick?
        get() = AppUtils.getKuick(this)
    protected val defaultPreferences: SharedPreferences?
        protected get() = AppUtils.getDefaultPreferences(this)
    open val identity: Identity
        get() = withORs(
            Identifier.from(
                AsyncTask.Id.HashCode, AsyncTask.Companion.hashIntent(
                    intent
                )
            )
        )

    fun <T : BaseAttachableAsyncTask?> getTaskListOf(clazz: Class<T>): List<T> {
        synchronized(mAttachedTaskList) { return App.Companion.getTaskListOf(mAttachedTaskList, clazz) }
    }

    fun hasIntroductionShown(): Boolean {
        return defaultPreferences!!.getBoolean("introduction_shown", false)
    }

    fun hasTaskOf(clazz: Class<out AsyncTask?>): Boolean {
        synchronized(mAttachedTaskList) { return App.Companion.hasTaskOf(mAttachedTaskList, clazz) }
    }

    fun hasTaskWith(identity: Identity): Boolean {
        synchronized(mAttachedTaskList) { return App.Companion.hasTaskWith(mAttachedTaskList, identity) }
    }

    val isPowerSaveMode: Boolean
        get() {
            if (Build.VERSION.SDK_INT < 23) return false
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            return powerManager != null && powerManager.isPowerSaveMode
        }

    fun interruptAllTasks(userAction: Boolean) {
        if (mAttachedTaskList.size <= 0) return
        synchronized(mAttachedTaskList) { for (task in mAttachedTaskList) task.interrupt(userAction) }
    }

    val isAmoledDarkThemeRequested: Boolean
        get() = defaultPreferences!!.getBoolean("amoled_theme", false)
    val isDarkThemeRequested: Boolean
        get() {
            val value = defaultPreferences!!.getString("theme", "light")
            val systemWideTheme = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return ("dark" == value || "system" == value && systemWideTheme == Configuration.UI_MODE_NIGHT_YES
                    || "battery" == value && isPowerSaveMode)
        }
    val isUsingCustomFonts: Boolean
        get() = defaultPreferences!!.getBoolean("custom_fonts", false) && Build.VERSION.SDK_INT >= 16

    fun loadProfilePictureInto(deviceName: String?, imageView: ImageView) {
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
        if (mOngoingRequest != null && mOngoingRequest!!.isShowing) return
        for (request in AppUtils.getRequiredPermissions(this)) if (RationalePermissionRequest.Companion.requestIfNecessary(
                this, request,
                finishIfOtherwise
            ).also { mOngoingRequest = it } != null
        ) break
    }

    fun run(task: AsyncTask) {
        task.setContentIntent(this, intent)
        App.Companion.run<AsyncTask>(this, task)
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

    fun <T : AttachedTaskListener?, V : AttachableAsyncTask<T>?> runUiTask(task: V, anchor: T) {
        task!!.anchor = anchor
        runUiTask(task)
    }

    fun setSkipPermissionRequest(skip: Boolean) {
        mSkipPermissionRequest = skip
    }

    fun setWelcomePageDisallowed(disallow: Boolean) {
        mWelcomePageDisallowed = disallow
    }

    fun startProfileEditor() {
        ProfileEditorDialog(this).show()
    }

    protected fun stopUiTasks() {
        if (mUiTaskList.size <= 0) return
        synchronized(mUiTaskList) {
            for (task in mUiTaskList) task.interrupt(true)
            mUiTaskList.clear()
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