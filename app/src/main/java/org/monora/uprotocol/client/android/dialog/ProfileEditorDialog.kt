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

import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceManager
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.databinding.LayoutProfileEditorBinding

class ProfileEditorDialog(activity: Activity) : AlertDialog.Builder(activity) {
    private var dialog: AlertDialog? = null

    private fun closeIfPossible() {
        dialog?.let {
            if (it.isShowing) it.dismiss()
            dialog = null
        }
    }

    override fun show(): AlertDialog {
        return super.show().also { dialog = it }
    }

    private fun saveNickname(editText: EditText) {
        PreferenceManager.getDefaultSharedPreferences(context).edit()
            .putString("client_nickname", editText.text.toString())
            .apply()
    }

    init {
        val binding = LayoutProfileEditorBinding.inflate(
            LayoutInflater.from(activity),
            null,
            false
        )

        setView(binding.root)

        binding.editText.requestFocus()
        binding.editText.setOnClickListener {
            saveNickname(it as EditText)
            closeIfPossible()
        }
        setNegativeButton(R.string.butn_remove) { _: DialogInterface?, _: Int ->
            activity.deleteFile("profilePicture")
        }
        setPositiveButton(R.string.butn_save) { _: DialogInterface?, _: Int ->
            saveNickname(binding.editText)
        }
        setNeutralButton(R.string.butn_close, null)
    }
}