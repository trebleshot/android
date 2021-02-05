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

package com.genonbeta.TrebleShot.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.StyleSpan;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.*;
import com.genonbeta.TrebleShot.dataobject.Device;
import com.genonbeta.TrebleShot.dataobject.TextStreamObject;
import com.genonbeta.TrebleShot.dataobject.Transfer;
import com.genonbeta.TrebleShot.dataobject.TransferItem;
import com.genonbeta.TrebleShot.receiver.DialogEventReceiver;
import com.genonbeta.TrebleShot.service.BackgroundService;
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.android.framework.io.DocumentFile;

import java.text.NumberFormat;
import java.util.List;

/**
 * created by: Veli
 * date: 26.01.2018 18:29
 */

public class NotificationHelper
{
    public static final int ID_BG_SERVICE = 1;

    private final NotificationUtils mNotificationUtils;

    private final NumberFormat mPercentFormat = NumberFormat.getPercentInstance();

    public NotificationHelper(NotificationUtils notificationUtils)
    {
        mNotificationUtils = notificationUtils;
    }

    public DynamicNotification getForegroundNotification()
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(ID_BG_SERVICE,
                NotificationUtils.NOTIFICATION_CHANNEL_LOW);

        String sendString = getContext().getString(R.string.butn_send);
        String receiveString = getContext().getString(R.string.butn_receive);

        PendingIntent sendIntent = PendingIntent.getActivity(getContext(), 0, new Intent(getContext(),
                ContentSharingActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);
        PendingIntent receiveIntent = PendingIntent.getActivity(getContext(), 0, new Intent(getContext(),
                AddDeviceActivity.class).putExtra(AddDeviceActivity.EXTRA_CONNECTION_MODE,
                AddDeviceActivity.ConnectionMode.WaitForRequests).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);

        notification.setSmallIcon(R.drawable.ic_trebleshot_rounded_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_communicationServiceRunning))
                .setContentText(getContext().getString(R.string.text_notificationOpenHome))
                .setContentIntent(generateHomePendingIntent())
                .addAction(generateExitNotificationAction())
                .addAction(R.drawable.ic_arrow_up_white_24dp_static, sendString, sendIntent)
                .addAction(R.drawable.ic_arrow_down_white_24dp_static, receiveString, receiveIntent);

