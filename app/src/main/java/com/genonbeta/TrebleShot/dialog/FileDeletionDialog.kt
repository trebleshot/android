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
package com.genonbeta.TrebleShot.dialogimport

import android.app.Activity
import android.content.DialogInterface
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.FileListAdapter
import com.genonbeta.TrebleShot.task.FileDeletionTask

/**
 * Created by: veli
 * Date: 5/21/17 2:21 AM
 */
class FileDeletionDialog(activity: Activity, items: List<FileListAdapter.FileHolder>) : AlertDialog.Builder(activity) {
    init {
        setTitle(R.string.text_deleteConfirm)
        setMessage(context.resources.getQuantityString(R.plurals.ques_deleteFile, items.size, items.size))
        setNegativeButton(R.string.butn_cancel, null)
        setPositiveButton(R.string.butn_delete) { dialog: DialogInterface?, p2: Int ->
            App.from(activity).run(FileDeletionTask(items))
        }
    }
}