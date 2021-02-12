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
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.genonbeta.TrebleShot.BuildConfig
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.TextStreamObject
import com.genonbeta.TrebleShot.dialog.ShareAppDialog
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Updates
import com.google.android.material.navigation.NavigationView
import java.io.ByteArrayOutputStream
import java.io.IOException

class HomeActivity : Activity(), NavigationView.OnNavigationItemSelectedListener {
    private lateinit var navigationView: NavigationView

    private lateinit var drawerLayout: DrawerLayout

    private var exitPressTime: Long = 0

    private var chosenMenuItemId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationView = findViewById(R.id.nav_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        drawerLayout.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerClosed(drawerView: View) {
                applyAwaitingDrawerAction()
            }
        })
        navigationView.setNavigationItemSelectedListener(this)
        navigationView.menu.setGroupEnabled(R.id.nav_group_dev_options, BuildConfig.DEBUG)
        if (Updates.hasNewVersion(this))
            highlightUpdate()
        if (Keyword.Flavor.googlePlay == AppUtils.buildFlavor) {
            val donateItem: MenuItem = navigationView.menu
                .findItem(R.id.menu_activity_main_donate)
            donateItem.isVisible = true
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
                        AddDeviceActivity.EXTRA_CONNECTION_MODE,
                        AddDeviceActivity.ConnectionMode.WaitForRequests
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
        chosenMenuItemId = item.itemId
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        val pressTime = System.nanoTime()
        if (drawerLayout.isDrawerOpen(GravityCompat.START))
            drawerLayout.closeDrawer(GravityCompat.START)
        else if (pressTime - exitPressTime < 2e9)
            super.onBackPressed()
        else {
            exitPressTime = pressTime
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
        if (chosenMenuItemId == 0) // drawer was opened, but nothing was clicked.
            return

        when {
            R.id.menu_activity_main_manage_devices == chosenMenuItemId -> {
                startActivity(Intent(this, ManageDevicesActivity::class.java))
            }
            R.id.menu_activity_main_about == chosenMenuItemId -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }
            R.id.menu_activity_main_send_application == chosenMenuItemId -> {
                ShareAppDialog(this@HomeActivity)
                    .show()
            }
            R.id.menu_activity_main_preferences == chosenMenuItemId -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
            }
            R.id.menu_activity_main_exit == chosenMenuItemId -> {
                exitApp()
            }
            R.id.menu_activity_main_donate == chosenMenuItemId -> {
                try {
                    startActivity(
                        Intent(this,
                            Class.forName(
                                "com.genonbeta.TrebleShot.activity.DonationActivity"
                            )
                        )
                    )
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
            R.id.menu_activity_main_dev_survey == chosenMenuItemId -> {
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
            }
            R.id.menu_activity_feedback == chosenMenuItemId -> {
                AppUtils.startFeedbackActivity(this@HomeActivity)
            }
            R.id.menu_activity_main_crash_test == chosenMenuItemId -> {
                throw NullPointerException("The crash was intentional, since 'Crash now' was called")
            }
        }
        chosenMenuItemId = 0
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
                streamObject.dateInternal = logFile.lastModified()
                streamObject.id = AppUtils.uniqueNumber.toLong()
                logFile.delete()
                val dialogBuilder = AlertDialog.Builder(this)
                dialogBuilder.setTitle(R.string.text_crashReport)
                dialogBuilder.setMessage(R.string.text_crashInfo)
                dialogBuilder.setNegativeButton(R.string.butn_dismiss, null)
                dialogBuilder.setNeutralButton(android.R.string.copy) { dialog: DialogInterface?, which: Int ->
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
                    clipboardManager.setPrimaryClip(
                        ClipData.newPlainText(
                            getString(R.string.text_crashReport),
                            outputStream.toString()
                        )
                    )
                    Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show()
                }
                dialogBuilder.setPositiveButton(R.string.butn_save) { dialog: DialogInterface?, which: Int ->
                    database.insert(streamObject)
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
        val headerView: View = navigationView.getHeaderView(0)
        val localDevice = AppUtils.getLocalDevice(applicationContext)
        val imageView = headerView.findViewById<ImageView>(R.id.layout_profile_picture_image_default)
        val editImageView = headerView.findViewById<ImageView>(R.id.layout_profile_picture_image_preferred)
        val deviceNameText: TextView = headerView.findViewById(R.id.header_default_device_name_text)
        val versionText: TextView = headerView.findViewById(R.id.header_default_device_version_text)
        deviceNameText.text = localDevice.username
        versionText.text = localDevice.versionName
        loadProfilePictureInto(localDevice.username, imageView)
        editImageView.setOnClickListener { v: View? -> startProfileEditor() }
    }

    private fun highlightUpdate() {
        val item: MenuItem = navigationView.menu.findItem(R.id.menu_activity_main_about)
        item.setTitle(R.string.text_newVersionAvailable)
    }

    companion object {
        const val REQUEST_PERMISSION_ALL = 1
    }
}