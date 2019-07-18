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
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.FileExplorerActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.receiver.DialogEventReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.android.framework.io.DocumentFile;

import java.util.List;

/**
 * created by: Veli
 * date: 26.01.2018 18:29
 */

public class CommunicationNotificationHelper
{
    public static final int SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID = 1;

    private NotificationUtils mNotificationUtils;

    public CommunicationNotificationHelper(NotificationUtils notificationUtils)
    {
        mNotificationUtils = notificationUtils;
    }

    public DynamicNotification getCommunicationServiceNotification(boolean seamlessMode,
                                                                   boolean pinAccess,
                                                                   boolean webShare)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID, NotificationUtils.NOTIFICATION_CHANNEL_LOW);
        StringBuilder builder = new StringBuilder();

        if(webShare)
            builder.append(getContext().getString(R.string.text_webShare));

        if (builder.length() > 0)
            builder.append(" - ");

        builder.append(getContext().getString(R.string.text_communicationServiceRunning));

        notification.setSmallIcon(R.drawable.ic_trebleshot_rounded_white_24dp_static)
                .setContentTitle(builder.toString())
                .setContentText(getContext().getString(R.string.text_communicationServiceStop))
                .setAutoCancel(true)
                .addAction(R.drawable.ic_compare_arrows_white_24dp_static, getContext().getString(seamlessMode ? R.string.butn_turnTrustZoneOff : R.string.butn_turnTrustZoneOn),
                        PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), CommunicationService.class).setAction(CommunicationService.ACTION_TOGGLE_FAST_MODE), PendingIntent.FLAG_CANCEL_CURRENT))
                .setContentIntent(PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), CommunicationService.class)
                        .setAction(CommunicationService.ACTION_END_SESSION), 0));

        if (pinAccess)
            notification.addAction(R.drawable.ic_autorenew_white_24dp_static, getContext().getString(R.string.butn_revokePin),
                    PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), CommunicationService.class).setAction(CommunicationService.ACTION_REVOKE_ACCESS_PIN), PendingIntent.FLAG_CANCEL_CURRENT));

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

    public void notifyConnectionRequest(NetworkDevice device)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(AppUtils.getUniqueNumber(), NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        Intent acceptIntent = new Intent(getContext(), CommunicationService.class);
        Intent dialogIntent = new Intent(getContext(), DialogEventReceiver.class);

        acceptIntent.setAction(CommunicationService.ACTION_IP)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, device.id)
                .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId())
                .putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);

        Intent rejectIntent = ((Intent) acceptIntent.clone())
                .putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

        PendingIntent positiveIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), acceptIntent, 0);
        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), rejectIntent, 0);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_connectionPermission))
                .setContentText(getContext().getString(R.string.ques_allowDeviceToConnect))
                .setContentInfo(device.nickname)
                .setContentIntent(PendingIntent.getBroadcast(getContext(), AppUtils.getUniqueNumber(), dialogIntent, 0))
                .setDefaults(getUtils().getNotificationSettings())
                .setDeleteIntent(negativeIntent)
                .addAction(R.drawable.ic_check_white_24dp_static, getContext().getString(R.string.butn_accept), positiveIntent)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_reject), negativeIntent)
                .setTicker(getContext().getString(R.string.text_connectionPermission));

        notification.show();
    }

    public void notifyTransferRequest(NetworkDevice device, TransferGroup group, TransferObject.Type type,
                                      List<TransferObject> objectList)
    {
        int numberOfFiles = objectList.size();
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(group.id, device.id, type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
        String message = numberOfFiles > 1 ? getContext().getResources().getQuantityString(R.plurals.ques_receiveMultipleFiles, numberOfFiles, numberOfFiles) : objectList.get(0).name;
        Intent acceptIntent = new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_FILE_TRANSFER)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, device.id)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, group.id).putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

        Intent rejectIntent = ((Intent) acceptIntent.clone());

        acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
        rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

        PendingIntent positiveIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), acceptIntent, 0);
        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), rejectIntent, 0);

        notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(getContext().getString(R.string.ques_receiveFile))
                .setContentText(message)
                .setContentInfo(device.nickname)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, group.id), 0))
                .setDefaults(getUtils().getNotificationSettings())
                .setDeleteIntent(negativeIntent)
                .addAction(R.drawable.ic_check_white_24dp_static, getContext().getString(R.string.butn_receive), positiveIntent)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_reject), negativeIntent)
                .setTicker(getContext().getString(R.string.ques_receiveFile))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notification.show();
    }

    public void notifyFileTransaction(CommunicationService.ProcessHolder processHolder) throws Exception
    {
        if (processHolder.notification == null) {
            NetworkDevice device = new NetworkDevice(processHolder.device.id);
            getUtils().getDatabase().reconstruct(device);

            boolean isIncoming = TransferObject.Type.INCOMING.equals(processHolder.object.type);

            processHolder.notification = getUtils().buildDynamicNotification(
                    TransferUtils.createUniqueTransferId(processHolder.group.id, device.id, processHolder.object.type),
                    NotificationUtils.NOTIFICATION_CHANNEL_LOW);
            Intent cancelIntent = new Intent(getContext(), CommunicationService.class);

            cancelIntent.setAction(CommunicationService.ACTION_STOP_TRANSFER)
                    .putExtra(CommunicationService.EXTRA_REQUEST_ID, processHolder.object.id)
                    .putExtra(CommunicationService.EXTRA_GROUP_ID, processHolder.group.id)
                    .putExtra(CommunicationService.EXTRA_DEVICE_ID, processHolder.device.id)
                    .putExtra(CommunicationService.EXTRA_TRANSFER_TYPE, processHolder.type.toString())
                    .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, processHolder.notification.getNotificationId());

            processHolder.notification.setSmallIcon(isIncoming ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_upload)
                    .setContentText(getContext().getString(isIncoming ? R.string.text_receiving : R.string.text_sending))
                    .setContentInfo(device.nickname)
                    .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                            .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                            .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, processHolder.object.groupId), 0))
                    .setOngoing(true)
                    .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(isIncoming ? R.string.butn_cancelReceiving : R.string.butn_cancelSending), PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), cancelIntent, 0));
        }

        processHolder.notification.setContentTitle(processHolder.object.name);

    }

    public void notifyClipboardRequest(NetworkDevice device, TextStreamObject object)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(object.id, NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        Intent acceptIntent = new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_CLIPBOARD)
                .putExtra(CommunicationService.EXTRA_CLIPBOARD_ID, object.id)
                .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

        Intent activityIntent = new Intent(getContext(), TextEditorActivity.class);

        Intent rejectIntent = ((Intent) acceptIntent.clone());

        acceptIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, true);
        rejectIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, false);

        PendingIntent positiveIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), acceptIntent, 0);
        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), rejectIntent, 0);

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
                .setContentInfo(device.nickname)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), activityIntent, 0))
                .setDefaults(getUtils().getNotificationSettings())
                .setDeleteIntent(negativeIntent)
                .addAction(R.drawable.ic_check_white_24dp_static, getContext().getString(android.R.string.copy), positiveIntent)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(android.R.string.no), negativeIntent)
                .setTicker(getContext().getString(R.string.text_receivedTextSummary))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        notification.show();
    }

    public void notifyFileReceived(CommunicationService.ProcessHolder processHolder, NetworkDevice device, DocumentFile savePath)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(processHolder.group.id, device.id, processHolder.object.type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentInfo(device.nickname)
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(getContext().getString(R.string.text_receivedTransfer,
                        FileUtils.sizeExpression(processHolder.completedBytes, false),
                        TimeUtils.getFriendlyElapsedTime(getContext(), processHolder.timePassed)));

        if (processHolder.completedCount != 1) {
            notification
                    .setContentTitle(getContext().getResources().getQuantityString(
                            R.plurals.text_fileReceiveCompletedSummary, processHolder.completedCount,
                            processHolder.completedCount))
                    .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), FileExplorerActivity.class)
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        } else {
            try {
                Intent openIntent = FileUtils.getOpenIntent(getContext(), processHolder.currentFile);
                notification.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), openIntent, 0));
            } catch (Exception e) {
                // do nothing
            }

            notification
                    .setContentTitle(processHolder.object.name)
                    .addAction(R.drawable.ic_folder_white_24dp_static, getContext().getString(R.string.butn_showFiles),
                            PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), FileExplorerActivity.class)
                                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        }

        notification.show();
    }

    public void notifyReceiveError(CommunicationService.ProcessHolder processHolder)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(processHolder.group.id, processHolder.device.id, processHolder.object.type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_error))
                .setContentText(getContext().getString(R.string.mesg_fileReceiveFilesLeftError))
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, processHolder.group.id), 0));

        notification.show();
    }

    public void notifyReceiveError(TransferObject transferObject, NetworkDevice device)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(transferObject.getId(), NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_error))
                .setContentText(getContext().getString(R.string.mesg_fileReceiveError, transferObject.name))
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, transferObject.groupId)
                        .putExtra(ViewTransferActivity.EXTRA_REQUEST_ID, transferObject.id)
                        .putExtra(ViewTransferActivity.EXTRA_REQUEST_TYPE, transferObject.type.toString())
                        .putExtra(ViewTransferActivity.EXTRA_DEVICE_ID, device.id), 0));

        notification.show();
    }

    public void notifyConnectionError(CommunicationService.ProcessHolder holder, @Nullable String errorKey)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(holder.group.id, holder.device.id, holder.type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
        String errorMsg = getContext().getString(R.string.mesg_deviceConnectionError, holder.device.nickname,
                TextUtils.getAdapterName(getContext(), holder.connection));

        if (errorKey != null)
            switch (errorKey) {
                case Keyword.ERROR_NOT_ALLOWED:
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
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, holder.group.id), 0));

        notification.show();
    }

    public DynamicNotification notifyPrepareFiles(TransferGroup group, NetworkDevice device)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(group.id, device.id, TransferObject.Type.INCOMING),
                NotificationUtils.NOTIFICATION_CHANNEL_LOW);

        Intent cancelIntent = new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_CANCEL_INDEXING)
                .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId())
                .putExtra(CommunicationService.EXTRA_GROUP_ID, group.id);

        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), cancelIntent, 0);

        notification.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getContext().getString(R.string.text_preparingFiles))
                .setContentText(getContext().getString(R.string.text_savingDetails))
                .setAutoCancel(false)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_cancel), negativeIntent)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, group.id), 0));

        return notification.show();
    }

    public DynamicNotification notifyStuckThread(CommunicationService.ProcessHolder processHolder)
    {
        return notifyStuckThread(processHolder.group.id, processHolder.device.id, processHolder.type);
    }

    public DynamicNotification notifyStuckThread(long groupId, String deviceId, TransferObject.Type type)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(groupId, deviceId, type),
                NotificationUtils.NOTIFICATION_CHANNEL_LOW);

        Intent killIntent = new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_STOP_TRANSFER)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, groupId)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, deviceId)
                .putExtra(CommunicationService.EXTRA_TRANSFER_TYPE, type.toString())
                .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setOngoing(true)
                .setContentTitle(getContext().getString(R.string.text_stopping))
                .setContentText(getContext().getString(R.string.text_cancellingTransfer))
                .setProgress(0, 0, true)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_killNow),
                        PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), killIntent, 0));

        return notification.show();
    }

    public void showToast(int toastTextRes)
    {
        Toast.makeText(getContext(), toastTextRes, Toast.LENGTH_SHORT).show();
    }
}
