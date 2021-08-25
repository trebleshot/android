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

import android.app.Service
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.genonbeta.android.framework.io.DocumentFile
import org.monora.uprotocol.client.android.R

object Activities {
    private const val MIME_APK = "application/vnd.android.package-archive"

    fun startLocationServiceSettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun view(context: Context, documentFile: DocumentFile) {
        view(
            context,
            documentFile.getSecureUri(context, context.getString(R.string.file_provider)),
            documentFile.getType()
        )
    }

    fun view(context: Context, uri: Uri, type: String) {
        try {
            if (MIME_APK == type) {
                if (Build.VERSION.SDK_INT >= 29) {
                    // TODO: 8/10/21 PackageInstaller for API 29
                } else {
                    @Suppress("DEPRECATION")
                    context.startActivity(
                        Intent(Intent.ACTION_INSTALL_PACKAGE)
                            .setDataAndType(uri, type)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    )
                }
            } else {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setDataAndType(uri, type)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                )
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.error_no_activity_to_view, Toast.LENGTH_LONG).show()
        } catch (e: SecurityException) {
            Toast.makeText(context, R.string.text_contentNotFound, Toast.LENGTH_LONG).show()
        }
    }
}
