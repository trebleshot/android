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
import android.content.pm.PackageManager.NameNotFoundException
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.util.Log
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat.*
import androidx.lifecycle.lifecycleScope
import androidx.preference.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.App
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.HomeActivity
import org.monora.uprotocol.client.android.activity.WelcomeActivity
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.data.SharedTextRepository
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.util.Permissions
import java.io.FileReader
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
abstract class Activity : AppCompatActivity(), OnSharedPreferenceChangeListener {
    @Inject
    lateinit var backend: Backend

    @Inject
    lateinit var sharedTextRepository: SharedTextRepository

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
        } catch (e: NameNotFoundException) {
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

    override fun onDestroy() {
        super.onDestroy()
        ongoingRequest?.takeIf { it.isShowing }?.dismiss()
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String?) {
        if ("custom_fonts" == key || "theme" == key || "amoled_theme" == key) {
            checkForThemeChange()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION && !Permissions.checkRunningConditions(this)) {
            requestRequiredPermissions(
                !skipPermissionRequest,
                permissions.mapIndexed { index, s -> s to (grantResults[index] == PERMISSION_GRANTED) }.toMap()
            )
        }
    }

    fun checkForThemeChange() {
        if ((darkThemeState != darkThemeEnabled || (darkThemeEnabled && amoledThemeState != amoledThemeEnabled))
            && !themeLoadingFailed || customFontsState != customFontsEnabled
        ) recreate()
    }

    fun hasIntroductionShown(): Boolean {
        return defaultPreferences.getBoolean("introduction_shown", false)
    }

    private fun requestRequiredPermissions(finishOtherwise: Boolean, results: Map<String, Boolean>? = null) {
        if (ongoingRequest?.isShowing == true) return
        for (permission in Permissions.getAll()) {
            val id = permission.id
            val showRationale = shouldShowRequestPermissionRationale(this@Activity, id)
            val deniedAfterRequest = results?.get(id) == false
            val granted = checkSelfPermission(this, id) == PERMISSION_GRANTED
            val request = {
                requestPermissions(this@Activity, arrayOf(permission.id), REQUEST_PERMISSION)
            }

            if (granted) continue

            if (deniedAfterRequest && !showRationale) {
                if (finishOtherwise) finish()
            } else if (showRationale) {
                AlertDialog.Builder(this).apply {
                    setCancelable(false)
                    setTitle(permission.title)
                    setMessage(permission.description)
                    setPositiveButton(R.string.grant) { _: DialogInterface?, _: Int -> request() }
                    if (finishOtherwise) {
                        setNegativeButton(R.string.butn_reject) { _: DialogInterface?, _: Int -> finish() }
                    } else {
                        setNegativeButton(R.string.butn_close, null)
                    }
                    ongoingRequest = show()
                }
            } else {
                request()
            }

            break
        }
    }

    companion object {
        private const val TAG = "Activity (monora)"

        private const val ACTION_SYSTEM_POWER_SAVE_MODE_CHANGED = "android.os.action.POWER_SAVE_MODE_CHANGED"

        private const val REQUEST_PERMISSION = 1
    }
}
