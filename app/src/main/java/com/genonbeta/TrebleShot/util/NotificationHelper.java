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
import androidx.core.app.Person;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.FileExplorerActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.activity.TransferDetailActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.object.Transfer;
import com.genonbeta.TrebleShot.object.TransferItem;
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

    public static final int ID_ONGOING_TASKS = 2;

    private final NotificationUtils mNotificationUtils;

    private final Person mMe;

    private final NumberFormat mPercentFormat = NumberFormat.getPercentInstance();

    public NotificationHelper(NotificationUtils notificationUtils)
    {
        mNotificationUtils = notificationUtils;
        mMe = new Person.Builder()
                .setName(AppUtils.getLocalDeviceName(getContext()))
                .setKey(AppUtils.getDeviceId(getContext()))
                .build();
    }

    public DynamicNotification getForegroundNotification()
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(ID_BG_SERVICE,
                NotificationUtils.NOTIFICATION_CHANNEL_LOW);

        notification.setSmallIcon(R.drawable.ic_trebleshot_rounded_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_communicationServiceRunning))
                .setContentText(getContext().getString(R.string.text_communicationServiceStop))
                .setAutoCancel(true)
                .setContentIntent(PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), new Intent(
                        getContext(), BackgroundService.class)
                        .setAction(BackgroundService.ACTION_END_SESSION), 0));

        return notification.show();
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

    public void notifyFileTransfer(FileTransferTask task) throws Exception
    {
        DynamicNotification notification = task.getCustomNotification();

        if (notification == null) {
            boolean isIncoming = TransferItem.Type.INCOMING.equals(task.object.type);
            notification = getUtils().buildDynamicNotification(Transfers.createUniqueTransferId(
                    task.transfer.id, task.device.uid, task.object.type), NotificationUtils.NOTIFICATION_CHANNEL_LOW);

            task.setCustomNotification(notification);

            Intent cancelIntent = new Intent(getContext(), BackgroundService.class)
                    .setAction(BackgroundService.ACTION_STOP_TASK)
                    .putExtra(BackgroundService.EXTRA_TRANSFER_ITEM_ID, task.object.id)
                    .putExtra(BackgroundService.EXTRA_TRANSFER, task.transfer)
                    .putExtra(BackgroundService.EXTRA_DEVICE, task.device)
                    .putExtra(BackgroundService.EXTRA_TRANSFER_TYPE, task.type)
                    .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

            notification.setSmallIcon(isIncoming ? android.R.drawable.stat_sys_download
                    : android.R.drawable.stat_sys_upload)
                    .setContentText(getContext().getString(isIncoming ? R.string.text_receiving : R.string.text_sending))
                    .setContentInfo(task.device.username)
                    .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(
                            getContext(), TransferDetailActivity.class)
                            .setAction(TransferDetailActivity.ACTION_LIST_TRANSFERS)
                            .putExtra(TransferDetailActivity.EXTRA_TRANSFER, task.transfer), 0))
                    .setOngoing(true)
                    .setWhen(task.timeStarted)
                    .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(isIncoming
                            ? R.string.butn_cancelReceiving : R.string.butn_cancelSending), PendingIntent.getService(getContext(),
                            AppUtils.getUniqueNumber(), cancelIntent, 0));
        }

        notification.setContentTitle(task.object.name);
        notification.setProgress(100, (int) (Math.max(0.01, task.completedBytes + task.currentBytes)
                / Math.max(0.01, task.index.bytesPending()) * 100), false);

        long bytesTransferred = task.completedBytes + task.currentBytes;

        if (task.lastKnownBytes > 0 && bytesTransferred > 0) {
            long change = bytesTransferred - task.lastKnownBytes;
            StringBuilder text = new StringBuilder();

            text.append(FileUtils.sizeExpression(change, false));
            text.append("/s");

            if (task.index.bytesPending() > 0 && change > 0) {
                long timeNeeded = (task.index.bytesPending() - bytesTransferred) / change;

                text.append(" (");
                text.append(getContext().getString(R.string.text_remainingTime,
                        TimeUtils.getDuration(timeNeeded, false)));
                text.append(")");
            }

            notification.setSubText(text.toString());
        }

        task.lastKnownBytes = bytesTransferred;

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
                                - task.timeStarted)));

        if (task.completedCount != 1) {
            notification
                    .setContentTitle(getContext().getResources().getQuantityString(
                            R.plurals.text_fileReceiveCompletedSummary, task.completedCount,
                            task.completedCount))
                    .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(
                            getContext(), FileExplorerActivity.class)
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        } else {
            try {
                Intent openIntent = FileUtils.getOpenIntent(getContext(), task.currentFile);
                notification.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(),
                        openIntent, 0));
            } catch (Exception e) {
                // do nothing
            }

            notification
                    .setContentTitle(task.object.name)
                    .addAction(R.drawable.ic_folder_white_24dp_static, getContext().getString(R.string.butn_showFiles),
                            PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(),
                                    FileExplorerActivity.class)
                                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        }

        notification.show();
    }

    public void notifyReceiveError(FileTransferTask task)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(Transfers.createUniqueTransferId(
                task.transfer.id, task.device.uid, task.object.type), NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_error))
                .setContentText(getContext().getString(R.string.mesg_fileReceiveFilesLeftError))
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(
                        getContext(), TransferDetailActivity.class)
                        .setAction(TransferDetailActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(TransferDetailActivity.EXTRA_TRANSFER, task.transfer), 0));

        notification.show();
    }

    public void notifyReceiveError(Device device, Transfer transfer, TransferItem transferItem)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(transferItem.getId(),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_error))
                .setContentText(getContext().getString(R.string.mesg_fileReceiveError, transferItem.name))
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(
                        getContext(), TransferDetailActivity.class)
                        .setAction(TransferDetailActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(TransferDetailActivity.EXTRA_TRANSFER, transfer)
                        .putExtra(TransferDetailActivity.EXTRA_TRANSFER_ITEM_ID, transferItem.id)
                        .putExtra(TransferDetailActivity.EXTRA_TRANSFER_TYPE, transferItem.type)
                        .putExtra(TransferDetailActivity.EXTRA_DEVICE, device), 0));

        notification.show();
    }

    public void notifyConnectionError(FileTransferTask task, @Nullable String errorKey)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(Transfers.createUniqueTransferId(
                task.transfer.id, task.device.uid, task.type), NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
        // TODO: 8/11/20 The msg "hey" should be removed.
        String errorMsg = getContext().getString(R.string.mesg_deviceConnectionError, task.device.username, "hey");

        if (errorKey != null)
            switch (errorKey) {
                case Keyword.ERROR_NOT_ALLOWED:
                case Keyword.ERROR_NOT_TRUSTED:
                    errorMsg = getContext().getString(R.string.mesg_notAllowed);
                    break;
                case Keyword.ERROR_NOT_FOUND:
                    errorMsg = getContext().getString(R.string.mesg_notValidTransfer);
            }

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_error))
                .setContentText(errorMsg)
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(
                        getContext(), TransferDetailActivity.class)
                        .setAction(TransferDetailActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(TransferDetailActivity.EXTRA_TRANSFER, task.transfer), 0));

        notification.show();
    }

    public DynamicNotification notifyTasksNotification(List<AsyncTask> taskList,
                                                       @Nullable DynamicNotification notification)
    {
        if (notification == null) {
            notification = getUtils().buildDynamicNotification(ID_ONGOING_TASKS,
                    NotificationUtils.NOTIFICATION_CHANNEL_LOW);

            notification.setSmallIcon(R.drawable.ic_compare_arrows_white_24dp_static)
                    .setContentTitle(getContext().getString(R.string.text_taskOngoing))
                    .setOngoing(true);
        }

        SpannableStringBuilder msg = new SpannableStringBuilder();
        for (AsyncTask task : taskList) {
            String content = task.getCurrentContent();
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

            msg.append(middleDot);

            if (progressCurrent > 0 && progressTotal > 0) {
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
