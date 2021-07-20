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

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.ChangelogActivity
import org.monora.uprotocol.client.android.activity.HomeActivity
import org.monora.uprotocol.client.android.activity.WelcomeActivity
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.databinding.LayoutProfileEditorBinding
import org.monora.uprotocol.client.android.dialog.PermissionRequests
import org.monora.uprotocol.client.android.util.Permissions
import org.monora.uprotocol.client.android.util.Updater
import org.monora.uprotocol.client.android.viewmodel.UserProfileViewModel
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
abstract class Activity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    @Inject
    lateinit var backend: Backend

    @Inject
    lateinit var sharedTextRepository: SharedTextRepository

    @Inject
    lateinit var updater: Updater

    private var amoledThemeState = false

    private var customFontsState = false

    private var darkThemeState = false

    protected val defaultPreferences: SharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(this)

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
            if (ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED == intent.action) {
                checkForThemeChange()
            }
        }
    }

    var skipPermissionRequest = false
        protected set

    private var themeLoadingFailed = false

    var welcomePageDisallowed = false
        protected set

    override fun onCreate(savedInstanceState: Bundle?) {
        darkThemeState = darkThemeEnabled
        amoledThemeState = amoledThemeEnabled
        customFontsState = customFontsEnabled

        intentFilter.addAction(ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED)

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
        registerReceiver(receiver, intentFilter)
        backend.notifyActivityInForeground(this, true)
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
        backend.notifyActivityInForeground(this, false)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if ("custom_fonts" == key || "theme" == key || "amoled_theme" == key) {
            checkForThemeChange()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (!Permissions.checkRunningConditions(this)) {
            requestRequiredPermissions(!skipPermissionRequest)
        }
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
                        sharedTextRepository.insert(streamObject)
                    }

                    Toast.makeText(this, R.string.mesg_textStreamSaved, Toast.LENGTH_SHORT).show()
                }.show()
        } catch (ignored: IOException) {
        }
    }

    private fun checkAndShowChangelog() {
        if (!updater.isLatestChangelogShown()) {
            AlertDialog.Builder(this)
                .setMessage(R.string.mesg_versionUpdatedChangelog)
                .setPositiveButton(R.string.butn_yes) { _: DialogInterface?, _: Int ->
                    updater.declareLatestChangelogAsShown()
                    startActivity(Intent(this@Activity, ChangelogActivity::class.java))
                }
                .setNeutralButton(R.string.butn_never) { _: DialogInterface?, _: Int ->
                    defaultPreferences.edit()
                        .putBoolean("show_changelog_dialog", false)
                        .apply()
                }
                .setNegativeButton(R.string.butn_no) { _: DialogInterface?, _: Int ->
                    updater.declareLatestChangelogAsShown()
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

    protected fun editProfile(
        userProfileViewModel: UserProfileViewModel,
        pickPhoto: ActivityResultLauncher<String>,
        lifecycleOwner: LifecycleOwner,
    ) {
        val binding = LayoutProfileEditorBinding.inflate(
            LayoutInflater.from(this), null, false
        )

        val dialog = BottomSheetDialog(this)

        binding.viewModel = userProfileViewModel
        binding.lifecycleOwner = lifecycleOwner
        binding.pickPhotoClickListener = View.OnClickListener {
            pickPhoto.launch("image/*")
        }

        binding.executePendingBindings()

        dialog.setContentView(binding.root)
        dialog.show()
    }

    fun hasIntroductionShown(): Boolean {
        return defaultPreferences.getBoolean("introduction_shown", false)
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

    companion object {
        private val TAG = Activity::class.simpleName

        const val ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED"
    }
}