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
package com.genonbeta.TrebleShot.activity

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
import android.os.Parcelable
import android.os.Parcel
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
import android.os.Bundle
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.os.PowerManager
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
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
import android.net.Uri
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.ImageView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import com.genonbeta.TrebleShot.BuildConfig
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.NullPointerException

class HomeActivity : Activity(), NavigationView.OnNavigationItemSelectedListener {
    private var mNavigationView: NavigationView? = null
    private var mDrawerLayout: DrawerLayout? = null
    private var mExitPressTime: Long = 0
    private var mChosenMenuItemId = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        mNavigationView = findViewById<NavigationView>(R.id.nav_view)
        mDrawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, mDrawerLayout, toolbar,
            R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose
        )
        mDrawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        mDrawerLayout.addDrawerListener(object : SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                applyAwaitingDrawerAction()
            }
        })
        mNavigationView.setNavigationItemSelectedListener(this)
        mNavigationView.getMenu().setGroupEnabled(R.id.nav_group_dev_options, BuildConfig.DEBUG)
        if (Updates.hasNewVersion(this)) highlightUpdate()
        if (Flavor.googlePlay == AppUtils.getBuildFlavor()) {
            val donateItem: MenuItem = mNavigationView.getMenu()
                .findItem(R.id.menu_activity_main_donate)
            if (donateItem != null) donateItem.isVisible = true
        }
        findViewById<View>(R.id.sendLayoutButton).setOnClickListener { v: View? ->
            startActivity(
                Intent(this, ContentSharingActivity::class.java)
            )
        }
        findViewById<View>(R.id.receiveLayoutButton).setOnClickListener { v: View? ->
            startActivity(
                Intent(this, AddDeviceActivity::class.java)
                    .putExtra(
                        AddDeviceActivity.Companion.EXTRA_CONNECTION_MODE,
                        ConnectionMode.WaitForRequests
                    )
            )
        }
    }

    override fun onStart() {
        super.onStart()
        createHeaderView()
    }

    override fun onResume() {
        super.onResume()
        checkAndShowCrashReport()
        checkAndShowChangelog()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actions_home, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.actions_home_transfer_history) {
            startActivity(Intent(this, TransferHistoryActivity::class.java))
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        mChosenMenuItemId = item.itemId
        if (mDrawerLayout != null) mDrawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        val pressTime = System.nanoTime()
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START)) mDrawerLayout.closeDrawer(
            GravityCompat.START
        ) else if (pressTime - mExitPressTime < 2e9) super.onBackPressed() else {
            mExitPressTime = pressTime
            Toast.makeText(this, R.string.mesg_secureExit, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onUserProfileUpdated() {
        createHeaderView()
    }

    /**
     * Do not load the chosen item immediately. Wait for the drawer to close.
     */
    private fun applyAwaitingDrawerAction() {
        if (mChosenMenuItemId == 0) // drawer was opened, but nothing was clicked.
            return
        if (R.id.menu_activity_main_manage_devices == mChosenMenuItemId) {
            startActivity(Intent(this, ManageDevicesActivity::class.java))
        } else if (R.id.menu_activity_main_about == mChosenMenuItemId) {
            startActivity(Intent(this, AboutActivity::class.java))
        } else if (R.id.menu_activity_main_send_application == mChosenMenuItemId) {
            ShareAppDialog(this@HomeActivity)
                .show()
        } else if (R.id.menu_activity_main_preferences == mChosenMenuItemId) {
            startActivity(Intent(this, PreferencesActivity::class.java))
        } else if (R.id.menu_activity_main_exit == mChosenMenuItemId) {
            exitApp()
        } else if (R.id.menu_activity_main_donate == mChosenMenuItemId) {
            try {
                startActivity(
                    Intent(
                        this, Class.forName(
                            "com.genonbeta.TrebleShot.activity.DonationActivity"
                        )
                    )
                )
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
        } else if (R.id.menu_activity_main_dev_survey == mChosenMenuItemId) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.text_developmentSurvey)
            builder.setMessage(R.string.text_developmentSurveySummary)
            builder.setNegativeButton(R.string.genfw_uwg_later, null)
            builder.setPositiveButton(R.string.butn_temp_doIt) { dialog: DialogInterface?, which: Int ->
                try {
                    startActivity(
                        Intent(Intent.ACTION_VIEW).setData(
                            Uri.parse(
                                "https://docs.google.com/forms/d/e/1FAIpQLScmwX923MACmHvZTpEyZMDCxRQj" +
                                        "rd8b67u9p9MOjV1qFVp-_A/viewform?usp=sf_link"
                            )
                        )
                    )
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(
                        this@HomeActivity, R.string.mesg_temp_noBrowser,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            builder.show()
        } else if (R.id.menu_activity_feedback == mChosenMenuItemId) {
            AppUtils.startFeedbackActivity(this@HomeActivity)
        } else if (R.id.menu_activity_main_crash_test == mChosenMenuItemId) {
            throw NullPointerException("The crash was intentional, since 'Crash now' was called")
        } else if (R.id.menu_activity_main_db_migration == mChosenMenuItemId) {
            val db = AppUtils.getKuick(this)
            val dbVersion: Int = Kuick.Companion.DATABASE_VERSION
            Toast.makeText(this, "Running migration rules again", Toast.LENGTH_SHORT).show()
            Migration.migrate(db, db.writableDatabase, dbVersion, dbVersion)
        }
        mChosenMenuItemId = 0
    }

    private fun checkAndShowCrashReport() {
        try {
            openFileInput(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG).use { inputStream ->
                val logFile = getFileStreamPath(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG)
                val outputStream = ByteArrayOutputStream()
                var len: Int
                val buffer = ByteArray(8096)
                while (inputStream.read(buffer).also { len = it } != -1) {
                    outputStream.write(buffer, 0, len)
                    outputStream.flush()
                }
                val report = outputStream.toString()
                val streamObject = TextStreamObject()
                streamObject.text = report
                streamObject.comparableDate = logFile.lastModified()
                streamObject.id = AppUtils.getUniqueNumber()
                logFile.delete()
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setTitle(R.string.text_crashReport)
                dialogBuilder.setMessage(R.string.text_crashInfo)
                dialogBuilder.setNegativeButton(R.string.butn_dismiss, null)
                dialogBuilder.setNeutralButton(android.R.string.copy) { dialog: DialogInterface?, which: Int ->
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager?.setPrimaryClip(
                        ClipData.newPlainText(
                            getString(R.string.text_crashReport),
                            outputStream.toString()
                        )
                    )
                    Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
                }
                dialogBuilder.setPositiveButton(R.string.butn_save) { dialog: DialogInterface?, which: Int ->
                    database.insert<Any, TextStreamObject>(streamObject)
                    Toast.makeText(this, R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show()
                }
                dialogBuilder.show()
            }
        } catch (ignored: IOException) {
        }
    }

    private fun checkAndShowChangelog() {
        if (!AppUtils.isLatestChangeLogSeen(this)) {
            AlertDialog.Builder(this)
                .setMessage(R.string.mesg_versionUpdatedChangelog)
                .setPositiveButton(R.string.butn_yes) { dialog: DialogInterface?, which: Int ->
                    AppUtils.publishLatestChangelogSeen(this@HomeActivity)
                    startActivity(Intent(this@HomeActivity, ChangelogActivity::class.java))
                }
                .setNeutralButton(R.string.butn_never) { dialog: DialogInterface?, which: Int ->
                    defaultPreferences.edit()
                        .putBoolean("show_changelog_dialog", false)
                        .apply()
                }
                .setNegativeButton(R.string.butn_no) { dialog: DialogInterface?, which: Int ->
                    AppUtils.publishLatestChangelogSeen(this@HomeActivity)
                    Toast.makeText(
                        this@HomeActivity, R.string.mesg_versionUpdatedChangelogRejected,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }
    }

    private fun createHeaderView() {
        val headerView: View = mNavigationView.getHeaderView(0)
        if (headerView != null) {
            val localDevice = AppUtils.getLocalDevice(applicationContext)
            val imageView = headerView.findViewById<ImageView>(R.id.layout_profile_picture_image_default)
            val editImageView = headerView.findViewById<ImageView>(R.id.layout_profile_picture_image_preferred)
            val deviceNameText: TextView = headerView.findViewById<TextView>(R.id.header_default_device_name_text)
            val versionText: TextView = headerView.findViewById<TextView>(R.id.header_default_device_version_text)
            deviceNameText.setText(localDevice!!.username)
            versionText.setText(localDevice.versionName)
            loadProfilePictureInto(localDevice.username, imageView)
            editImageView.setOnClickListener { v: View? -> startProfileEditor() }
        }
    }

    private fun highlightUpdate() {
        val item: MenuItem = mNavigationView.getMenu().findItem(R.id.menu_activity_main_about)
        item.setTitle(R.string.text_newVersionAvailable)
    }

    companion object {
        const val REQUEST_PERMISSION_ALL = 1
    }
}