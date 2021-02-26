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
package org.monora.uprotocol.client.android.util

import android.content.Context
import androidx.preference.PreferenceManager
import com.genonbeta.android.updatewithgithub.GitHubUpdater
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.config.AppConfig

/**
 * created by: Veli
 * date: 30.12.2017 17:08
 */
object Updates {
    fun checkForUpdates(
        context: Context, updater: GitHubUpdater, popupDialog: Boolean,
        listener: GitHubUpdater.OnInfoAvailableListener?,
    ) {
        updater.checkForUpdates(popupDialog, object : GitHubUpdater.OnInfoAvailableListener {
            override fun onInfoAvailable(
                newVersion: Boolean,
                versionName: String?,
                title: String?,
                description: String?,
                releaseDate: String?,
            ) {
                PreferenceManager.getDefaultSharedPreferences(context).edit()
                    .putString("availableVersion", versionName)
                    .putLong("checkedForUpdatesTime", System.currentTimeMillis())
                    .apply()

                listener?.onInfoAvailable(newVersion, versionName, title, description, releaseDate)
            }
        })
    }

    fun declareLatestChangelogAsShown(context: Context) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt("changelog_seen_version", BuildConfig.VERSION_CODE)
            .apply()
    }

    private fun getAvailableVersion(context: Context): String? {
        return PreferenceManager.getDefaultSharedPreferences(context).getString("availableVersion", null)
    }

    fun getCheckTime(context: Context): Long {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong("checkedForUpdatesTime", 0)
    }

    fun getDefaultUpdater(context: Context): GitHubUpdater {
        return GitHubUpdater(context, AppConfig.URI_REPO_APP_UPDATE, false)
    }

    fun hasNewVersion(context: Context): Boolean {
        val availableVersion = getAvailableVersion(context)
        return availableVersion != null && GitHubUpdater.isNewVersion(context, availableVersion)
    }

    fun isLatestChangelogShown(context: Context): Boolean {
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        val lastSeenChangelog = preferences.getInt("changelog_seen_version", -1)
        val dialogAllowed = preferences.getBoolean("show_changelog_dialog", true)
        return !preferences.contains("previously_migrated_version")
                || BuildConfig.VERSION_CODE == lastSeenChangelog
                || !dialogAllowed
    }
}