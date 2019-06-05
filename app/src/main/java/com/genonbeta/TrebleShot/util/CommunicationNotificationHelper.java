package com.genonbeta.TrebleShot.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.FileExplorerActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferInstance;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.receiver.DialogEventReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.android.framework.io.DocumentFile;

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
                        PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), CommunicationService.class).setAction(CommunicationService.ACTION_TOGGLE_SEAMLESS_MODE), PendingIntent.FLAG_CANCEL_CURRENT))
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

    public DynamicNotification notifyConnectionRequest(NetworkDevice device)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(AppUtils.getUniqueNumber(), NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        Intent acceptIntent = new Intent(getContext(), CommunicationService.class);
        Intent dialogIntent = new Intent(getContext(), DialogEventReceiver.class);

        acceptIntent.setAction(CommunicationService.ACTION_IP);
        acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_ID, device.deviceId);
        acceptIntent.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

        Intent rejectIntent = ((Intent) acceptIntent.clone());

        acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
        rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

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

        return notification.show();
    }

    public DynamicNotification notifyTransferRequest(TransferObject transferObject, NetworkDevice device, int numberOfFiles)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(transferObject.groupId, device.deviceId, transferObject.type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
        String message = numberOfFiles > 1 ? getContext().getResources().getQuantityString(R.plurals.ques_receiveMultipleFiles, numberOfFiles, numberOfFiles) : transferObject.friendlyName;
        Intent acceptIntent = new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_FILE_TRANSFER)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, device.deviceId)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, transferObject.groupId).putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

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
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, transferObject.groupId), 0))
                .setDefaults(getUtils().getNotificationSettings())
                .setDeleteIntent(negativeIntent)
                .addAction(R.drawable.ic_check_white_24dp_static, getContext().getString(R.string.butn_receive), positiveIntent)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_reject), negativeIntent)
                .setTicker(getContext().getString(R.string.ques_receiveFile))
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        return notification.show();
    }

    public DynamicNotification notifyFileTransaction(CommunicationService.ProcessHolder processHolder) throws Exception
    {
        if (processHolder.notification == null) {
            NetworkDevice device = new NetworkDevice(processHolder.deviceId);
            getUtils().getDatabase().reconstruct(device);

            boolean isIncoming = TransferObject.Type.INCOMING.equals(processHolder.transferObject.type);

            processHolder.notification = getUtils().buildDynamicNotification(
                    TransferUtils.createUniqueTransferId(processHolder.groupId, device.deviceId, processHolder.transferObject.type),
                    NotificationUtils.NOTIFICATION_CHANNEL_LOW);
            Intent cancelIntent = new Intent(getContext(), CommunicationService.class);

            cancelIntent.setAction(CommunicationService.ACTION_CANCEL_JOB);
            cancelIntent.putExtra(CommunicationService.EXTRA_REQUEST_ID, processHolder.transferObject.requestId);
            cancelIntent.putExtra(CommunicationService.EXTRA_GROUP_ID, processHolder.groupId);
            cancelIntent.putExtra(CommunicationService.EXTRA_DEVICE_ID, processHolder.deviceId);
            cancelIntent.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, processHolder.notification.getNotificationId());

            processHolder.notification.setSmallIcon(isIncoming ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_upload)
                    .setContentText(getContext().getString(isIncoming ? R.string.text_receiving : R.string.text_sending))
                    .setContentInfo(device.nickname)
                    .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                            .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                            .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, processHolder.transferObject.groupId), 0))
                    .setOngoing(true)
                    .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(isIncoming ? R.string.butn_cancelReceiving : R.string.butn_cancelSending), PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), cancelIntent, 0));
        }

        processHolder.notification.setContentTitle(processHolder.transferObject.friendlyName);

        return processHolder.notification;
    }

    public DynamicNotification notifyClipboardRequest(NetworkDevice device, TextStreamObject object)
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

        return notification.show();
    }

    public DynamicNotification notifyFileReceived(CommunicationService.ProcessHolder processHolder, NetworkDevice device, DocumentFile savePath)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(processHolder.groupId, device.deviceId, processHolder.transferObject.type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
        CoolTransfer.TransferProgress progress = processHolder.builder.getTransferProgress();

        notification
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentInfo(device.nickname)
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentText(getContext().getString(R.string.text_receivedTransfer, FileUtils.sizeExpression(progress.getTransferredByte(), false), TimeUtils.getFriendlyElapsedTime(getContext(), progress.getTimeElapsed())));

        if (progress.getTransferredFileCount() != 1) {
            notification
                    .setContentTitle(getContext().getResources().getQuantityString(R.plurals.text_fileReceiveCompletedSummary, progress.getTransferredFileCount(), progress.getTransferredFileCount()))
                    .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), FileExplorerActivity.class)
                            .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        } else {
            try {
                Intent openIntent = FileUtils.getOpenIntent(getContext(), processHolder.currentFile);
                notification.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), openIntent, 0));
            } catch (Exception e) {
            }

            notification
                    .setContentTitle(processHolder.transferObject.friendlyName)
                    .addAction(R.drawable.ic_folder_white_24dp_static, getContext().getString(R.string.butn_showFiles),
                            PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), FileExplorerActivity.class)
                                    .putExtra(FileExplorerActivity.EXTRA_FILE_PATH, savePath.getUri()), 0));
        }

        return notification.show();
    }

    public DynamicNotification notifyReceiveError(CommunicationService.ProcessHolder processHolder)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(processHolder.groupId, processHolder.deviceId, processHolder.transferObject.type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_error))
                .setContentText(getContext().getString(R.string.mesg_fileReceiveFilesLeftError))
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, processHolder.groupId), 0));

        return notification.show();
    }

    public DynamicNotification notifyReceiveError(TransferObject transferObject)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(transferObject.getId(), NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

        notification.setSmallIcon(R.drawable.ic_alert_circle_outline_white_24dp_static)
                .setContentTitle(getContext().getString(R.string.text_error))
                .setContentText(getContext().getString(R.string.mesg_fileReceiveError, transferObject.friendlyName))
                .setAutoCancel(true)
                .setDefaults(getUtils().getNotificationSettings())
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, transferObject.groupId)
                        .putExtra(ViewTransferActivity.EXTRA_REQUEST_ID, transferObject.requestId)
                        .putExtra(ViewTransferActivity.EXTRA_REQUEST_TYPE, transferObject.type.toString())
                        .putExtra(ViewTransferActivity.EXTRA_DEVICE_ID, transferObject.deviceId), 0));

        return notification.show();
    }

    public DynamicNotification notifyConnectionError(TransferInstance transferInstance, TransferObject.Type type, @Nullable String errorKey)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(transferInstance.getGroup().groupId, transferInstance.getDevice().deviceId, type),
                NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
        String errorMsg = getContext().getString(R.string.mesg_deviceConnectionError, transferInstance.getDevice().nickname, TextUtils.getAdapterName(getContext(), transferInstance.getConnection()));

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
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, transferInstance.getGroup().groupId), 0));

        return notification.show();
    }

    public DynamicNotification notifyPrepareFiles(TransferGroup group, NetworkDevice device)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(group.groupId, device.deviceId, TransferObject.Type.INCOMING),
                NotificationUtils.NOTIFICATION_CHANNEL_LOW);

        Intent cancelIntent = new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_CANCEL_INDEXING)
                .putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId())
                .putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId);

        PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), cancelIntent, 0);

        notification.setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getContext().getString(R.string.text_preparingFiles))
                .setContentText(getContext().getString(R.string.text_savingDetails))
                .setAutoCancel(false)
                .addAction(R.drawable.ic_close_white_24dp_static, getContext().getString(R.string.butn_cancel), negativeIntent)
                .setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), ViewTransferActivity.class)
                        .setAction(ViewTransferActivity.ACTION_LIST_TRANSFERS)
                        .putExtra(ViewTransferActivity.EXTRA_GROUP_ID, group.groupId), 0));

        return notification.show();
    }

    public DynamicNotification notifyStuckThread(CommunicationService.ProcessHolder processHolder)
    {
        return notifyStuckThread(processHolder.groupId, processHolder.deviceId, processHolder.type);
    }

    public DynamicNotification notifyStuckThread(long groupId, String deviceId, TransferObject.Type type)
    {
        DynamicNotification notification = getUtils().buildDynamicNotification(
                TransferUtils.createUniqueTransferId(groupId, deviceId, type),
                NotificationUtils.NOTIFICATION_CHANNEL_LOW);

        Intent killIntent = new Intent(getContext(), CommunicationService.class)
                .setAction(CommunicationService.ACTION_CANCEL_JOB)
                .putExtra(CommunicationService.EXTRA_GROUP_ID, groupId)
                .putExtra(CommunicationService.EXTRA_DEVICE_ID, deviceId)
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

    public void showToast(String toastText)
    {
        Toast.makeText(getContext(), toastText, Toast.LENGTH_SHORT).show();
    }

    public void showToast(int toastTextRes)
    {
        Toast.makeText(getContext(), toastTextRes, Toast.LENGTH_SHORT).show();
    }

    public void showToast(String text, int length)
    {
        Toast.makeText(getContext(), text, length).show();
    }

    public void showToast(int textRes, int length)
    {
        Toast.makeText(getContext(), textRes, length).show();
    }
}
