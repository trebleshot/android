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

import android.os.Build
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.edit
import androidx.multidex.MultiDexApplication
import androidx.preference.PreferenceManager
import dagger.hilt.android.HiltAndroidApp
import org.monora.uprotocol.client.android.data.ExtrasRepository
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.PrintWriter
import java.util.*

/**
 * created by: Veli
 * date: 25.02.2018 01:23
 */
@HiltAndroidApp
class App : MultiDexApplication(), Thread.UncaughtExceptionHandler {
    private lateinit var crashLogFile: File

    private var defaultExceptionHandler: Thread.UncaughtExceptionHandler? = null

    override fun onCreate() {
        super.onCreate()

        crashLogFile = ExtrasRepository.getCrashLogFile(applicationContext)
        defaultExceptionHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        initializeSettings()
    }

    private fun initializeSettings() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        val hasNsdSet = preferences.contains("nsd_enabled")
        val hasReferralVersion = preferences.contains("referral_version")

        PreferenceManager.setDefaultValues(this, R.xml.preferences_defaults_main, false)

        if (!hasReferralVersion) preferences.edit {
            putInt("referral_version", BuildConfig.VERSION_CODE)
        }

        // Some pre-kitkat devices were soft rebooting when this feature was turned on by default.
        // So we will disable it for them, and it will still remain as an option for the user.
        if (!hasNsdSet) preferences.edit {
            putBoolean("nsd_enabled", Build.VERSION.SDK_INT >= 19)
        }

        val migratedVersion = preferences.getInt("migrated_version", MIGRATION_NONE)
        if (migratedVersion < BuildConfig.VERSION_CODE) preferences.edit {
            putInt("migrated_version", BuildConfig.VERSION_CODE)
            if (migratedVersion > MIGRATION_NONE) putInt("previously_migrated_version", migratedVersion)
        }
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        try {
            PrintWriter(FileOutputStream(crashLogFile)).use { printWriter ->
                val stackTrace = e.stackTrace

                printWriter.append("--Uprotocol Client Crash Log ---\n")
                    .append("\nException: ${e.javaClass.simpleName}")
                    .append("\nMessage: ${e.message}")
                    .append("\nCause: ${e.cause}")
                    .append("\nDate: ")
                    .append(DateFormat.getLongDateFormat(this).format(Date()))
                    .append("\n\n--Stacktrace--\n")

                if (stackTrace.isNotEmpty()) for (element in stackTrace) with(element) {
                    printWriter.append("\n$className.$methodName:$lineNumber")
                }
            }
        } catch (ex: IOException) {
            ex.printStackTrace()
        } finally {
            defaultExceptionHandler?.uncaughtException(t, e)
        }
    }

    companion object {
        private const val TAG = "App"

        private const val MIGRATION_NONE = -1
    }
}
