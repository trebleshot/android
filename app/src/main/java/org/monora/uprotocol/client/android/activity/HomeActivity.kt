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
package org.monora.uprotocol.client.android.activity

import android.content.*
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.AddDeviceActivity.Companion.EXTRA_CONNECTION_MODE
import org.monora.uprotocol.client.android.activity.AddDeviceActivity.ConnectionMode.WaitForRequests
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.databinding.LayoutClientStatusBinding
import org.monora.uprotocol.client.android.dialog.ShareAppDialog
import org.monora.uprotocol.client.android.task.DeviceIntroductionTask.*
import org.monora.uprotocol.client.android.task.DeviceIntroductionTask.SuggestNetworkException.*
import org.monora.uprotocol.client.android.util.Activities
import org.monora.uprotocol.client.android.util.Drawables
import org.monora.uprotocol.client.android.util.Updates
import org.monora.uprotocol.client.android.viewmodel.UserProfileViewModel
import org.monora.uprotocol.core.protocol.Client
import java.io.*
import javax.inject.Inject

@AndroidEntryPoint
class HomeActivity : Activity(), NavigationView.OnNavigationItemSelectedListener {
    @Inject
    lateinit var appDatabase: AppDatabase

    @Inject
    lateinit var userDataRepository: UserDataRepository

    private lateinit var navigationView: NavigationView

    private lateinit var drawerLayout: DrawerLayout

    private var pendingMenuItemId = 0

    private val userProfileViewModel: UserProfileViewModel by viewModels()

    private val clientStatusBinding by lazy {
        LayoutClientStatusBinding.bind(navigationView.getHeaderView(0)).also {
            it.viewModel = userProfileViewModel
            it.lifecycleOwner = this
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigationView = findViewById(R.id.nav_view)
        drawerLayout = findViewById(R.id.drawer_layout)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.text_navigationDrawerOpen, R.string.text_navigationDrawerClose
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        drawerLayout.addDrawerListener(
            object : DrawerLayout.SimpleDrawerListener() {
                override fun onDrawerClosed(drawerView: View) {
                    applyAwaitingDrawerAction()
                }
            }
        )

        fun requestProfilePictureChange() {
            //startActivityForResult(Intent(Intent.ACTION_PICK).setType("image/*"), REQUEST_PICK_PROFILE_PHOTO)
        }

        requestProfilePictureChange()

        navigationView.setNavigationItemSelectedListener(this)
        if (Updates.hasNewVersion(this)) {
            highlightUpdate()
        }
        if (BuildConfig.FLAVOR == "googlePlay") {
            navigationView.menu.findItem(R.id.menu_activity_main_donate).isVisible = true
        }
        findViewById<View>(R.id.sendLayoutButton).setOnClickListener {
            startActivity(Intent(it.context, ContentSharingActivity::class.java))
        }
        findViewById<View>(R.id.receiveLayoutButton).setOnClickListener {
            startActivity(
                Intent(it.context, AddDeviceActivity::class.java).putExtra(EXTRA_CONNECTION_MODE, WaitForRequests)
            )
        }

        clientStatusBinding.executePendingBindings()
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
            startActivity(Intent(this, SharedTextActivity::class.java))
        } else {
            return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        pendingMenuItemId = item.itemId
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_PROFILE_PHOTO) if (resultCode == RESULT_OK) data?.data?.let { uri ->
            GlideApp.with(this)
                .load(uri)
                .centerCrop()
                .override(200, 200)
                .into(ProfilePictureTarget(applicationContext, userDataRepository.clientStatic()))
        }
    }

    private fun applyAwaitingDrawerAction() {
        if (pendingMenuItemId == 0) {
            return // drawer was opened, but nothing was clicked.
        }

        when (pendingMenuItemId) {
            R.id.menu_activity_main_manage_devices -> {
                startActivity(Intent(this, ManageDevicesActivity::class.java))
            }
            R.id.menu_activity_main_about -> startActivity(Intent(this, AboutActivity::class.java))
            R.id.menu_activity_main_send_application -> ShareAppDialog(this).show()
            R.id.menu_activity_main_preferences -> {
                startActivity(Intent(this, PreferencesActivity::class.java))
            }
            R.id.menu_activity_main_donate -> try {
                startActivity(
                    Intent(
                        applicationContext,
                        Class.forName("org.monora.uprotocol.client.android.activity.DonationActivity")
                    )
                )
            } catch (e: ClassNotFoundException) {
                e.printStackTrace()
            }
            R.id.menu_activity_feedback -> Activities.startFeedbackActivity(this)
        }

        pendingMenuItemId = 0
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
                        appDatabase.sharedTextDao().insert(streamObject)
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
                    Updates.declareLatestChangelogAsShown(this@HomeActivity)
                    startActivity(Intent(this@HomeActivity, ChangelogActivity::class.java))
                }
                .setNeutralButton(R.string.butn_never) { _: DialogInterface?, _: Int ->
                    defaultPreferences.edit()
                        .putBoolean("show_changelog_dialog", false)
                        .apply()
                }
                .setNegativeButton(R.string.butn_no) { _: DialogInterface?, _: Int ->
                    Updates.declareLatestChangelogAsShown(this@HomeActivity)
                    Toast.makeText(
                        this@HomeActivity, R.string.mesg_versionUpdatedChangelogRejected,
                        Toast.LENGTH_SHORT
                    ).show()
                }
                .show()
        }
    }

    private fun highlightUpdate() {
        navigationView.menu.findItem(R.id.menu_activity_main_about).setTitle(R.string.text_newVersionAvailable)
    }

    companion object {
        private val TAG = HomeActivity::class.simpleName

        const val REQUEST_PERMISSION_ALL = 1

        const val REQUEST_PICK_PROFILE_PHOTO = 2
    }
}

class ProfilePictureTarget(
    private val context: Context,
    private val client: Client,
) : CustomTarget<Drawable>() {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        try {
            val file = context.getFileStreamPath(Drawables.clientPicturePath(client)).also {
                if ((it.exists() && !it.delete()) || !it.createNewFile()) {
                    throw IOException("Could not create a new file")
                }
            }

            val bitmap = Bitmap.createBitmap(
                AppConfig.PHOTO_SCALE_FACTOR,
                AppConfig.PHOTO_SCALE_FACTOR,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)

            FileOutputStream(file).use { outputStream ->
                resource.setBounds(0, 0, canvas.width, canvas.height)
                resource.draw(canvas)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {}
}