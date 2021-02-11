/*
 * Copyright (C) 2020 Veli Tasalı
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
package com.genonbeta.TrebleShot.service.backgroundservice

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.util.DynamicNotification
import com.google.android.material.snackbar.Snackbar

interface TaskMessage {
    var title: String

    var message: String

    var tone: Tone

    fun addAction(action: Action)

    fun addAction(context: Context, nameRes: Int, callback: Callback?)

    fun addAction(name: String, callback: Callback?)

    fun addAction(context: Context, nameRes: Int, tone: Tone, callback: Callback?)

    fun addAction(name: String, tone: Tone, callback: Callback?)

    fun getActionList(): List<Action>

    fun removeAction(action: Action)

    fun setMessage(context: Context, msgRes: Int)

    fun setTitle(context: Context, titleRes: Int)

    fun sizeOfActions(): Int

    fun toDialogBuilder(activity: Activity): AlertDialog.Builder

    fun toNotification(task: AsyncTask): DynamicNotification

    fun toSnackbar(view: View): Snackbar

    enum class Tone {
        Positive, Confused, Neutral, Negative
    }

    class Action(val name: String, val tone: Tone = Tone.Neutral, val callback: Callback? = null) {
        override fun toString(): String {
            return "Action [\n\tName=$name\n\tTone=$tone\n]\n"
        }
    }

    interface Callback {
        fun call(context: Context)
    }

    companion object {
        fun newInstance(title: String, message: String, tone: Tone = Tone.Neutral): TaskMessage {
            return TaskMessageImpl(title, message, tone)
        }
    }
}