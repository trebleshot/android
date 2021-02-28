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
package org.monora.uprotocol.client.android

import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaScannerConnection
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import com.genonbeta.android.updatewithgithub.GitHubUpdater
import dagger.hilt.android.HiltAndroidApp
import org.monora.uprotocol.client.android.activity.AddDeviceActivity
import org.monora.uprotocol.client.android.activity.TransferDetailActivity
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.model.Identity
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.WebShareServer
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.util.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.util.*
import java.util.concurrent.Executors
import javax.inject.Inject

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */
@HiltAndroidApp
class App : MultiDexApplication(), Thread.UncaughtExceptionHandler {
    private lateinit var crashFile: File

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()
        crashFile = applicationContext.getFileStreamPath(Keyword.Local.FILENAME_UNHANDLED_CRASH_LOG)
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        initializeSettings()

        if (BuildConfig.FLAVOR != "googlePlay" && !Updates.hasNewVersion(applicationContext)
            && System.currentTimeMillis() - Updates.getCheckTime(applicationContext) >= AppConfig.DELAY_UPDATE_CHECK
        ) {
            val updater: GitHubUpdater = Updates.getDefaultUpdater(applicationContext)
            Updates.checkForUpdates(applicationContext, updater, false, null)
        }
    }

    private fun initializeSettings() {
        //SharedPreferences defaultPreferences = AppUtils.getDefaultLocalPreferences(this);
        val defaultPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        val nsdDefined = defaultPreferences.contains("nsd_enabled")
        val refVersion = defaultPreferences.contains("referral_version")

        PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false)

        if (!refVersion)
            defaultPreferences.edit()
                .putInt("referral_version", BuildConfig.VERSION_CODE)
                .apply()

        // Some pre-kitkat devices were soft rebooting when this feature was turned on by default.
        // So we will disable it for them and it will still remain as an option for the user.
        if (!nsdDefined)
            defaultPreferences.edit()
                .putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
                .apply()

        if (defaultPreferences.contains("migrated_version")) {
            val migratedVersion = defaultPreferences.getInt("migrated_version", BuildConfig.VERSION_CODE)
            if (migratedVersion < BuildConfig.VERSION_CODE) {
                defaultPreferences.edit()
                    .putInt("migrated_version", BuildConfig.VERSION_CODE)
                    .putInt("previously_migrated_version", migratedVersion)
                    .apply()
            }
        } else
            defaultPreferences.edit()
                .putInt("migrated_version", BuildConfig.VERSION_CODE)
                .apply()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            if (crashFile.canWrite()) {
                Log.d(TAG, "uncaughtException: Check failed")
                return
            }

            PrintWriter(FileOutputStream(crashFile)).use { printWriter ->
                val stackTrace = e.stackTrace

                printWriter.append("--TREBLESHOT-CRASH-LOG--\n")
                    .append("\nException: ${e.javaClass.simpleName}")
                    .append("\nMessage: ${e.message}")
                    .append("\nCause: ${e.cause}")
                    .append("\nDate: ")
                    .append(DateFormat.getLongDateFormat(this).format(Date()))
                    .append("\n\n--STACKTRACE--\n\n")

                if (stackTrace.isNotEmpty()) for (element in stackTrace) {
                    printWriter.append("${element.className}.${element.methodName}:${element.lineNumber}\n")
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            defaultExceptionHandler?.uncaughtException(t, e)
        }
    }

    companion object {
        val TAG = App::class.java.simpleName
    }
}