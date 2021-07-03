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
import com.genonbeta.android.framework.io.DocumentFile
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.*
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.database.model.Transfer
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.receiver.BgBroadcastReceiver
import org.monora.uprotocol.client.android.receiver.BgBroadcastReceiver.Companion.ACTION_STOP_ALL_TASKS
import org.monora.uprotocol.client.android.service.BackgroundService
import org.monora.uprotocol.client.android.service.BackgroundService.Companion.ACTION_STOP_ALL
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.task.FileTransferTask
import org.monora.uprotocol.core.transfer.TransferItem
import java.text.NumberFormat

/**
 * created by: Veli
 * date: 26.01.2018 18:29
 */
class Notifications(val backend: NotificationBackend) {
    val context: Context
        get() = backend.context

    private val percentFormat = NumberFormat.getPercentInstance()

    val foregroundNotification: DynamicNotification by lazy {
        val notification = backend.buildDynamicNotification(
            ID_BG_SERVICE, NotificationBackend.NOTIFICATION_CHANNEL_LOW
        )
        val sendString = context.getString(R.string.butn_send)
        val receiveString = context.getString(R.string.butn_receive)
        val sendIntent: PendingIntent = PendingIntent.getActivity(
            context,
            ID_BG_SERVICE + 1,
            Intent(context, ContentSharingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val receiveIntent: PendingIntent = PendingIntent.getActivity(
            context,
            ID_BG_SERVICE + 2,
            Intent(context, ReceiveActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        val exitAction = NotificationCompat.Action(
            R.drawable.ic_close_white_24dp_static, context.getString(R.string.butn_exit),
            PendingIntent.getService(
                context,
                ID_BG_SERVICE + 3,
                Intent(context, BackgroundService::class.java).setAction(ACTION_STOP_ALL),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        )
        val homeIntent = PendingIntent.getActivity(
            context,
            ID_BG_SERVICE + 4,
            Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT
        )
        notification.setSmallIcon(R.drawable.ic_trebleshot_rounded_white_24dp_static)
            .setContentTitle(context.getString(R.string.text_communicationServiceRunning))
            .setContentText(context.getString(R.string.text_notificationOpenHome))
            .setContentIntent(homeIntent)
            .addAction(exitAction)
            .addAction(R.drawable.ic_arrow_up_white_24dp_static, sendString, sendIntent)
            .addAction(R.drawable.ic_arrow_down_white_24dp_static, receiveString, receiveIntent)

        notification.show()
    }

    fun notifyKeyChanged(client: UClient) {
        val uidHash = client.clientUid.hashCode()
        val notification = backend.buildDynamicNotification(uidHash, NotificationBackend.NOTIFICATION_CHANNEL_HIGH)
        val acceptIntent = Intent(context, BgBroadcastReceiver::class.java)
        acceptIntent.setAction(BgBroadcastReceiver.ACTION_DEVICE_KEY_CHANGE_APPROVAL)
            .putExtra(BgBroadcastReceiver.EXTRA_DEVICE, client)
            .putExtra(NotificationBackend.EXTRA_NOTIFICATION_ID, notification.notificationId)
            .putExtra(BgBroadcastReceiver.EXTRA_ACCEPTED, true)
        val rejectIntent = (acceptIntent.clone() as Intent)
            .putExtra(BgBroadcastReceiver.EXTRA_ACCEPTED, false)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, uidHash + REQUEST_CODE_ACCEPT, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, uidHash + REQUEST_CODE_ACCEPT, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
            .setContentTitle(context.getString(R.string.text_deviceKeyChanged))
            .setContentText(context.getString(R.string.ques_acceptNewDeviceKey, client.clientNickname))
            .setContentInfo(client.clientNickname)
            .setContentIntent(
                PendingIntent.getBroadcast(
                    context,
                    uidHash + REQUEST_CODE_NEUTRAL,
                    Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setDefaults(backend.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(R.drawable.ic_check_white_24dp_static, context.getString(R.string.butn_accept), positiveIntent)
            .addAction(R.drawable.ic_close_white_24dp_static, context.getString(R.string.butn_reject), negativeIntent)
            .setTicker(context.getString(R.string.text_connectionPermission))
        notification.show()
    }

    fun notifyTransferRequest(
        client: UClient, transfer: Transfer, acceptIntent: Intent, rejectIntent: Intent,
        transferDetail: Intent?, message: String?,
    ) {
        val hash = Transfers.createUniqueTransferId(transfer.id, client.uid, TransferItem.Type.Incoming)
        val notification = backend.buildDynamicNotification(hash, NotificationBackend.NOTIFICATION_CHANNEL_HIGH)
        acceptIntent.putExtra(NotificationBackend.EXTRA_NOTIFICATION_ID, notification.notificationId)
        rejectIntent.putExtra(NotificationBackend.EXTRA_NOTIFICATION_ID, notification.notificationId)
        val positiveIntent: PendingIntent = PendingIntent.getService(
            context, hash + REQUEST_CODE_ACCEPT, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val negativeIntent: PendingIntent = PendingIntent.getService(
            context, hash + REQUEST_CODE_REJECT, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(context.getString(R.string.ques_receiveFile))
            .setContentText(message)
            .setContentInfo(client.clientNickname)
            .setContentIntent(
                PendingIntent.getActivity(
                    context, hash + REQUEST_CODE_NEUTRAL, transferDetail, PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setDefaults(backend.notificationSettings)
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

    fun notifyClipboardRequest(client: UClient, item: SharedText) {
        val notification = backend.buildDynamicNotification(item.id, NotificationBackend.NOTIFICATION_CHANNEL_HIGH)
        val acceptIntent: Intent = Intent(context, BgBroadcastReceiver::class.java)
            .setAction(BgBroadcastReceiver.ACTION_CLIPBOARD)
            .putExtra(BgBroadcastReceiver.EXTRA_TEXT_MODEL, item)
            .putExtra(NotificationBackend.EXTRA_NOTIFICATION_ID, notification.notificationId)
        val activityIntent = Intent(context, TextEditorActivity::class.java)
        val rejectIntent = acceptIntent.clone() as Intent
        acceptIntent.putExtra(BgBroadcastReceiver.EXTRA_TEXT_ACCEPTED, true)
        rejectIntent.putExtra(BgBroadcastReceiver.EXTRA_TEXT_ACCEPTED, false)
        val positiveIntent: PendingIntent = PendingIntent.getBroadcast(
            context, item.id + REQUEST_CODE_ACCEPT, acceptIntent, PendingIntent.FLAG_UPDATE_CURRENT
        )
        val negativeIntent: PendingIntent = PendingIntent.getBroadcast(
            context, item.id + REQUEST_CODE_REJECT, rejectIntent, PendingIntent.FLAG_UPDATE_CURRENT
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
            .setContentInfo(client.clientNickname)
            .setContentIntent(
                PendingIntent.getActivity(
                    context,
                    item.id + REQUEST_CODE_NEUTRAL,
                    activityIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            .setDefaults(backend.notificationSettings)
            .setDeleteIntent(negativeIntent)
            .addAction(
                R.drawable.ic_check_white_24dp_static, context.getString(android.R.string.copy), positiveIntent
            )
            .addAction(
                R.drawable.ic_close_white_24dp_static, context.getString(R.string.butn_no), negativeIntent
            )
            .setTicker(context.getString(R.string.text_receivedTextSummary)).priority =
            NotificationCompat.PRIORITY_HIGH
        notification.show()
    }

    fun notifyFileReceived(task: FileTransferTask, saveLocation: DocumentFile) {
        // FIXME: 2/25/21 We no longer have the file and lastItem attributes to generate a notification.
        /*
        val file = task.file ?: return
        val lastItem = task.lastItem ?: return
        val notification = utils.buildDynamicNotification(
            Transfers.createUniqueTransferId(task.transfer.id, task.client.clientUid, task.type),
            Notifications.NOTIFICATION_CHANNEL_HIGH
        )
        notification
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentInfo(task.client.clientNickname)
            .setAutoCancel(true)
            .setDefaults(utils.notificationSettings)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentText(
                context.getString(
                    R.string.text_receivedTransfer,
                    Files.formatLength(task.transferOperation.bytesTotal, false),
                    TimeUtils.getFriendlyElapsedTime(
                        context, System.currentTimeMillis() - task.startTime
                    )
                )
            )
        if (task.transferOperation.count == 1) {
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
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, saveLocation.getUri()), 0
                    )
                )
        } else {
            notification
                .setContentTitle(
                    context.resources.getQuantityString(
                        R.plurals.text_fileReceiveCompletedSummary,
                        task.transferOperation.count,
                        task.transferOperation.count
                    )
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        context, AppUtils.uniqueNumber,
                        Intent(context, FileExplorerActivity::class.java)
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, saveLocation.getUri()), 0
                    )
                )
        }
        notification.show()

         */
    }

    fun notifyTasksNotification(
        taskList: List<AsyncTask>,
        notification: DynamicNotification?,
    ): DynamicNotification {
        val notificationLocal = notification ?: backend.buildDynamicNotification(
            ID_BG_SERVICE,
            NotificationBackend.NOTIFICATION_CHANNEL_LOW
        ).also {
            val transfersString = context.getString(R.string.butn_transfers)
            val transfersIntent: PendingIntent = PendingIntent.getActivity(
                context,
                ID_BG_SERVICE + 1,
                Intent(context, TransferHistoryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val stopAllTasksAction = NotificationCompat.Action(
                R.drawable.ic_close_white_24dp_static, context.getString(R.string.butn_stopAll),
                PendingIntent.getBroadcast(
                    context,
                    ID_BG_SERVICE + 2,
                    Intent(context, BgBroadcastReceiver::class.java).setAction(ACTION_STOP_ALL_TASKS),
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
            )
            val homeIntent = PendingIntent.getActivity(
                context,
                ID_BG_SERVICE + 3,
                Intent(context, HomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT
            )

            it.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                .setContentTitle(context.getString(R.string.text_taskOngoing))
                .setContentIntent(homeIntent)
                .setOngoing(true)
                .addAction(stopAllTasksAction)
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

    fun createAddingWifiNetworkNotification(ssid: String, password: String?): DynamicNotification {
        val notification = backend.buildDynamicNotification(
            ID_ADDING_WIFI_NETWORK, NotificationBackend.CHANNEL_INSTRUCTIVE
        )

        notification
            .setSmallIcon(R.drawable.ic_help_white_24_static)
            .setContentTitle(context.getString(R.string.text_connectToWifiTitle, ssid))
            .setContentText(context.getString(R.string.text_connectToWifiPasswordInstructions, password))
            .setAutoCancel(false)
            .setOngoing(true)

        return notification
    }

    fun showToast(toastTextRes: Int) {
        Toast.makeText(context, toastTextRes, Toast.LENGTH_SHORT).show()
    }

    companion object {
        const val ID_BG_SERVICE = 1

        const val ID_ADDING_WIFI_NETWORK = 2

        const val REQUEST_CODE_ACCEPT = 1

        const val REQUEST_CODE_REJECT = 2

        const val REQUEST_CODE_NEUTRAL = 3
    }
}