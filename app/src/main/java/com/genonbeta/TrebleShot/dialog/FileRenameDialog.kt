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
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.android.framework.io.DocumentFile
import java.util.*

/**
 * created by: Veli
 * date: 26.02.2018 08:53
 */
class FileRenameDialog(activity: Activity, list: List<FileHolder?>?) : AbstractSingleTextInputDialog(activity) {
    companion object {
        val TAG = FileRenameDialog::class.java.simpleName
    }

    init {
        val itemList: List<FileHolder> = ArrayList<FileHolder>(list)
        val multiple = itemList.size > 1
        setTitle(if (multiple) R.string.text_renameMultipleItems else R.string.text_rename)
        editText.setText(if (multiple) "%d" else itemList[0].fileName)
        setOnProceedClickListener(R.string.butn_rename) { dialog: AlertDialog? ->
            val renameTo = editText.text.toString()
            if (multiple) try {
                String.format(renameTo, itemList.size)
                App.Companion.from(activity).run(RenameMultipleFilesTask(itemList, renameTo))
            } catch (e: Exception) {
                editText.error = activity.getString(R.string.text_errorIncludePrintfPlaceholder)
                return@setOnProceedClickListener false
            } else if (itemList.size == 1) {
                val fileHolder: FileHolder = itemList[0]
                val scannerList: List<DocumentFile> = ArrayList()
                RenameMultipleFilesTask.Companion.renameFile(
                    AppUtils.getKuick(activity),
                    fileHolder,
                    renameTo,
                    scannerList
                )
                RenameMultipleFilesTask.Companion.notifyFileChanges(context, scannerList)
            }
            true
        }
    }
}