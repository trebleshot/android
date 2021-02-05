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
import com.genonbeta.TrebleShot.utilimport.DynamicNotification
import com.google.android.material.snackbar.Snackbar

interface TaskMessage {
    fun addAction(action: Action): TaskMessage

    fun addAction(context: Context, nameRes: Int, callback: Callback?): TaskMessage

    fun addAction(name: String?, callback: Callback?): TaskMessage

    fun addAction(context: Context, nameRes: Int, tone: Tone?, callback: Callback?): TaskMessage

    fun addAction(name: String?, tone: Tone?, callback: Callback?): TaskMessage

    fun getActionList(): List<Action>

    fun getMessage(): String?

    fun getTitle(): String?

    fun getTone(): Tone

    fun removeAction(action: Action): TaskMessage

    fun setMessage(context: Context, msgRes: Int): TaskMessage

    fun setMessage(msg: String?): TaskMessage

    fun setTitle(context: Context, titleRes: Int): TaskMessage

    fun setTitle(title: String?): TaskMessage

    fun setTone(tone: Tone): TaskMessage

    fun sizeOfActions(): Int

    fun toDialogBuilder(activity: Activity): AlertDialog.Builder

    fun toNotification(task: AsyncTask): DynamicNotification?

    fun toSnackbar(view: View): Snackbar

    enum class Tone {
        Positive, Confused, Neutral, Negative
    }

    class Action {
        var tone: Tone? = null
        var name: String? = null
        var callback: Callback? = null
        override fun toString(): String {
            return "Action [\n\tName=$name\n\tTone=$tone\n]\n"
        }
    }

    interface Callback {
        fun call(context: Context?)
    }

    companion object {
        fun newInstance(): TaskMessage? {
            return TaskMessageImpl()
        }
    }
}