        return notification.show();
    }

    public PendingIntent generateHomePendingIntent()
    {
        return PendingIntent.getActivity(getContext(), 0, new Intent(getContext(),
                HomeActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
    }

    public NotificationCompat.Action generateStopAllTasksAction()
    {
        return new NotificationCompat.Action(R.drawable.ic_close_white_24dp_static, getContext().getString(
                R.string.butn_stopAll), PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(),
                new Intent(getContext(), BackgroundService.class)
                        .setAction(BackgroundService.ACTION_STOP_ALL_TASKS), 0));
    }

    public NotificationCompat.Action generateExitNotificationAction()
    {
        return new NotificationCompat.Action(R.drawable.ic_close_white_24dp_static, getContext().getString(
                R.string.butn_exit), PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(),
                new Intent(getContext(), BackgroundService.class)
                        .setAction(BackgroundService.ACTION_END_SESSION), 0));
    }

    public Context getContext()
    {
        return getUtils().getContext();
    }

    public NotificationUtils getUtils()
    {
        return mNotificationUtils;
    }

    public void notifyKeyChanged(Device device, int receiveKey, int sendKey)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(AppUtils.getUniqueNumber(),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        Intent acceptIntent = new Intent(getContext(), BackgroundService.class);
        Intent dialogIntent = new Intent(getContext(), DialogEventReceiver.class);

        acceptIntent.setAction(BackgroundService.ACTION_DEVICE_KEY_CHANGE_APPROVAL)
                .putExtra(BackgroundService.EXTRA_DEVICE, device)
                .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId())
                .putExtra(BackgroundService.EXTRA_ACCEPTED, true)
                .putExtra(BackgroundService.EXTRA_RECEIVE_KEY, receiveKey)
                .putExtra(BackgroundService.EXTRA_SEND_KEY, sendKey);

        Intent rejectIntent = ((Intent) acceptIntent.clone())
                .putExtra(BackgroundService.EXTRA_ACCEPTED, false);

        PendingIntent positiveIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), acceptIntent,
                0);
        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), rejectIntent,
                0);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_deviceKeyChanged))
                .setContentText(getContext().getString(R.string.ques_acceptNewDeviceKey, device.username))
                .setContentInfo(device.username)
                .setContentIntent(PendingIntent.getBroadcast(getContext(), AppUtils.getUniqueNumber(), dialogIntent,
                        0))
                .setDefaults(getUtils().getNotificationSettings())
                .setDeleteIntent(negativeIntent)
                .addAction(R.drawable.ic_check_white_24dp_static, getContext().getString(R.string.butn_accept), positiveIntent)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_reject), negativeIntent)
                .setTicker(getContext().getString(R.string.text_connectionPermission));

        notification.show();
    }

    public void notifyTransferRequest(Device device, Transfer transfer, Intent acceptIntent, Intent rejectIntent,
                                      Intent transferDetail, String message)
    {

        DynamicNotification notification = getUtils().buildDynamicNotification(
                Transfers.createUniqueTransferId(transfer.id, device.uid, TransferItem.Type.INCOMING),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        acceptIntent.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());
        rejectIntent.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

        PendingIntent positiveIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), acceptIntent,
                0);
        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), rejectIntent,
                0);

        notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(getContext().getString(R.string.ques_receiveFile))
                .setContentText(message)
                .setContentInfo(device.username)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), transferDetail,
                        0))
                .setDefaults(getUtils().getNotificationSettings())
                .setDeleteIntent(negativeIntent)
                .addAction(R.drawable.ic_check_white_24dp_static, getContext().getString(R.string.butn_receive), positiveIntent)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_reject), negativeIntent)
                .setTicker(getContext().getString(R.string.ques_receiveFile))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notification.show();
    }

    public void notifyClipboardRequest(Device device, TextStreamObject object)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(object.id,
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        Intent acceptIntent = new Intent(getContext(), BackgroundService.class)
                .setAction(BackgroundService.ACTION_CLIPBOARD)
                .putExtra(BackgroundService.EXTRA_CLIPBOARD_ID, object.id)
                .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

        Intent activityIntent = new Intent(getContext(), TextEditorActivity.class);

        Intent rejectIntent = ((Intent) acceptIntent.clone());

        acceptIntent.putExtra(BackgroundService.EXTRA_CLIPBOARD_ACCEPTED, true);
        rejectIntent.putExtra(BackgroundService.EXTRA_CLIPBOARD_ACCEPTED, false);

        PendingIntent positiveIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), acceptIntent,
                0);
        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), rejectIntent,
                0);

        activityIntent
                .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                .putExtra(TextEditorActivity.EXTRA_CLIPBOARD_ID, object.id)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        notification
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(getContext().getString(R.string.ques_copyToClipboard))
                .setContentText(getContext().getString(R.string.text_textReceived))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(object.text)
                        .setBigContentTitle(getContext().getString(R.string.ques_copyToClipboard)))
                .setContentInfo(device.username)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), activityIntent,
                        0))
                .setDefaults(getUtils().getNotificationSettings())
                .setDeleteIntent(negativeIntent)
                .addAction(R.drawable.ic_check_white_24dp_static, getContext().getString(android.R.string.copy),
                        positiveIntent)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(android.R.string.no),
                        negativeIntent)
                .setTicker(getContext().getString(R.string.text_receivedTextSummary))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notification.show();
    }

    public void notifyFileReceived(FileTransferTask task, DocumentFile savePath)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(Transfers.createUniqueTransferId(
                task.transfer.id, task.device.uid, task.type), NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentInfo(task.device.username)
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(getContext().getString(R.string.text_receivedTransfer,
                        FileUtils.sizeExpression(task.completedBytes, false),
                        TimeUtils.getFriendlyElapsedTime(getContext(), System.currentTimeMillis()
                                - task.getStartTime())));

        if (task.completedCount == 1) {
            try {
                Intent openIntent = FileUtils.getOpenIntent(getContext(), task.file);
                notification.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(),
                        openIntent, 0));
            } catch (Exception ignored) {
            }

            notification
                    .setContentTitle(task.lastItem.name)
                    .addAction(R.drawable.ic_folder_white_24dp_static, getContext().getString(R.string.butn_showFiles),
                            PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(),
                                    FileExplorerActivity.class)
                                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        } else {
            notification
                    .setContentTitle(getContext().getResources().getQuantityString(
                            R.plurals.text_fileReceiveCompletedSummary, task.completedCount,
                            task.completedCount))
                    .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(
                            getContext(), FileExplorerActivity.class)
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        }

        notification.show();
    }

    public DynamicNotification notifyTasksNotification(List<AsyncTask> taskList,
                                                       @Nullable DynamicNotification notification)
    {
        if (notification == null) {
            notification = getUtils().buildDynamicNotification(ID_BG_SERVICE,
                    NotificationUtils.NOTIFICATION_CHANNEL_LOW);

            String transfersString = getContext().getString(R.string.butn_transfers);
            PendingIntent transfersIntent = PendingIntent.getActivity(getContext(), 0,
                    new Intent(getContext(), TransferHistoryActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), 0);

            notification.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                    .setContentTitle(getContext().getString(R.string.text_taskOngoing))
                    .setContentIntent(generateHomePendingIntent())
                    .setOngoing(true)
                    .addAction(generateStopAllTasksAction())
                    .addAction(R.drawable.ic_swap_vert_white_24dp_static, transfersString, transfersIntent);
        }

        SpannableStringBuilder msg = new SpannableStringBuilder();
        for (AsyncTask task : taskList) {
            task.onPublishStatus();

            String content = task.getOngoingContent();
            String middleDot = " " + getContext().getString(R.string.mode_middleDot) + " ";
            String taskName = task.getName(getContext());
            int progressCurrent = task.progress().getCurrent();
            int progressTotal = task.progress().getTotal();

            if (msg.length() > 0)
                msg.append("\n");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                msg.append(taskName, new StyleSpan(Typeface.BOLD), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            else
                msg.append(taskName);

            if (progressCurrent > 0 && progressTotal > 0) {
                msg.append(middleDot);

                String percentage = mPercentFormat.format((double) progressCurrent / progressTotal);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    msg.append(percentage, new StyleSpan(Typeface.ITALIC), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                else
                    msg.append(percentage);
            }

            if (content != null && content.length() > 0)
                msg.append(middleDot)
                        .append(content);

            if (msg.length() < 1)
                msg.append(getContext().getString(R.string.text_empty));
        }

        String summary = getContext().getResources().getQuantityString(R.plurals.text_tasks, taskList.size(),
                taskList.size());
        NotificationCompat.BigTextStyle textStyle = new NotificationCompat.BigTextStyle()
                .setBigContentTitle(getContext().getString(R.string.text_taskOngoing))
                .setSummaryText(summary)
                .bigText(msg);
        notification.setContentText(summary)
                .setStyle(textStyle);

        return notification.show();
    }

    public void showToast(int toastTextRes)
    {
        Toast.makeText(getContext(), toastTextRes, Toast.LENGTH_SHORT).show();
    }
}
