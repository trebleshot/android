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
package org.monora.uprotocol.client.android.service.backgroundservice

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.activity.HomeActivity
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage.Tone
import org.monora.uprotocol.client.android.util.DynamicNotification
import org.monora.uprotocol.client.android.util.Notifications
import com.google.android.material.snackbar.Snackbar
import java.util.*

class TaskMessageImpl(
    override var title: String,
    override var message: String,
    override var tone: Tone = Tone.Neutral,
) : TaskMessage {
    private val actionList: MutableList<TaskMessage.Action> = ArrayList()

    override fun addAction(action: TaskMessage.Action) {
        synchronized(actionList) { actionList.add(action) }
    }

    override fun addAction(context: Context, nameRes: Int, callback: TaskMessage.Callback?) {
        addAction(context.getString(nameRes), callback)
    }

    override fun addAction(name: String, callback: TaskMessage.Callback?) {
        addAction(name, Tone.Neutral, callback)
    }

    override fun addAction(context: Context, nameRes: Int, tone: Tone, callback: TaskMessage.Callback?) {
        addAction(context.getString(nameRes), tone, callback)
    }

    override fun addAction(name: String, tone: Tone, callback: TaskMessage.Callback?) {
        addAction(TaskMessage.Action(name, tone, callback))
    }

    override fun getActionList(): List<TaskMessage.Action> {
        synchronized(actionList) { return ArrayList(actionList) }
    }

    override fun removeAction(action: TaskMessage.Action) {
        synchronized(actionList) { actionList.remove(action) }
    }

    override fun setMessage(context: Context, msgRes: Int) {
        message = context.getString(msgRes)
    }

    override fun setTitle(context: Context, titleRes: Int) {
        title = context.getString(titleRes)
    }

    override fun sizeOfActions(): Int {
        synchronized(actionList) { return actionList.size }
    }

    override fun toDialogBuilder(activity: Activity): AlertDialog.Builder {
        val builder = AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(message)
        synchronized(actionList) {
            val appliedTones = BooleanArray(Tone.values().size)
            for (action in actionList) {
                if (appliedTones[action.tone.ordinal]) continue
                when (action.tone) {
                    Tone.Positive -> builder.setPositiveButton(action.name) { dialog: DialogInterface?, which: Int ->
                        action.callback?.call(activity)
                    }
                    Tone.Negative -> builder.setNegativeButton(action.name) { dialog: DialogInterface?, which: Int ->
                        action.callback?.call(activity)
                    }
                    else -> builder.setNeutralButton(action.name) { dialog: DialogInterface?, which: Int ->
                        action.callback?.call(activity)
                    }
                }
                appliedTones[action.tone.ordinal] = true
            }
            if (appliedTones.isEmpty() || !appliedTones[Tone.Negative.ordinal])
                builder.setNegativeButton(R.string.butn_close, null)
        }
        return builder
    }

    override fun toNotification(task: AsyncTask): DynamicNotification {
        val context = task.context.applicationContext
        val utils = task.notificationHelper.utils
        val notification = utils.buildDynamicNotification(task.hashCode(), Notifications.NOTIFICATION_CHANNEL_HIGH)
        val intent: PendingIntent = PendingIntent.getActivity(
            context, 0, Intent(context, HomeActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0
        )
        notification.setSmallIcon(iconFor(tone))
            .setGroup(task.getTaskGroup())
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(intent)
            .setAutoCancel(true)
        for (action in actionList) notification.addAction(
            iconFor(action.tone), action.name, PendingIntent.getActivity(
                context, 0, Intent(context, HomeActivity::class.java), 0
            )
        )
        return notification
    }

    override fun toSnackbar(view: View): Snackbar {
        val snackbar: Snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG)
        if (sizeOfActions() > 0) {
            synchronized(actionList) {
                val action = actionList[0]
                snackbar.setAction(action.name) { v: View -> action.callback?.call(v.context) }
            }
        }
        return snackbar
    }

    override fun toString(): String = "Title=$title Msg=$message Tone=$tone"

    companion object {
        @DrawableRes
        fun iconFor(tone: Tone?): Int {
            return when (tone) {
                Tone.Confused -> R.drawable.ic_help_white_24_static
                Tone.Positive -> R.drawable.ic_check_white_24dp_static
                Tone.Negative -> R.drawable.ic_close_white_24dp_static
                Tone.Neutral -> R.drawable.ic_trebleshot_white_24dp_static
                else -> R.drawable.ic_trebleshot_white_24dp_static
            }
        }
    }
}