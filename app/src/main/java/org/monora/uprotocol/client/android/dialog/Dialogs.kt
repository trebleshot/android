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

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.core.transfer.TransferItem
import java.util.*

/**
 * created by: veli
 * date: 7/3/19 7:53 PM
 */
object Dialogs {
    fun showGenericCheckBoxDialog(
        activity: Activity,
        @StringRes title: Int,
        content: String?,
        @StringRes positiveButton: Int,
        @StringRes checkBox: Int,
        positiveListener: ClickListener,
        vararg textArgs: String?,
    ) {
        val view = LayoutInflater.from(activity).inflate(
            R.layout.abstract_layout_dialog_text_option,
            null
        )
        val text1: TextView = view.findViewById(R.id.text1)
        val checkBox1: CheckBox = view.findViewById(R.id.checkbox1)

        text1.text = content
        if (checkBox == 0) checkBox1.visibility = View.GONE else checkBox1.setText(checkBox)
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setView(view)
            .setNegativeButton(R.string.butn_cancel, null)
            .setPositiveButton(positiveButton) { dialog: DialogInterface, which: Int ->
                positiveListener.onClick(dialog, which, checkBox1)
            }
            .show()
    }

    fun showRemoveDialog(activity: Activity, transfer: Transfer) {
        showGenericCheckBoxDialog(
            activity,
            R.string.ques_removeAll,
            activity.getString(R.string.text_removeTransferGroupSummary),
            R.string.butn_remove,
            R.string.text_alsoDeleteReceivedFiles,
            object : ClickListener {
                override fun onClick(dialog: DialogInterface, which: Int, checkBox: CheckBox) {
                    // FIXME: 2/25/21 Transfer removal
                    /*
                    transfer.deleteFilesOnRemoval = checkBox.isChecked == true
                    AppUtils.getKuick(activity).removeAsynchronous(activity, transfer, null)

                     */
                }
            }
        )
    }

    fun showRemoveDialog(activity: Activity, item: TransferItem) {
        val checkBox = if (TransferItem.Type.Incoming == item.itemType) R.string.text_alsoDeleteReceivedFiles else 0
        showGenericCheckBoxDialog(
            activity,
            R.string.ques_removeTransfer,
            activity.getString(R.string.text_removeTransferSummary, item.itemName),
            R.string.butn_remove,
            checkBox,
            object : ClickListener {
                override fun onClick(dialog: DialogInterface, which: Int, checkBox: CheckBox) {
                    // FIXME: 2/25/21 TransferItem removal
                    /*
                    item.setDeleteOnRemoval(checkBox.isChecked)
                    AppUtils.getKuick(activity).removeAsynchronous(activity, item, null)

                     */
                }
            }
        )
    }

    fun showRemoveTransferObjectListDialog(
        activity: Activity,
        objects: List<TransferItem>,
    ) {
        val copiedObjects: List<TransferItem> = ArrayList(objects)
        showGenericCheckBoxDialog(
            activity,
            R.string.ques_removeTransfer,
            activity.resources.getQuantityString(R.plurals.text_removeQueueSummary, objects.size, objects.size),
            R.string.butn_remove, R.string.text_alsoDeleteReceivedFiles,
            object : ClickListener {
                override fun onClick(dialog: DialogInterface, which: Int, checkBox: CheckBox) {
                    // FIXME: 2/25/21 Transfer item list removal
                    /**
                    val isChecked = checkBox.isChecked
                    for (item in copiedObjects) item.setDeleteOnRemoval(isChecked)
                    AppUtils.getKuick(activity).removeAsynchronous(activity, copiedObjects, null)
                     */
                }
            }
        )
    }

    fun showRemoveTransferGroupListDialog(
        activity: Activity,
        groups: List<Transfer>,
    ) {
        val copiedTransfers: List<Transfer> = ArrayList(groups)
        showGenericCheckBoxDialog(
            activity,
            R.string.ques_removeAll,
            activity.getString(R.string.text_removeSelected),
            R.string.butn_remove,
            R.string.text_alsoDeleteReceivedFiles,
            object : ClickListener {
                override fun onClick(dialog: DialogInterface, which: Int, checkBox: CheckBox) {
                    val isChecked = checkBox.isChecked
                    // FIXME: 2/25/21 Transfer list removal
                    /*
                    for (transfer in copiedTransfers) transfer.deleteFilesOnRemoval = isChecked
                    AppUtils.getKuick(activity).removeAsynchronous(activity, copiedTransfers, null)

                     */
                }
            }
        )
    }

    interface ClickListener {
        fun onClick(dialog: DialogInterface, which: Int, checkBox: CheckBox)
    }
}