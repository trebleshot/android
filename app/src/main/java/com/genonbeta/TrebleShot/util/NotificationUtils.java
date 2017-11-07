package com.genonbeta.TrebleShot.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.NotificationCompat;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.PendingTransferListActivity;
import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.receiver.DialogEventReceiver;
import com.genonbeta.TrebleShot.service.AbstractTransactionService;
import com.genonbeta.TrebleShot.service.ClientService;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;

import java.io.File;

/**
 * Created by: veli
 * Date: 4/28/17 2:00 AM
 */

public class NotificationUtils
{
	public static final String TAG = "NotificationUtils";
	public static final String EXTRA_NOTIFICATION_ID = "notificationId";

	private Context mContext;
	private NotificationManagerCompat mManager;
	private AccessDatabase mDatabase;
	private SharedPreferences mPreferences;

	public NotificationUtils(Context context)
	{
		mContext = context;
		mManager = NotificationManagerCompat.from(context);
		mDatabase = new AccessDatabase(context);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
	}

	public NotificationUtils cancel(int notificationId)
	{
		mManager.cancel(notificationId);
		return this;
	}

	public int getNotificationSettings()
	{
		int makeSound = (mPreferences.getBoolean("notification_sound", false)) ? NotificationCompat.DEFAULT_SOUND : 0;
		int vibrate = (mPreferences.getBoolean("notification_vibrate", false)) ? NotificationCompat.DEFAULT_VIBRATE : 0;
		int light = (mPreferences.getBoolean("notification_light", false)) ? NotificationCompat.DEFAULT_LIGHTS : 0;

		return makeSound | vibrate | light;
	}

