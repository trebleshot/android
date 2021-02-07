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
package com.genonbeta.TrebleShot.dialog

import android.content.*
import android.os.*
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.util.Files
import com.genonbeta.android.framework.io.DocumentFile
import java.io.File

class ShareAppDialog(context: Context) : AlertDialog.Builder(context) {
    private fun shareAsApk(context: Context) {
        try {
            val interrupter: Stoppable = StoppableImpl()
            val pm = context.packageManager
            val packageInfo: PackageInfo = pm.getPackageInfo(context.applicationInfo.packageName, 0)
            val fileName: String = packageInfo.applicationInfo.loadLabel(pm).toString() + "_" + packageInfo.versionName
            val storageDirectory = Files.getApplicationDirectory(context.applicationContext)
            val codeFile = DocumentFile.fromFile(File(context.applicationInfo.sourceDir))
            val cloneFile = storageDirectory!!.createFile(codeFile.type, fileName)
            if (cloneFile.exists()) cloneFile.delete()
            Files.copy(context, codeFile, cloneFile, interrupter)
            try {
                val sendIntent = Intent(Intent.ACTION_SEND)
                    .putExtra(
                        Intent.EXTRA_STREAM,
                        com.genonbeta.android.framework.util.Files.getSecureUri(context, cloneFile)
                    )
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType(cloneFile.type)
                context.startActivity(
                    Intent.createChooser(
                        sendIntent, context.getString(
                            R.string.text_fileShareAppChoose
                        )
                    )
                )
            } catch (e: IllegalArgumentException) {
                Toast.makeText(context, R.string.mesg_providerNotAllowedError, Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun shareAsLink(context: Context) {
        Handler(Looper.getMainLooper()).post {
            try {
                val textToShare = context.getString(R.string.text_linkTrebleshot, AppConfig.URI_GOOGLE_PLAY)
                val sendIntent = Intent(Intent.ACTION_SEND)
                    .putExtra(Intent.EXTRA_TEXT, textToShare)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    .setType("text/plain")
                context.startActivity(
                    Intent.createChooser(
                        sendIntent, context.getString(
                            R.string.text_fileShareAppChoose
                        )
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    init {
        setMessage(R.string.ques_shareAsApkOrLink)
        setNegativeButton(R.string.butn_cancel, null)
        setNeutralButton(R.string.butn_asApk) { dialogInterface: DialogInterface?, i: Int -> shareAsApk(context) }
        setPositiveButton(R.string.butn_asLink) { dialogInterface: DialogInterface?, i: Int -> shareAsLink(context) }
    }
}