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

import android.app.Activity
import android.content.DialogInterface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.util.*
import com.genonbeta.android.framework.io.DocumentFile
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */
class TransferInfoDialog(
    activity: Activity, loadedGroup: TransferIndex,
    `object`: TransferItem, deviceId: String?
) : AlertDialog.Builder(activity) {
    init {
        var attemptedFile: DocumentFile? = null
        val isIncoming = TransferItem.Type.INCOMING == `object`.type
        try {
            // If it is incoming than get the received or cache file
            // If not then try to reach to the source file that is being send
            attemptedFile = if (isIncoming) Files.getIncomingPseudoFile(
                context, `object`, loadedGroup.transfer,
                false
            ) else com.genonbeta.android.framework.util.Files.fromUri(context, Uri.parse(`object`.file))
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val pseudoFile = attemptedFile
        val fileExists = pseudoFile != null && pseudoFile.canRead()
        @SuppressLint("InflateParams") val rootView =
            LayoutInflater.from(activity).inflate(R.layout.layout_transfer_info, null)
        val nameText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_name)
        val sizeText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_size)
        val typeText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_mime)
        val flagText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_file_status)
        val incomingDetailsLayout = rootView.findViewById<View>(R.id.transfer_info_incoming_details_layout)
        val receivedSizeText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_received_size)
        val locationText: TextView = rootView.findViewById<TextView>(R.id.transfer_info_pseudo_location)
        setTitle(R.string.text_transactionDetails)
        setView(rootView)
        nameText.setText(`object`.name)
        sizeText.setText(com.genonbeta.android.framework.util.Files.sizeExpression(`object`.comparableSize, false))
        typeText.setText(`object`.mimeType)
        receivedSizeText.setText(
            if (fileExists) com.genonbeta.android.framework.util.Files.sizeExpression(
                pseudoFile!!.getLength(),
                false
            ) else context.getString(R.string.text_unknown)
        )
        locationText.setText(if (fileExists) Files.getReadableUri(pseudoFile!!.uri) else context.getString(R.string.text_unknown))
        flagText.setText(
            TextUtils.getTransactionFlagString(
                context, `object`,
                NumberFormat.getPercentInstance(), deviceId
            )
        )
        setPositiveButton(R.string.butn_close, null)
        setNegativeButton(
            R.string.butn_remove
        ) { dialogInterface: DialogInterface?, i: Int -> DialogUtils.showRemoveDialog(activity, `object`) }
        if (isIncoming) {
            incomingDetailsLayout.visibility = View.VISIBLE
            if (TransferItem.Flag.INTERRUPTED == `object`.flag || TransferItem.Flag.IN_PROGRESS == `object`.flag) {
                setNeutralButton(R.string.butn_retry) { dialogInterface: DialogInterface?, i: Int ->
                    `object`.flag = TransferItem.Flag.PENDING
                    AppUtils.getKuick(activity).publish(`object`)
                    AppUtils.getKuick(activity).broadcast()
                }
            } else if (fileExists) {
                if (TransferItem.Flag.REMOVED == `object`.flag && pseudoFile!!.parentFile != null) {
                    setNeutralButton(R.string.butn_saveAnyway) { dialogInterface: DialogInterface?, i: Int ->
                        val saveAnyway = AlertDialog.Builder(
                            context
                        )
                        saveAnyway.setTitle(R.string.ques_saveAnyway)
                        saveAnyway.setMessage(R.string.text_saveAnywaySummary)
                        saveAnyway.setNegativeButton(R.string.butn_cancel, null)
                        saveAnyway.setPositiveButton(R.string.butn_proceed) { dialog: DialogInterface?, which: Int ->
                            try {
                                val savedFile = Files.saveReceivedFile(
                                    pseudoFile.parentFile, pseudoFile, `object`
                                )
                                `object`.flag = TransferItem.Flag.DONE
                                AppUtils.getKuick(activity).update(`object`)
                                AppUtils.getKuick(activity).broadcast()
                                Toast.makeText(context, R.string.mesg_fileSaved, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show()
                            }
                        }
                        saveAnyway.show()
                    }
                } else if (TransferItem.Flag.DONE == `object`.flag) {
                    setNeutralButton(R.string.butn_open) { dialog: DialogInterface?, which: Int ->
                        try {
                            com.genonbeta.android.framework.util.Files.openUri(context, pseudoFile)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else if (fileExists) try {
            val startIntent = com.genonbeta.android.framework.util.Files.getOpenIntent(context, attemptedFile)
            setNeutralButton(R.string.butn_open) { dialog: DialogInterface?, which: Int ->
                try {
                    context.startActivity(startIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (ignored: Exception) {
        }
    }
}