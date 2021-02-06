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
package com.genonbeta.TrebleShot.util

import android.content.*
import android.os.*
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import com.genonbeta.TrebleShot.dataobject.*
import com.genonbeta.TrebleShot.service.BackgroundService
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.util.FileUtils
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 26.01.2018 18:29
 */
class NotificationHelper(val utils: NotificationUtils) {
    private val mPercentFormat = NumberFormat.getPercentInstance()
    val foregroundNotification: DynamicNotification
        get() {
            val notification = utils.buildDynamicNotification(
                ID_BG_SERVICE.toLong(),
                NotificationUtils.Companion.NOTIFICATION_CHANNEL_LOW
            )
            val sendString = context!!.getString(R.string.butn_send)
            val receiveString = context!!.getString(R.string.butn_receive)
            val sendIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, Intent(
                    context,
                    ContentSharingActivity::class.java
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0
            )
            val receiveIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, Intent(
                    context,
                    AddDeviceActivity::class.java
                ).putExtra(
                    AddDeviceActivity.Companion.EXTRA_CONNECTION_MODE,
                    ConnectionMode.WaitForRequests
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0
            )
            notification!!.setSmallIcon(R.drawable.ic_trebleshot_rounded_white_24dp_static)
                .setContentTitle(context!!.getString(R.string.text_communicationServiceRunning))
                .setContentText(context!!.getString(R.string.text_notificationOpenHome))
                .setContentIntent(generateHomePendingIntent())
                .addAction(generateExitNotificationAction())
                .addAction(R.drawable.ic_arrow_up_white_24dp_static, sendString, sendIntent)
                .addAction(R.drawable.ic_arrow_down_white_24dp_static, receiveString, receiveIntent)
            return notification.show()
        }

    fun generateHomePendingIntent(): PendingIntent {
        return PendingIntent.getActivity(
            context, 0, Intent(
                context,
                HomeActivity::class.java
            ).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0
        )
    }

