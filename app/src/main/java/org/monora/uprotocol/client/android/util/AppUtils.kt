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

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.preference.PreferenceManager
import com.genonbeta.android.framework.util.Files.getSecureUri
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.ListingFragmentBase
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.config.Keyword
import org.monora.uprotocol.client.android.dialog.RationalePermissionRequest
import org.monora.uprotocol.client.android.drawable.TextDrawable
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.util.Resources.resToColor
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import java.util.*

object AppUtils {
    val TAG = AppUtils::class.java.simpleName


    fun generateKey(): Int {
        return (Int.MAX_VALUE * Math.random()).toInt()
    }

    fun generateNetworkPin(context: Context): Int {
        val networkPin = generateKey()
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putInt(Keyword.NETWORK_PIN, networkPin)
            .apply()
        return networkPin
    }

    val buildFlavor: Keyword.Flavor
        get() = try {
            Keyword.Flavor.valueOf(BuildConfig.FLAVOR)
        } catch (e: Exception) {
            Log.e(
                TAG, "Current build flavor " + BuildConfig.FLAVOR + " is not specified in " +
                        "the vocab. Is this a custom build?"
            )
            Keyword.Flavor.unknown
        }

    fun getDefaultIconBuilder(context: Context): TextDrawable.IShapeBuilder {
        val builder: TextDrawable.IShapeBuilder = TextDrawable.builder()
        builder.beginConfig()
            .firstLettersOnly(true)
            .textMaxLength(1)
            .bold()
            .textColor(R.attr.colorControlNormal.attrToRes(context).resToColor(context))
            .shapeColor(R.attr.colorPassive.attrToRes(context).resToColor(context))
        return builder
    }

    fun <T : ContentModel> showFolderSelectionHelp(fragment: ListingFragmentBase<T>) {
        val connection = fragment.engineConnection
        val preferences = PreferenceManager.getDefaultSharedPreferences(fragment.requireContext())
        val selectedItemList = connection.getSelectionList() ?: return

        if (selectedItemList.isNotEmpty() && !preferences.getBoolean("helpFolderSelection", false))
            fragment.createSnackbar(R.string.mesg_helpFolderSelection)
                ?.setAction(R.string.butn_gotIt) {
                    preferences
                        .edit()
                        .putBoolean("helpFolderSelection", true)
                        .apply()
                }
                ?.show()
    }

    fun startApplicationDetails(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun startFeedbackActivity(context: Context) {
        val intent = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(AppConfig.EMAIL_DEVELOPER))
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.text_appName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val logFile = Files.createLog(context)
        if (logFile != null) try {
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .putExtra(Intent.EXTRA_STREAM, getSecureUri(context, logFile))
        } catch (e: Exception) {
            e.printStackTrace()
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.butn_feedbackContact)))
    }

    fun getFriendlySSID(ssid: String) = ssid.replace("\"", "").let {
        if (it.startsWith(AppConfig.PREFIX_ACCESS_POINT))
            it.substring((AppConfig.PREFIX_ACCESS_POINT.length))
        else it
    }.replace("_", " ")


}