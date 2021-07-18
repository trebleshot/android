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

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog

/**
 * created by: Veli
 * date: 26.02.2018 08:19
 */
abstract class AbstractFailureAwareDialog(context: Context) : AlertDialog.Builder(context) {
    private var clickListener: OnProceedClickListener? = null

    fun setOnProceedClickListener(buttonText: String, listener: OnProceedClickListener) {
        setPositiveButton(buttonText, null)
        clickListener = listener
    }

    fun setOnProceedClickListener(buttonRes: Int, listener: OnProceedClickListener) {
        setOnProceedClickListener(context.getString(buttonRes), listener)
    }

    override fun show(): AlertDialog {
        val dialog = super.show()
        clickListener?.let {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener { v: View? ->
                if (it.onProceedClick(dialog)) dialog.dismiss()
            }
        }

        return dialog
    }

    interface OnProceedClickListener {
        fun onProceedClick(dialog: AlertDialog): Boolean
    }
}