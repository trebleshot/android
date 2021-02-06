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
package com.genonbeta.TrebleShot.util

import android.content.Context
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.android.updatewithgithub.GitHubUpdater

/**
 * created by: Veli
 * date: 30.12.2017 17:08
 */
object Updates {
    // TODO: 2/6/21 Check for updates method  is commented out
    fun checkForUpdates(
        context: Context?, updater: GitHubUpdater?, popupDialog: Boolean,
        listener: GitHubUpdater.OnInfoAvailableListener?
    ) {
        /*
        updater.checkForUpdates(popupDialog, (newVersion, versionName, title, description, releaseDate) -> {
            SharedPreferences sharedPreferences = AppUtils.getDefaultPreferences(context);

            sharedPreferences.edit()
                    .putString("availableVersion", versionName)
                    .putLong("checkedForUpdatesTime", System.currentTimeMillis())
                    .apply();

            if (listener != null)
                listener.onInfoAvailable(newVersion, versionName, title, description, releaseDate);
        });*/
    }

    private fun getAvailableVersion(context: Context): String? {
        return AppUtils.getDefaultPreferences(context).getString("availableVersion", null)
    }

    fun getCheckTime(context: Context): Long {
        return AppUtils.getDefaultPreferences(context).getLong("checkedForUpdatesTime", 0)
    }

    fun getDefaultUpdater(context: Context): GitHubUpdater {
        return GitHubUpdater(context, AppConfig.URI_REPO_APP_UPDATE, R.style.Theme_TrebleShot, false)
    }

    fun hasNewVersion(context: Context): Boolean {
        val availableVersion = getAvailableVersion(context)
        return availableVersion != null && GitHubUpdater.isNewVersion(context, availableVersion)
    }
}