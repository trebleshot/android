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
package org.monora.uprotocol.client.android.dialog

import android.annotation.SuppressLint
import android.app.Activity
import android.content.DialogInterface
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.model.TransferIndex
import org.monora.uprotocol.client.android.model.TransferItem
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.Files
import org.monora.uprotocol.client.android.util.TextUtils
import com.genonbeta.android.framework.util.Files.fromUri
import com.genonbeta.android.framework.util.Files.getOpenIntent
import com.genonbeta.android.framework.util.Files.openUri
import com.genonbeta.android.framework.util.Files.formatLength
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 10.11.2017 14:59
 */
class TransferInfoDialog(
    activity: Activity, loadedGroup: TransferIndex,
    item: TransferItem, deviceId: String?,
) : AlertDialog.Builder(activity) {
    init {
        val isIncoming = TransferItem.Type.INCOMING == item.type
        val file = try {
            val attemptedFile = if (isIncoming) Files.getIncomingPseudoFile(
                context, item, loadedGroup.transfer, false
            ) else fromUri(context, Uri.parse(item.file))

            if (attemptedFile.canRead()) attemptedFile else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        val parentFile = file?.getParentFile()
        val unknown = context.getString(R.string.text_unknown)

        @SuppressLint("InflateParams") val rootView = LayoutInflater.from(activity).inflate(
            R.layout.layout_transfer_info, null
        )
        val nameText: TextView = rootView.findViewById(R.id.transfer_info_file_name)
        val sizeText: TextView = rootView.findViewById(R.id.transfer_info_file_size)
        val typeText: TextView = rootView.findViewById(R.id.transfer_info_file_mime)
        val flagText: TextView = rootView.findViewById(R.id.transfer_info_file_status)
        val incomingDetailsLayout = rootView.findViewById<View>(R.id.transfer_info_incoming_details_layout)
        val receivedSizeText: TextView = rootView.findViewById(R.id.transfer_info_received_size)
        val locationText: TextView = rootView.findViewById(R.id.transfer_info_pseudo_location)
        setTitle(R.string.text_transactionDetails)
        setView(rootView)
        nameText.text = item.name
        sizeText.text = formatLength(item.length, false)
        typeText.text = item.mimeType
        receivedSizeText.text = if (file != null) formatLength(file.getLength(), false) else unknown
        locationText.text = if (file != null) Files.getReadableUri(file.getUri()) else unknown
        flagText.text = TextUtils.getTransactionFlagString(context, item, NumberFormat.getPercentInstance(), deviceId)
        setPositiveButton(R.string.butn_close, null)
        setNegativeButton(R.string.butn_remove) { dialogInterface: DialogInterface?, i: Int ->
            DialogUtils.showRemoveDialog(activity, item)
        }
        if (isIncoming) {
            incomingDetailsLayout.visibility = View.VISIBLE
            if (TransferItem.Flag.INTERRUPTED == item.flag || TransferItem.Flag.IN_PROGRESS == item.flag) {
                setNeutralButton(R.string.butn_retry) { dialogInterface: DialogInterface?, i: Int ->
                    item.flag = TransferItem.Flag.PENDING
                    AppUtils.getKuick(activity).publish(item)
                    AppUtils.getKuick(activity).broadcast()
                }
            } else if (file != null) {
                if (TransferItem.Flag.REMOVED == item.flag && parentFile != null) {
                    setNeutralButton(R.string.butn_saveAnyway) { dialogInterface: DialogInterface?, i: Int ->
                        val saveAnyway = AlertDialog.Builder(context)
                        saveAnyway.setTitle(R.string.ques_saveAnyway)
                        saveAnyway.setMessage(R.string.text_saveAnywaySummary)
                        saveAnyway.setNegativeButton(R.string.butn_cancel, null)
                        saveAnyway.setPositiveButton(R.string.butn_proceed) { dialog: DialogInterface?, which: Int ->
                            try {
                                val savedFile = Files.saveReceivedFile(parentFile, file, item)
                                item.flag = TransferItem.Flag.DONE
                                AppUtils.getKuick(activity).update(item)
                                AppUtils.getKuick(activity).broadcast()
                                Toast.makeText(context, R.string.mesg_fileSaved, Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Toast.makeText(context, R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show()
                            }
                        }
                        saveAnyway.show()
                    }
                } else if (TransferItem.Flag.DONE == item.flag) {
                    setNeutralButton(R.string.butn_open) { dialog: DialogInterface?, which: Int ->
                        try {
                            openUri(context, file)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } else if (file != null) try {
            val startIntent = getOpenIntent(context, file)
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