	public DynamicNotification notifyConnectionRequest(NetworkDevice device)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, AppUtils.getUniqueNumber());

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_IP);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_ID, device.deviceId);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.text_connectionPermission));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, mContext.getString(R.string.ques_allowDeviceToConnect));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		notification.setSmallIcon(android.R.drawable.stat_notify_error)
				.setContentTitle(mContext.getString(R.string.text_connectionPermission))
				.setContentText(mContext.getString(R.string.ques_allowDeviceToConnect))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getBroadcast(mContext, AppUtils.getUniqueNumber(), dialogIntent, 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(R.string.butn_accept), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.butn_reject), negativeIntent)
				.setTicker(mContext.getString(R.string.text_connectionPermission));

		return notification.show();
	}

	public DynamicNotification notifyTransferRequest(TransactionObject transactionObject, NetworkDevice device, int numberOfFiles)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, transactionObject.groupId);
		String message = numberOfFiles > 1 ? mContext.getResources().getQuantityString(R.plurals.ques_receiveMultipleFiles, numberOfFiles, numberOfFiles) : transactionObject.friendlyName; // TODO: 4.11.2017 Add the file name

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER);
		acceptIntent.putExtra(CommunicationService.EXTRA_GROUP_ID, transactionObject.groupId);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.ques_receiveFile));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, message);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.ques_receiveFile))
				.setContentText(message)
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, PendingTransferListActivity.class)
						.setAction(PendingTransferListActivity.ACTION_LIST_TRANSFERS)
						.putExtra(PendingTransferListActivity.EXTRA_GROUP_ID, transactionObject.groupId), 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(R.string.butn_accept), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.butn_reject), negativeIntent)
				.setTicker(mContext.getString(R.string.ques_receiveFile))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyFileTransaction(TransactionObject transaction) throws Exception
	{
		TransactionObject.Group group = new TransactionObject.Group(transaction.groupId);
		mDatabase.reconstruct(group);

		NetworkDevice device = new NetworkDevice(group.deviceId);
		mDatabase.reconstruct(device);

		boolean isIncoming = TransactionObject.Type.INCOMING.equals(transaction.type);

		DynamicNotification notification = new DynamicNotification(mContext, mManager, transaction.groupId);
		Intent cancelIntent = new Intent(mContext, isIncoming ? ServerService.class : ClientService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		cancelIntent.setAction(AbstractTransactionService.ACTION_CANCEL_JOB);
		cancelIntent.putExtra(CommunicationService.EXTRA_REQUEST_ID, transaction.requestId);
		cancelIntent.putExtra(CommunicationService.EXTRA_GROUP_ID, transaction.groupId);
		cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		PendingIntent positiveIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), cancelIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(isIncoming ? R.string.butn_cancelReceiving : R.string.butn_cancelSending));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, transaction.file);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);

		notification.setSmallIcon(isIncoming ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_upload)
				.setContentTitle(transaction.friendlyName)
				.setContentText(mContext.getString(isIncoming ? R.string.text_receiving : R.string.text_sending))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, PendingTransferListActivity.class)
						.setAction(PendingTransferListActivity.ACTION_LIST_TRANSFERS)
						.putExtra(PendingTransferListActivity.EXTRA_GROUP_ID, transaction.groupId), 0))
				.setOngoing(true)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(isIncoming ? R.string.butn_cancelReceiving : R.string.butn_cancelSending), PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), cancelIntent, 0));

		return notification.show();
	}

	public DynamicNotification notifyClipboardRequest(NetworkDevice device, CharSequence text)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, AppUtils.getUniqueNumber());

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_CLIPBOARD);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.ques_copyToClipboard));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, text);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.ques_copyToClipboard))
				.setContentText(mContext.getString(R.string.text_textReceived))
				.setStyle(new NotificationCompat.BigTextStyle()
						.bigText(text)
						.setBigContentTitle(mContext.getString(R.string.ques_copyToClipboard)))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getBroadcast(mContext, AppUtils.getUniqueNumber(), dialogIntent, 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(R.string.butn_accept), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.butn_reject), negativeIntent)
				.setTicker(mContext.getString(R.string.text_receivedTextSummary))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyFileReceived(TransactionObject transactionObject, File file) throws Exception
	{
		TransactionObject.Group group = new TransactionObject.Group(transactionObject.groupId);
		mDatabase.reconstruct(group);

		NetworkDevice device = new NetworkDevice(group.deviceId);
		mDatabase.reconstruct(device);

		DynamicNotification notification = new DynamicNotification(mContext, mManager, transactionObject.groupId);
		Intent openIntent = new Intent(Intent.ACTION_VIEW);

		try {
			mDatabase.reconstruct(device);
		} catch (Exception e) {
			e.printStackTrace();
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
			openIntent.setDataAndType(FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", file), transactionObject.fileMimeType)
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		else
			openIntent.setDataAndType(Uri.fromFile(file), transactionObject.fileMimeType);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(transactionObject.friendlyName)
				.setContentText(mContext.getString(R.string.text_fileReceived))
				.setContentInfo(device.user)
				.setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), openIntent, 0));

		return notification.show();
	}

	public DynamicNotification notifyFileReceived(TransactionObject transactionObject, String parentDir, int numberOfFiles)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, transactionObject.groupId);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.text_multipleFileReceiveCompleted))
				.setContentText(mContext.getResources().getQuantityString(R.plurals.text_fileReceiveCompletedSummary, numberOfFiles, numberOfFiles))
				.setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, HomeActivity.class)
						.setAction(HomeActivity.ACTION_OPEN_RECEIVED_FILES)
						.putExtra(HomeActivity.EXTRA_FILE_PATH, parentDir), 0));

		return notification.show();
	}

	public DynamicNotification notifyReceiveError(TransactionObject transactionObject)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, transactionObject.groupId);

		notification.setSmallIcon(android.R.drawable.stat_notify_error)
				.setContentTitle(mContext.getString(R.string.text_error))
				.setContentText(mContext.getString(R.string.mesg_fileReceiveError, transactionObject.friendlyName))
				.setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, PendingTransferListActivity.class)
						.setAction(PendingTransferListActivity.ACTION_LIST_TRANSFERS)
						.putExtra(PendingTransferListActivity.EXTRA_GROUP_ID, transactionObject.groupId), 0));

		return notification.show();
	}

	public DynamicNotification notifyStuckThread(TransactionObject transaction)
	{
		boolean isIncoming = TransactionObject.Type.INCOMING.equals(transaction.type);

		DynamicNotification notification = new DynamicNotification(mContext, mManager, transaction.groupId);
		Intent killIntent = new Intent(mContext, isIncoming ? ServerService.class : ClientService.class).setAction(AbstractTransactionService.ACTION_CANCEL_KILL);

		notification.setSmallIcon(android.R.drawable.stat_sys_warning)
				.setOngoing(true)
				.setContentTitle(mContext.getString(R.string.text_stopping))
				.setContentText(mContext.getString(R.string.text_cancellingTransfer))
				.setProgress(0, 0, true)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.butn_killNow), PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), killIntent, 0));

		return notification.show();
	}

	public void showToast(String toastText)
	{
		Toast.makeText(mContext, toastText, Toast.LENGTH_SHORT).show();
	}

	public void showToast(int toastTextRes)
	{
		Toast.makeText(mContext, toastTextRes, Toast.LENGTH_SHORT).show();
	}

	public void showToast(String text, int length)
	{
		Toast.makeText(mContext, text, length).show();
	}

	public void showToast(int textRes, int length)
	{
		Toast.makeText(mContext, textRes, length).show();
	}
}
