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
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.data.GitHubDataRepository
import org.monora.uprotocol.client.android.remote.model.Release
import javax.inject.Inject
import javax.inject.Singleton

/**
 * created by: Veli
 * date: 30.12.2017 17:08
 */
@Singleton
class Updater @Inject constructor(
    @ApplicationContext context: Context,
    private val gitHubDataRepository: GitHubDataRepository,
) {
    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    suspend fun checkForUpdates(): Release? {
        return GitHubUpdater.checkForUpdates(gitHubDataRepository)?.also { release ->
            preferences.edit {
                putString("availableVersion", release.tag)
                putLong("checkedForUpdatesTime", System.currentTimeMillis())
            }
        }
    }

    fun declareLatestChangelogAsShown() = preferences.edit {
        putInt("changelog_seen_version", BuildConfig.VERSION_CODE)
    }

    private fun getAvailableVersion(): String? {
        return preferences.getString("availableVersion", null)
    }

    fun hasNewVersion(): Boolean {
        val availableVersion = getAvailableVersion()
        return availableVersion != null && GitHubUpdater.isNewerVersion(availableVersion)
    }

    fun isLatestChangelogShown(): Boolean {
        val lastSeenChangelog = preferences.getInt("changelog_seen_version", -1)
        val dialogAllowed = preferences.getBoolean("show_changelog_dialog", true)
        return !preferences.contains("previously_migrated_version")
                || BuildConfig.VERSION_CODE == lastSeenChangelog
                || !dialogAllowed
    }

    fun needsToCheckForUpdates(): Boolean {
        val checkedOn = preferences.getLong("checkedForUpdatesTime", 0)

        return BuildConfig.FLAVOR != "googlePlay" && !hasNewVersion()
                && System.currentTimeMillis() - checkedOn >= AppConfig.DELAY_UPDATE_CHECK
    }
}