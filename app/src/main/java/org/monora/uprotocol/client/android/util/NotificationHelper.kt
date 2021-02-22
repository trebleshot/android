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
package org.monora.uprotocol.client.android.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.widget.Toast
import androidx.core.app.NotificationCompat
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.*
import org.monora.uprotocol.client.android.model.Device
import org.monora.uprotocol.client.android.model.Transfer
import org.monora.uprotocol.client.android.model.TransferItem
import org.monora.uprotocol.client.android.receiver.DialogEventReceiver
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.task.FileTransferTask
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.Files
import org.monora.uprotocol.client.android.database.model.SharedTextModel
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 26.01.2018 18:29
 */
class NotificationHelper(val utils: Notifications) {
    private val percentFormat = NumberFormat.getPercentInstance()

    val foregroundNotification: DynamicNotification
        get() {
            val notification = utils.buildDynamicNotification(
                ID_BG_SERVICE, Notifications.NOTIFICATION_CHANNEL_LOW
            )
            val sendString = context.getString(R.string.butn_send)
            val receiveString = context.getString(R.string.butn_receive)
            val sendIntent: PendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, ContentSharingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                0
            )
            val receiveIntent: PendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, AddDeviceActivity::class.java)
                    .putExtra(AddDeviceActivity.EXTRA_CONNECTION_MODE, AddDeviceActivity.ConnectionMode.WaitForRequests)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                0
            )
            notification.setSmallIcon(R.drawable.ic_trebleshot_rounded_white_24dp_static)
                .setContentTitle(context.getString(R.string.text_communicationServiceRunning))
                .setContentText(context.getString(R.string.text_notificationOpenHome))
                .setContentIntent(generateHomePendingIntent())
                .addAction(generateExitNotificationAction())
                .addAction(R.drawable.ic_arrow_up_white_24dp_static, sendString, sendIntent)
                .addAction(R.drawable.ic_arrow_down_white_24dp_static, receiveString, receiveIntent)
            return notification.show()
        }

    fun generateHomePendingIntent(): PendingIntent {
        return PendingIntent.getActivity(context, 0, Intent(context, HomeActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
    }

    fun generateStopAllTasksAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.ic_close_white_24dp_static, context.getString(
                R.string.butn_stopAll
            ), PendingIntent.getService(
                context, AppUtils.uniqueNumber,
                Intent(context, BackgroundService::class.java)
                    .setAction(BackgroundService.ACTION_STOP_ALL_TASKS), 0
            )
        )
    }

    fun generateExitNotificationAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.ic_close_white_24dp_static, context.getString(
                R.string.butn_exit
            ), PendingIntent.getService(
                context, AppUtils.uniqueNumber,
                Intent(context, BackgroundService::class.java)
                    .setAction(BackgroundService.ACTION_END_SESSION), 0
            )
        )
    }

    val context: Context
        get() = utils.context

    fun notifyKeyChanged(device: Device, receiveKey: Int, sendKey: Int) {
        val notification = utils.buildDynamicNotification(
            AppUtils.uniqueNumber,
            Notifications.NOTIFICATION_CHANNEL_HIGH
        )
        val acceptIntent = Intent(context, BackgroundService::class.java)
        val dialogIntent = Intent(context, DialogEventReceiver::class.java)
        acceptIntent.setAction(BackgroundService.ACTION_DEVICE_KEY_CHANGE_APPROVAL)
            .putExtra(BackgroundService.EXTRA_DEVICE, device)
            .putExtra(Notifications.EXTRA_NOTIFICATION_ID, notification.notificationId)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, true)
            .putExtra(BackgroundService.EXTRA_RECEIVE_KEY, receiveKey)
            .putExtra(BackgroundService.EXTRA_SEND_KEY, sendKey)
        val rejectIntent = (acceptIntent.clone() as Intent)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, false)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.uniqueNumber, acceptIntent,
            0
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.uniqueNumber, rejectIntent,
            0
        )
        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
            .setContentTitle(context.getString(R.string.text_deviceKeyChanged))
            .setContentText(context.getString(R.string.ques_acceptNewDeviceKey, device.username))
            .setContentInfo(device.username)
            .setContentIntent(
                PendingIntent.getBroadcast(
                    context, AppUtils.uniqueNumber, dialogIntent,
                    0
                )
            )
            .setDefaults(utils.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(R.drawable.ic_check_white_24dp_static, context.getString(R.string.butn_accept), positiveIntent)
            .addAction(R.drawable.ic_close_white_24dp_static, context.getString(R.string.butn_reject), negativeIntent)
            .setTicker(context.getString(R.string.text_connectionPermission))
        notification.show()
    }

    fun notifyTransferRequest(
        device: Device, transfer: Transfer, acceptIntent: Intent, rejectIntent: Intent,
        transferDetail: Intent?, message: String?,
    ) {
        val notification = utils.buildDynamicNotification(
            Transfers.createUniqueTransferId(transfer.id, device.uid, TransferItem.Type.INCOMING),
            Notifications.NOTIFICATION_CHANNEL_HIGH
        )
        acceptIntent.putExtra(Notifications.EXTRA_NOTIFICATION_ID, notification.notificationId)
        rejectIntent.putExtra(Notifications.EXTRA_NOTIFICATION_ID, notification.notificationId)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.uniqueNumber, acceptIntent,
            0
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.uniqueNumber, rejectIntent,
            0
        )
        notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.ques_receiveFile))
            .setContentText(message)
            .setContentInfo(device.username)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, AppUtils.uniqueNumber, transferDetail,
                    0
                )
            )
            .setDefaults(utils.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(
                R.drawable.ic_check_white_24dp_static,
                context.getString(R.string.butn_receive),
                positiveIntent
            )
            .addAction(R.drawable.ic_close_white_24dp_static, context.getString(R.string.butn_reject), negativeIntent)
            .setTicker(context.getString(R.string.ques_receiveFile)).priority = NotificationCompat.PRIORITY_HIGH
        notification.show()
    }

    fun notifyClipboardRequest(device: Device, item: SharedTextModel) {
        val notification = utils.buildDynamicNotification(item.id, Notifications.NOTIFICATION_CHANNEL_HIGH)
        val acceptIntent: Intent = Intent(context, BackgroundService::class.java)
            .setAction(BackgroundService.ACTION_CLIPBOARD)
            .putExtra(BackgroundService.EXTRA_TEXT_MODEL, item.id)
            .putExtra(Notifications.EXTRA_NOTIFICATION_ID, notification.notificationId)
        val activityIntent = Intent(context, TextEditorActivity::class.java)
        val rejectIntent = acceptIntent.clone() as Intent
        acceptIntent.putExtra(BackgroundService.EXTRA_TEXT_ACCEPTED, true)
        rejectIntent.putExtra(BackgroundService.EXTRA_TEXT_ACCEPTED, false)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.uniqueNumber, acceptIntent,
            0
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.uniqueNumber, rejectIntent,
            0
        )
        activityIntent
            .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
            .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, item)
            .flags = Intent.FLAG_ACTIVITY_NEW_TASK
        notification
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.ques_copyToClipboard))
            .setContentText(context.getString(R.string.text_textReceived))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(item.text)
                    .setBigContentTitle(context.getString(R.string.ques_copyToClipboard))
            )
            .setContentInfo(device.username)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, AppUtils.uniqueNumber, activityIntent,
                    0
                )
            )
            .setDefaults(utils.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(
                R.drawable.ic_check_white_24dp_static, context.getString(android.R.string.copy),
                positiveIntent
            )
            .addAction(
                R.drawable.ic_close_white_24dp_static, context.getString(android.R.string.no),
                negativeIntent
            )
            .setTicker(context.getString(R.string.text_receivedTextSummary)).priority =
            NotificationCompat.PRIORITY_HIGH
        notification.show()
    }

    fun notifyFileReceived(task: FileTransferTask, savePath: DocumentFile) {
        val file = task.file ?: return
        val lastItem = task.lastItem ?: return

        val notification = utils.buildDynamicNotification(
            Transfers.createUniqueTransferId(task.transfer.id, task.device.uid, task.type),
            Notifications.NOTIFICATION_CHANNEL_HIGH
        )
        notification
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentInfo(task.device.username)
            .setAutoCancel(true)
            .setDefaults(utils.notificationSettings)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentText(
                context.getString(
                    R.string.text_receivedTransfer,
                    Files.formatLength(task.completedBytes, false),
                    TimeUtils.getFriendlyElapsedTime(
                        context, System.currentTimeMillis() - task.startTime
                    )
                )
            )
        if (task.completedCount == 1) {
            try {
                val openIntent = Files.getOpenIntent(context, file)
                notification.setContentIntent(
                    PendingIntent.getActivity(
                        context, AppUtils.uniqueNumber, openIntent, 0
                    )
                )
            } catch (ignored: Exception) {
            }
            notification
                .setContentTitle(lastItem.name)
                .addAction(
                    R.drawable.ic_folder_white_24dp_static, context.getString(R.string.butn_showFiles),
                    PendingIntent.getActivity(
                        context, AppUtils.uniqueNumber,
                        Intent(context, FileExplorerActivity::class.java)
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0
                    )
                )
        } else {
            notification
                .setContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.text_fileReceiveCompletedSummary, task.completedCount,
                        task.completedCount
                    )
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, AppUtils.uniqueNumber,
                        Intent(context, FileExplorerActivity::class.java)
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0
                    )
                )
        }
        notification.show()
    }

    fun notifyTasksNotification(
        taskList: List<AsyncTask>,
        notification: DynamicNotification?,
    ): DynamicNotification {
        var notificationLocal = notification ?: utils.buildDynamicNotification(
            ID_BG_SERVICE,
            Notifications.NOTIFICATION_CHANNEL_LOW
        ).also {
            val transfersString = context.getString(R.string.butn_transfers)
            val transfersIntent: PendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, TransferHistoryActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0
            )

            it.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                .setContentTitle(context.getString(R.string.text_taskOngoing))
                .setContentIntent(generateHomePendingIntent())
                .setOngoing(true)
                .addAction(generateStopAllTasksAction())
                .addAction(R.drawable.ic_swap_vert_white_24dp_static, transfersString, transfersIntent)
        }

        val msg = SpannableStringBuilder()
        for (task in taskList) {
            task.onPublishStatus()
            val content = task.ongoingContent
            val middleDot = " " + context.getString(R.string.mode_middleDot) + " "
            val taskName = task.getName(context)
            val progressCurrent = task.progress.getProgress()
            val progressTotal = task.progress.getTotal()
            if (msg.isNotEmpty()) msg.append("\n")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) msg.append(
                taskName,
                StyleSpan(Typeface.BOLD),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) else msg.append(taskName)
            if (progressCurrent > 0 && progressTotal > 0) {
                msg.append(middleDot)
                val percentage = percentFormat.format(progressCurrent.toDouble() / progressTotal)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) msg.append(
                    percentage,
                    StyleSpan(Typeface.ITALIC),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                ) else msg.append(percentage)
            }
            if (content != null && content.isNotEmpty()) msg.append(middleDot).append(content)
            if (msg.isEmpty()) msg.append(context.getString(R.string.text_empty))
        }
        val summary = context.resources.getQuantityString(
            R.plurals.text_tasks, taskList.size,
            taskList.size
        )
        val textStyle: NotificationCompat.BigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(context.getString(R.string.text_taskOngoing))
            .setSummaryText(summary)
            .bigText(msg)
        notificationLocal.setContentText(summary)
            .setStyle(textStyle)
        return notificationLocal.show()
    }

    fun showToast(toastTextRes: Int) {
        Toast.makeText(context, toastTextRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ID_BG_SERVICE = 1
    }
}