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

package com.genonbeta.TrebleShot.util;

import android.content.Context;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.android.updatewithgithub.GitHubUpdater;

/**
 * created by: Veli
 * date: 30.12.2017 17:08
 */

public class Updates
{
    public static void checkForUpdates(final Context context, GitHubUpdater updater, boolean popupDialog,
                                       final GitHubUpdater.OnInfoAvailableListener listener)
    {
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

    public static String getAvailableVersion(Context context)
    {
        return AppUtils.getDefaultPreferences(context).getString("availableVersion", null);
    }

    public static GitHubUpdater getDefaultUpdater(Context context)
    {
        return new GitHubUpdater(context, AppConfig.URI_REPO_APP_UPDATE, R.style.Theme_TrebleShot, false);
    }

    public static long getLastTimeCheckedForUpdates(Context context)
    {
        return AppUtils.getDefaultPreferences(context).getLong("checkedForUpdatesTime", 0);
    }

    public static boolean hasNewVersion(Context context)
    {
        String availableVersion = getAvailableVersion(context);
        return availableVersion != null && GitHubUpdater.isNewVersion(context, availableVersion);
    }
}
