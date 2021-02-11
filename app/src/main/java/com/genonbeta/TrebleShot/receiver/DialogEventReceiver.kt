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
package com.genonbeta.TrebleShot.receiver

import android.app.AlertDialog
import android.app.Dialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.view.WindowManager
import com.genonbeta.TrebleShot.R

class DialogEventReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (ACTION_DIALOG == intent.action && Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) showDialog(
            context,
            intent.getStringExtra(EXTRA_TITLE),
            intent.getStringExtra(EXTRA_MESSAGE),
            intent.getParcelableExtra(EXTRA_POSITIVE_INTENT),
            intent.getParcelableExtra(EXTRA_NEGATIVE_INTENT)
        )
    }

    fun showDialog(
        context: Context,
        title: String?,
        message: String?,
        accept: PendingIntent?,
        reject: PendingIntent?,
    ) {
        val dialogBuilder = AlertDialog.Builder(context)
        if (title != null) dialogBuilder.setTitle(title)
        if (message != null) dialogBuilder.setMessage(message)
        if (accept != null) dialogBuilder.setPositiveButton(android.R.string.ok) { p1: DialogInterface?, p2: Int ->
            try {
                accept.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
        }
        if (reject != null) dialogBuilder.setNegativeButton(android.R.string.cancel) { p1: DialogInterface?, p2: Int ->
            try {
                reject.send()
            } catch (e: PendingIntent.CanceledException) {
                e.printStackTrace()
            }
        } else dialogBuilder.setNegativeButton(R.string.butn_close, null)
        val dialog: Dialog = dialogBuilder.create()
        dialog.window?.setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
        dialog.show()
    }

    companion object {
        const val ACTION_DIALOG = "com.genonbeta.TrebleShot.action.makeDialog"
        const val EXTRA_TITLE = "title"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_POSITIVE_INTENT = "positive"
        const val EXTRA_NEGATIVE_INTENT = "negative"
    }
}