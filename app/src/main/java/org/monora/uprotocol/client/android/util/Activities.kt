/*
 * Copyright (C) 2021 Veli Tasalı
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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig
import android.content.ContextWrapper
import android.view.View


object Activities {
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
                .putExtra(
                    Intent.EXTRA_STREAM,
                    com.genonbeta.android.framework.util.Files.getSecureUri(context, logFile)
                )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        context.startActivity(Intent.createChooser(intent, context.getString(R.string.butn_feedbackContact)))
    }

    fun startLocationServiceSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}