    fun generateStopAllTasksAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.ic_close_white_24dp_static, context!!.getString(
                R.string.butn_stopAll
            ), PendingIntent.getService(
                context, AppUtils.getUniqueNumber(),
                Intent(context, BackgroundService::class.java)
                    .setAction(BackgroundService.ACTION_STOP_ALL_TASKS), 0
            )
        )
    }

    fun generateExitNotificationAction(): NotificationCompat.Action {
        return NotificationCompat.Action(
            R.drawable.ic_close_white_24dp_static, context!!.getString(
                R.string.butn_exit
            ), PendingIntent.getService(
                context, AppUtils.getUniqueNumber(),
                Intent(context, BackgroundService::class.java)
                    .setAction(BackgroundService.ACTION_END_SESSION), 0
            )
        )
    }

    val context: Context?
        get() = utils.context

    fun notifyKeyChanged(device: Device, receiveKey: Int, sendKey: Int) {
        val notification = utils.buildDynamicNotification(
            AppUtils.getUniqueNumber().toLong(),
            NotificationUtils.Companion.NOTIFICATION_CHANNEL_HIGH
        )
        val acceptIntent = Intent(context, BackgroundService::class.java)
        val dialogIntent = Intent(context, DialogEventReceiver::class.java)
        acceptIntent.setAction(BackgroundService.ACTION_DEVICE_KEY_CHANGE_APPROVAL)
            .putExtra(BackgroundService.EXTRA_DEVICE, device)
            .putExtra(NotificationUtils.Companion.EXTRA_NOTIFICATION_ID, notification!!.notificationId)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, true)
            .putExtra(BackgroundService.EXTRA_RECEIVE_KEY, receiveKey)
            .putExtra(BackgroundService.EXTRA_SEND_KEY, sendKey)
        val rejectIntent = (acceptIntent.clone() as Intent)
            .putExtra(BackgroundService.EXTRA_ACCEPTED, false)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.getUniqueNumber(), acceptIntent,
            0
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.getUniqueNumber(), rejectIntent,
            0
        )
        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
            .setContentTitle(context!!.getString(R.string.text_deviceKeyChanged))
            .setContentText(context!!.getString(R.string.ques_acceptNewDeviceKey, device.username))
            .setContentInfo(device.username)
            .setContentIntent(
                PendingIntent.getBroadcast(
                    context, AppUtils.getUniqueNumber(), dialogIntent,
                    0
                )
            )
            .setDefaults(utils.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(R.drawable.ic_check_white_24dp_static, context!!.getString(R.string.butn_accept), positiveIntent)
            .addAction(R.drawable.ic_close_white_24dp_static, context!!.getString(R.string.butn_reject), negativeIntent)
            .setTicker(context!!.getString(R.string.text_connectionPermission))
        notification.show()
    }

    fun notifyTransferRequest(
        device: Device, transfer: Transfer, acceptIntent: Intent, rejectIntent: Intent,
        transferDetail: Intent?, message: String?
    ) {
        val notification = utils.buildDynamicNotification(
            Transfers.createUniqueTransferId(transfer.id, device.uid, TransferItem.Type.INCOMING),
            NotificationUtils.Companion.NOTIFICATION_CHANNEL_HIGH
        )
        acceptIntent.putExtra(NotificationUtils.Companion.EXTRA_NOTIFICATION_ID, notification!!.notificationId)
        rejectIntent.putExtra(NotificationUtils.Companion.EXTRA_NOTIFICATION_ID, notification.notificationId)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.getUniqueNumber(), acceptIntent,
            0
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.getUniqueNumber(), rejectIntent,
            0
        )
        notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context!!.getString(R.string.ques_receiveFile))
            .setContentText(message)
            .setContentInfo(device.username)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, AppUtils.getUniqueNumber(), transferDetail,
                    0
                )
            )
            .setDefaults(utils.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(
                R.drawable.ic_check_white_24dp_static,
                context!!.getString(R.string.butn_receive),
                positiveIntent
            )
            .addAction(R.drawable.ic_close_white_24dp_static, context!!.getString(R.string.butn_reject), negativeIntent)
            .setTicker(context!!.getString(R.string.ques_receiveFile)).priority = NotificationCompat.PRIORITY_HIGH
        notification.show()
    }

    fun notifyClipboardRequest(device: Device, `object`: TextStreamObject) {
        val notification = utils.buildDynamicNotification(
            `object`.id,
            NotificationUtils.Companion.NOTIFICATION_CHANNEL_HIGH
        )
        val acceptIntent: Intent = Intent(context, BackgroundService::class.java)
            .setAction(BackgroundService.ACTION_CLIPBOARD)
            .putExtra(BackgroundService.EXTRA_CLIPBOARD_ID, `object`.id)
            .putExtra(NotificationUtils.Companion.EXTRA_NOTIFICATION_ID, notification!!.notificationId)
        val activityIntent = Intent(context, TextEditorActivity::class.java)
        val rejectIntent = acceptIntent.clone() as Intent
        acceptIntent.putExtra(BackgroundService.EXTRA_CLIPBOARD_ACCEPTED, true)
        rejectIntent.putExtra(BackgroundService.EXTRA_CLIPBOARD_ACCEPTED, false)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.getUniqueNumber(), acceptIntent,
            0
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, AppUtils.getUniqueNumber(), rejectIntent,
            0
        )
        activityIntent
            .setAction(TextEditorActivity.Companion.ACTION_EDIT_TEXT)
            .putExtra(TextEditorActivity.Companion.EXTRA_CLIPBOARD_ID, `object`.id)
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        notification
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context!!.getString(R.string.ques_copyToClipboard))
            .setContentText(context!!.getString(R.string.text_textReceived))
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(`object`.text)
                    .setBigContentTitle(context!!.getString(R.string.ques_copyToClipboard))
            )
            .setContentInfo(device.username)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, AppUtils.getUniqueNumber(), activityIntent,
                    0
                )
            )
            .setDefaults(utils.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(
                R.drawable.ic_check_white_24dp_static, context!!.getString(android.R.string.copy),
                positiveIntent
            )
            .addAction(
                R.drawable.ic_close_white_24dp_static, context!!.getString(android.R.string.no),
                negativeIntent
            )
            .setTicker(context!!.getString(R.string.text_receivedTextSummary)).priority =
            NotificationCompat.PRIORITY_HIGH
        notification.show()
    }

    fun notifyFileReceived(task: FileTransferTask, savePath: DocumentFile) {
        val notification = utils.buildDynamicNotification(
            Transfers.createUniqueTransferId(
                task.transfer.id, task.device.uid, task.type
            ), NotificationUtils.Companion.NOTIFICATION_CHANNEL_HIGH
        )
        notification
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentInfo(task.device.username)
            .setAutoCancel(true)
            .setDefaults(utils.notificationSettings)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentText(
                context!!.getString(
                    R.string.text_receivedTransfer,
                    FileUtils.sizeExpression(task.completedBytes, false),
                    TimeUtils.getFriendlyElapsedTime(
                        context, System.currentTimeMillis()
                                - task.getStartTime()
                    )
                )
            )
        if (task.completedCount == 1) {
            try {
                val openIntent = FileUtils.getOpenIntent(context, task.file)
                notification!!.setContentIntent(
                    PendingIntent.getActivity(
                        context, AppUtils.getUniqueNumber(),
                        openIntent, 0
                    )
                )
            } catch (ignored: Exception) {
            }
            notification
                .setContentTitle(task.lastItem.name)
                .addAction(
                    R.drawable.ic_folder_white_24dp_static, context!!.getString(R.string.butn_showFiles),
                    PendingIntent.getActivity(
                        context, AppUtils.getUniqueNumber(), Intent(
                            context,
                            FileExplorerActivity::class.java
                        )
                            .putExtra(FileExplorerActivity.Companion.EXTRA_FILE_PATH, savePath.uri), 0
                    )
                )
        } else {
            notification
                .setContentTitle(
                    context!!.resources.getQuantityString(
                        R.plurals.text_fileReceiveCompletedSummary, task.completedCount,
                        task.completedCount
                    )
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, AppUtils.getUniqueNumber(), Intent(
                            context, FileExplorerActivity::class.java
                        )
                            .putExtra(FileExplorerActivity.Companion.EXTRA_FILE_PATH, savePath.uri), 0
                    )
                )
        }
        notification!!.show()
    }

    fun notifyTasksNotification(
        taskList: List<AsyncTask>,
        notification: DynamicNotification?
    ): DynamicNotification {
        var notification = notification
        if (notification == null) {
            notification = utils.buildDynamicNotification(
                ID_BG_SERVICE.toLong(),
                NotificationUtils.Companion.NOTIFICATION_CHANNEL_LOW
            )
            val transfersString = context!!.getString(R.string.butn_transfers)
            val transfersIntent: PendingIntent = PendingIntent.getActivity(
                context, 0,
                Intent(context, TransferHistoryActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0
            )
            notification.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                .setContentTitle(context!!.getString(R.string.text_taskOngoing))
                .setContentIntent(generateHomePendingIntent())
                .setOngoing(true)
                .addAction(generateStopAllTasksAction())
                .addAction(R.drawable.ic_swap_vert_white_24dp_static, transfersString, transfersIntent)
        }
        val msg = SpannableStringBuilder()
        for (task in taskList) {
            task.onPublishStatus()
            val content = task.ongoingContent
            val middleDot = " " + context!!.getString(R.string.mode_middleDot) + " "
            val taskName = task.getName(context)
            val progressCurrent = task.progress().current
            val progressTotal = task.progress().total
            if (msg.length > 0) msg.append("\n")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) msg.append(
                taskName,
                StyleSpan(Typeface.BOLD),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            ) else msg.append(taskName)
            if (progressCurrent > 0 && progressTotal > 0) {
                msg.append(middleDot)
                val percentage = mPercentFormat.format(progressCurrent.toDouble() / progressTotal)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) msg.append(
                    percentage,
                    StyleSpan(Typeface.ITALIC),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                ) else msg.append(percentage)
            }
            if (content != null && content.length > 0) msg.append(middleDot)
                .append(content)
            if (msg.length < 1) msg.append(context!!.getString(R.string.text_empty))
        }
        val summary = context!!.resources.getQuantityString(
            R.plurals.text_tasks, taskList.size,
            taskList.size
        )
        val textStyle: NotificationCompat.BigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(context!!.getString(R.string.text_taskOngoing))
            .setSummaryText(summary)
            .bigText(msg)
        notification.setContentText(summary)
            .setStyle(textStyle)
        return notification.show()
    }

    fun showToast(toastTextRes: Int) {
        Toast.makeText(context, toastTextRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ID_BG_SERVICE = 1
    }
}