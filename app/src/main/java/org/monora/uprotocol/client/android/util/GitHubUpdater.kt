/*
 * Copyright (C) 2020 Veli Tasalı
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

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.maven.artifact.versioning.ComparableVersion
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.data.GitHubDataRepository
import org.monora.uprotocol.client.android.remote.model.Release


/**
 * Created by: veli
 * Date: 11/13/16 8:25 AM
 */

object GitHubUpdater {
    private val TAG = GitHubUpdater::class.simpleName

    suspend fun checkForUpdates(gitHubDataRepository: GitHubDataRepository): Release? {
        Log.d(TAG, "Checking for updates")

        val releases = withContext(Dispatchers.IO) { gitHubDataRepository.getReleases() }
        val installed = ComparableVersion(BuildConfig.VERSION_NAME)

        Log.d(TAG, "Gathered the results with length=" + releases.size)

        releases.forEach { release ->
            val installable = ComparableVersion(release.tag)
            val upgradable = installable > installed

            if (release.prerelase || release.assets.isNullOrEmpty() || !upgradable) {
                return@forEach
            }

            return release
        }

        return null
    }

    fun isNewerVersion(versionName: String): Boolean {
        val installable = ComparableVersion(versionName)
        val installed = ComparableVersion(BuildConfig.VERSION_NAME)

        return installable > installed
    }
}