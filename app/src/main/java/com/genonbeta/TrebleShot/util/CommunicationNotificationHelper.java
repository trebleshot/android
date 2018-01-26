package com.genonbeta.TrebleShot.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.activity.TransactionActivity;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.object.TransferInstance;
import com.genonbeta.TrebleShot.receiver.DialogEventReceiver;
import com.genonbeta.TrebleShot.service.CommunicationService;

import java.io.File;

/**
 * created by: Veli
 * date: 26.01.2018 18:29
 */

public class CommunicationNotificationHelper
{
	public static final int SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID = 1;

	private NotificationUtils mNotificationUtils;

	public CommunicationNotificationHelper(Context context)
	{
		mNotificationUtils = new NotificationUtils(context);
	}

	public DynamicNotification getCommunicationServiceNotification(boolean seamlessMode)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID, NotificationUtils.NOTIFICATION_CHANNEL_LOW);

		notification.setSmallIcon(R.drawable.ic_whatshot_white_24dp)
				.setContentTitle(getContext().getString(R.string.text_communicationServiceRunning))
				.setContentText(getContext().getString(R.string.text_communicationServiceStop))
				.setAutoCancel(true)
				.addAction(R.drawable.ic_compare_arrows_white_24dp, getContext().getString(seamlessMode ? R.string.butn_turnTrustZoneOff : R.string.butn_turnTrustZoneOn),
						PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), CommunicationService.class).setAction(CommunicationService.ACTION_TOGGLE_SEAMLESS_MODE), 0))
				.setContentIntent(PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), CommunicationService.class)
						.setAction(CommunicationService.ACTION_END_SESSION), 0));

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

		notification.setSmallIcon(R.drawable.ic_error_white_24dp)
				.setContentTitle(getContext().getString(R.string.text_connectionPermission))
				.setContentText(getContext().getString(R.string.ques_allowDeviceToConnect))
				.setContentInfo(device.nickname)
				.setContentIntent(PendingIntent.getBroadcast(getContext(), AppUtils.getUniqueNumber(), dialogIntent, 0))
				.setDefaults(getUtils().getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(R.drawable.ic_check_white_24dp, getContext().getString(R.string.butn_accept), positiveIntent)
				.addAction(R.drawable.ic_clear_white_24dp, getContext().getString(R.string.butn_reject), negativeIntent)
				.setTicker(getContext().getString(R.string.text_connectionPermission));

		return notification.show();
	}

	public DynamicNotification notifyTransferRequest(TransactionObject transactionObject, NetworkDevice device, int numberOfFiles)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(transactionObject.groupId, NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
		String message = numberOfFiles > 1 ? getContext().getResources().getQuantityString(R.plurals.ques_receiveMultipleFiles, numberOfFiles, numberOfFiles) : transactionObject.friendlyName;

		Intent acceptIntent = new Intent(getContext(), CommunicationService.class);

		acceptIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER);
		acceptIntent.putExtra(CommunicationService.EXTRA_GROUP_ID, transactionObject.groupId);
		acceptIntent.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), rejectIntent, 0);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(getContext().getString(R.string.ques_receiveFile))
				.setContentText(message)
				.setContentInfo(device.nickname)
				.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, transactionObject.groupId), 0))
				.setDefaults(getUtils().getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(R.drawable.ic_check_white_24dp, getContext().getString(R.string.butn_accept), positiveIntent)
				.addAction(R.drawable.ic_clear_white_24dp, getContext().getString(R.string.butn_reject), negativeIntent)
				.setTicker(getContext().getString(R.string.ques_receiveFile))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyFileTransaction(TransactionObject transaction) throws Exception
	{
		TransactionObject.Group group = new TransactionObject.Group(transaction.groupId);
		getUtils().getDatabase().reconstruct(group);

		NetworkDevice device = new NetworkDevice(group.deviceId);
		getUtils().getDatabase().reconstruct(device);

		boolean isIncoming = TransactionObject.Type.INCOMING.equals(transaction.type);

		DynamicNotification notification = getUtils().buildDynamicNotification(transaction.groupId, NotificationUtils.NOTIFICATION_CHANNEL_LOW);
		Intent cancelIntent = new Intent(getContext(), CommunicationService.class);

		cancelIntent.setAction(CommunicationService.ACTION_CANCEL_JOB);
		cancelIntent.putExtra(CommunicationService.EXTRA_REQUEST_ID, transaction.requestId);
		cancelIntent.putExtra(CommunicationService.EXTRA_GROUP_ID, transaction.groupId);
		cancelIntent.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		notification.setSmallIcon(isIncoming ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_upload)
				.setContentTitle(transaction.friendlyName)
				.setContentText(getContext().getString(isIncoming ? R.string.text_receiving : R.string.text_sending))
				.setContentInfo(device.nickname)
				.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, transaction.groupId), 0))
				.setOngoing(true)
				.addAction(R.drawable.ic_clear_white_24dp, getContext().getString(isIncoming ? R.string.butn_cancelReceiving : R.string.butn_cancelSending), PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), cancelIntent, 0));

		return notification;
	}

	public DynamicNotification notifyClipboardRequest(NetworkDevice device, TextStreamObject object)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(object.id, NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

		Intent acceptIntent = new Intent(getContext(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_CLIPBOARD)
				.putExtra(CommunicationService.EXTRA_CLIPBOARD_ID, object.id)
				.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, object.id);

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
				.addAction(R.drawable.ic_check_white_24dp, getContext().getString(R.string.butn_accept), positiveIntent)
				.addAction(R.drawable.ic_clear_white_24dp, getContext().getString(R.string.butn_reject), negativeIntent)
				.setTicker(getContext().getString(R.string.text_receivedTextSummary))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyFileReceived(TransactionObject transactionObject, NetworkDevice device, File file)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(transactionObject.groupId, NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

		try {
			Intent openIntent = new Intent(Intent.ACTION_VIEW);

			openIntent.setDataAndType(FileUtils.getUriForFile(getContext(), file, openIntent), transactionObject.fileMimeType);
			notification.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), openIntent, 0));
		} catch (Exception e) {
		}

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(transactionObject.friendlyName)
				.setContentText(getContext().getString(R.string.text_fileReceived))
				.setContentInfo(device.nickname)
				.setAutoCancel(true)
				.setDefaults(getUtils().getNotificationSettings())
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.addAction(R.drawable.ic_folder_white_24dp, getContext().getString(R.string.butn_showFiles),
						PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), HomeActivity.class)
								.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
								.putExtra(HomeActivity.EXTRA_FILE_PATH, file.getParent()), 0));

		return notification.show();
	}

	public DynamicNotification notifyFileReceived(TransactionObject transactionObject, String parentDir, int numberOfFiles)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(transactionObject.groupId, NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(getContext().getString(R.string.text_multipleFileReceiveCompleted))
				.setContentText(getContext().getResources().getQuantityString(R.plurals.text_fileReceiveCompletedSummary, numberOfFiles, numberOfFiles))
				.setAutoCancel(true)
				.setDefaults(getUtils().getNotificationSettings())
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), HomeActivity.class)
						.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
						.putExtra(HomeActivity.EXTRA_FILE_PATH, parentDir), 0));

		return notification.show();
	}

	public DynamicNotification notifyReceiveError(TransactionObject transactionObject)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(transactionObject.groupId, NotificationUtils.NOTIFICATION_CHANNEL_HIGH);

		notification.setSmallIcon(R.drawable.ic_error_white_24dp)
				.setContentTitle(getContext().getString(R.string.text_error))
				.setContentText(getContext().getString(R.string.mesg_fileReceiveError, transactionObject.friendlyName))
				.setAutoCancel(true)
				.setDefaults(getUtils().getNotificationSettings())
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, transactionObject.groupId), 0));

		return notification.show();
	}

	public DynamicNotification notifyConnectionError(TransferInstance transferInstance, @Nullable String errorKey)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(transferInstance.getGroup().groupId, NotificationUtils.NOTIFICATION_CHANNEL_HIGH);
		String errorMsg = getContext().getString(R.string.mesg_deviceConnectionError, transferInstance.getDevice().nickname, TextUtils.getAdapterName(getContext(), transferInstance.getConnection()));

		if (errorKey != null)
			switch (errorKey) {
				case Keyword.NOT_ALLOWED:
					errorMsg = getContext().getString(R.string.mesg_notAllowed);
					break;
				case Keyword.NOT_FOUND:
					errorMsg = getContext().getString(R.string.mesg_notValidTransfer);
			}

		notification.setSmallIcon(R.drawable.ic_error_white_24dp)
				.setContentTitle(getContext().getString(R.string.text_error))
				.setContentText(errorMsg)
				.setAutoCancel(true)
				.setDefaults(getUtils().getNotificationSettings())
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, transferInstance.getGroup().groupId), 0));

		return notification.show();
	}

	public DynamicNotification notifyPrepareFiles(TransactionObject.Group group)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(group.groupId, NotificationUtils.NOTIFICATION_CHANNEL_LOW);

		Intent cancelIntent = new Intent(getContext(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_CANCEL_INDEXING)
				.putExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, notification.getNotificationId())
				.putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId);

		PendingIntent negativeIntent = PendingIntent.getService(getContext(), AppUtils.getUniqueNumber(), cancelIntent, 0);

		notification.setSmallIcon(android.R.drawable.stat_sys_download)
				.setContentTitle(getContext().getString(R.string.text_preparingFiles))
				.setContentText(getContext().getString(R.string.text_savingDetails))
				.setAutoCancel(false)
				.addAction(R.drawable.ic_clear_white_24dp, getContext().getString(R.string.butn_cancel), negativeIntent)
				.setContentIntent(PendingIntent.getActivity(getContext(), AppUtils.getUniqueNumber(), new Intent(getContext(), TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, group.groupId), 0));

		return notification.show();
	}

	public DynamicNotification notifyStuckThread(TransactionObject transaction)
	{
		DynamicNotification notification = getUtils().buildDynamicNotification(transaction.groupId, NotificationUtils.NOTIFICATION_CHANNEL_LOW);
		Intent killIntent = new Intent(getContext(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_CANCEL_KILL);

		notification.setSmallIcon(R.drawable.ic_error_white_24dp)
				.setOngoing(true)
				.setContentTitle(getContext().getString(R.string.text_stopping))
				.setContentText(getContext().getString(R.string.text_cancellingTransfer))
				.setProgress(0, 0, true)
				.addAction(R.drawable.ic_clear_white_24dp, getContext().getString(R.string.butn_killNow),
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
