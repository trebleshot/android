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
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.FileListAdapter.FileHolder
import com.genonbeta.TrebleShot.task.RenameMultipleFilesTask
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.android.framework.io.DocumentFile
import java.util.*

/**
 * created by: Veli
 * date: 26.02.2018 08:53
 */
class FileRenameDialog(val activity: Activity, val list: List<FileHolder>) : AbstractSingleTextInputDialog(activity) {
    override fun show(): AlertDialog {
        val itemList: List<FileHolder> = ArrayList(list)
        val multiple = itemList.size > 1

        setTitle(if (multiple) R.string.text_renameMultipleItems else R.string.text_rename)
        editText.setText(if (multiple) "%d" else itemList[0].name())

        setOnProceedClickListener(R.string.butn_rename, object : OnProceedClickListener {
            override fun onProceedClick(dialog: AlertDialog): Boolean {
                val renameTo = editText.text.toString()
                if (multiple) try {
                    String.format(renameTo, itemList.size)
                    App.from(activity).run(RenameMultipleFilesTask(itemList, renameTo))
                } catch (e: Exception) {
                    editText.error = activity.getString(R.string.text_errorIncludePrintfPlaceholder)
                    return false
                } else if (itemList.size == 1) {
                    val fileHolder: FileHolder = itemList[0]
                    val scannerList: MutableList<DocumentFile> = ArrayList()

                    RenameMultipleFilesTask.renameFile(
                        AppUtils.getKuick(activity),
                        fileHolder,
                        renameTo,
                        scannerList
                    )
                    RenameMultipleFilesTask.notifyFileChanges(context, scannerList)
                }

                return true
            }
        })

        return super.show()
    }

    companion object {
        val TAG = FileRenameDialog::class.java.simpleName
    }
}