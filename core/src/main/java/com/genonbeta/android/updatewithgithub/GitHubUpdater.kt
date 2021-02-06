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
package com.genonbeta.android.updatewithgithub

import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat.startActivity
import com.genonbeta.android.framework.R
import org.apache.maven.artifact.versioning.ComparableVersion
import org.json.JSONArray


/**
 * Created by: veli
 * Date: 11/13/16 8:25 AM
 */

class GitHubUpdater(
    private val activity: Activity,
    private val repo: String,
    private val includePreReleases: Boolean
) {
    fun checkForUpdates(popupDialog: Boolean, listener: OnInfoAvailableListener?) {
        if (popupDialog)
            Toast.makeText(activity, R.string.genfw_uwg_check_for_updates_ongoing, Toast.LENGTH_LONG).show()

        val thread = Thread {
            try {
                Log.d(TAG, "Checking updates")

                val server = RemoteServer(repo)
                val result = server.connect(null, null)

                Log.d(TAG, "Server connected")

                val packageInfo = activity.packageManager.getPackageInfo(activity.applicationInfo.packageName, 0)
                val versionName = packageInfo.versionName
                val applicationName = getAppLabel()
                val releases = JSONArray(result)

                if (releases.length() > 0) {
                    Log.d(TAG, "Reading releases: (total) " + releases.length())

                    for (iterator in 0 until releases.length()) {
                        val releaseObject = releases.getJSONObject(iterator)
                        val isPreRelease = releaseObject.getBoolean("prerelease")
                        if (isPreRelease && !includePreReleases)
                            continue
                        val updateVersion = releaseObject.getString("tag_name")
                        val updateTitle = releaseObject.getString("name")
                        val updateDate = releaseObject.getString("published_at")
                        val updateBody = releaseObject.getString("body")
                        val pageUrl = releaseObject.getString("html_url")
                        val comparableLatest = ComparableVersion(updateVersion)
                        val comparableCurrent = ComparableVersion(versionName)
                        val isNew = comparableLatest > comparableCurrent

                        activity.runOnUiThread {
                            if (!activity.isFinishing)
                                listener?.onInfoAvailable(isNew, updateVersion, updateTitle, updateBody, updateDate)
                        }

                        if (popupDialog && isNew) {
                            Log.d(TAG, "New version found: $updateVersion")

                            if (releaseObject.has("assets")) {
                                Log.d(TAG, "Reading assets")
                                val releaseAssets = releaseObject.getJSONArray("assets")

                                if (releaseAssets.length() > 0) {
                                    Log.d(TAG, "Assets is cached: (total) " + releaseAssets.length())

                                    val visitPage = DialogInterface.OnClickListener { _, _ ->
                                        startActivity(
                                            activity, Intent(Intent.ACTION_VIEW, Uri.parse(pageUrl)),
                                            null
                                        )
                                    }

                                    activity.runOnUiThread {
                                        AlertDialog.Builder(activity)
                                            .setTitle(R.string.genfw_uwg_update_available)
                                            .setMessage(
                                                String.format(
                                                    activity.getString(R.string.genfw_uwg_update_body),
                                                    versionName,
                                                    updateVersion,
                                                    updateDate,
                                                    updateBody
                                                )
                                            )
                                            .setPositiveButton(R.string.genfw_uwg_visit_page, visitPage)
                                            .setNegativeButton(android.R.string.cancel, null)
                                            .show()
                                    }

                                } else Log.d(TAG, "No downloadable file is provided")
                            } else if (popupDialog)
                                activity.runOnUiThread {
                                    Toast.makeText(activity, R.string.genfw_uwg_no_update, Toast.LENGTH_LONG).show()
                                }
                        } else if (popupDialog)
                            activity.runOnUiThread {
                                Toast.makeText(activity, R.string.genfw_uwg_up_to_date, Toast.LENGTH_LONG).show()
                            }
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()

                if (popupDialog)
                    activity.runOnUiThread {
                        Toast.makeText(activity, R.string.genfw_uwg_version_check_error, Toast.LENGTH_LONG).show()
                    }
            }
        }

        thread.start()
    }

    private fun getAppLabel(): String? {
        val packageManager = activity.packageManager

        try {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(
                    activity.applicationInfo.packageName,
                    0
                )
            )
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }
        return null
    }

    fun isNewVersion(versionName: String): Boolean {
        return isNewVersion(activity, versionName)
    }

    interface OnInfoAvailableListener {
        fun onInfoAvailable(
            newVersion: Boolean,
            versionName: String?,
            title: String?,
            description: String?,
            releaseDate: String?
        )
    }

    companion object {
        val TAG: String = GitHubUpdater::class.java.simpleName

        fun isNewVersion(context: Context, versionName: String): Boolean {
            try {
                val packageInfo: PackageInfo =
                    context.packageManager.getPackageInfo(context.applicationInfo.packageName, 0)
                val comparableGiven = ComparableVersion(versionName)
                val comparableCurrent = ComparableVersion(packageInfo.versionName)

                return comparableGiven > comparableCurrent
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
            }
            return false
        }
    }
}