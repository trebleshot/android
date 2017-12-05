package com.genonbeta.TrebleShot.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.FileProvider;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.HomeActivity;
import com.genonbeta.TrebleShot.activity.TextEditorActivity;
import com.genonbeta.TrebleShot.activity.TransactionActivity;
import com.genonbeta.TrebleShot.app.TransactionService;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.receiver.DialogEventReceiver;
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
	public static final String NOTIFICATION_CHANNEL_HIGH = "tsHighPriority";
	public static final String NOTIFICATION_CHANNEL_LOW = "tsLowPriority";

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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

			NotificationChannel channelHigh = new NotificationChannel(NOTIFICATION_CHANNEL_HIGH, mContext.getString(R.string.text_appName), NotificationManager.IMPORTANCE_HIGH);
			channelHigh.enableLights(mPreferences.getBoolean("notification_light", false));
			channelHigh.enableVibration(mPreferences.getBoolean("notification_vibrate", false));
			notificationManager.createNotificationChannel(channelHigh);

			NotificationChannel channelLow = new NotificationChannel(NOTIFICATION_CHANNEL_LOW, mContext.getString(R.string.text_appName), NotificationManager.IMPORTANCE_NONE);
			notificationManager.createNotificationChannel(channelLow);
		}
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

		notification.setChannelId(NOTIFICATION_CHANNEL_HIGH)
				.setSmallIcon(android.R.drawable.stat_notify_error)
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
		String message = numberOfFiles > 1 ? mContext.getResources().getQuantityString(R.plurals.ques_receiveMultipleFiles, numberOfFiles, numberOfFiles) : transactionObject.friendlyName;

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);

		acceptIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER);
		acceptIntent.putExtra(CommunicationService.EXTRA_GROUP_ID, transactionObject.groupId);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), rejectIntent, 0);

		notification.setChannelId(NOTIFICATION_CHANNEL_HIGH)
				.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.ques_receiveFile))
				.setContentText(message)
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, transactionObject.groupId), 0))
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

		cancelIntent.setAction(TransactionService.ACTION_CANCEL_JOB);
		cancelIntent.putExtra(CommunicationService.EXTRA_REQUEST_ID, transaction.requestId);
		cancelIntent.putExtra(CommunicationService.EXTRA_GROUP_ID, transaction.groupId);
		cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		notification.setChannelId(NOTIFICATION_CHANNEL_LOW)
				.setSmallIcon(isIncoming ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_upload)
				.setContentTitle(transaction.friendlyName)
				.setContentText(mContext.getString(isIncoming ? R.string.text_receiving : R.string.text_sending))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, transaction.groupId), 0))
				.setOngoing(true)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(isIncoming ? R.string.butn_cancelReceiving : R.string.butn_cancelSending), PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), cancelIntent, 0));

		return notification.show();
	}

	public DynamicNotification notifyClipboardRequest(NetworkDevice device, CharSequence text)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, AppUtils.getUniqueNumber());

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent activityIntent = new Intent(mContext, TextEditorActivity.class);

		acceptIntent.setAction(CommunicationService.ACTION_CLIPBOARD).putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), rejectIntent, 0);

		activityIntent
				.setAction(TextEditorActivity.ACTION_EDIT_TEXT)
				.putExtra(TextEditorActivity.EXTRA_TEXT_INDEX, text)
				.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

		notification
				.setChannelId(NOTIFICATION_CHANNEL_HIGH)
				.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.ques_copyToClipboard))
				.setContentText(mContext.getString(R.string.text_textReceived))
				.setStyle(new NotificationCompat.BigTextStyle()
						.bigText(text)
						.setBigContentTitle(mContext.getString(R.string.ques_copyToClipboard)))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), activityIntent, 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(R.string.butn_accept), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.butn_reject), negativeIntent)
				.setTicker(mContext.getString(R.string.text_receivedTextSummary))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyFileReceived(TransactionObject transactionObject, NetworkDevice device, File file)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, transactionObject.groupId);
		Intent openIntent = new Intent(Intent.ACTION_VIEW);

		openIntent.setDataAndType(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
				? FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", file)
				: Uri.fromFile(file), transactionObject.fileMimeType)
				.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

		notification.setChannelId(NOTIFICATION_CHANNEL_LOW)
				.setSmallIcon(android.R.drawable.stat_sys_download_done)
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

		notification.setChannelId(NOTIFICATION_CHANNEL_LOW)
				.setSmallIcon(android.R.drawable.stat_sys_download_done)
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

		notification.setChannelId(NOTIFICATION_CHANNEL_HIGH)
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.setContentTitle(mContext.getString(R.string.text_error))
				.setContentText(mContext.getString(R.string.mesg_fileReceiveError, transactionObject.friendlyName))
				.setAutoCancel(true)
				.setDefaults(getNotificationSettings())
				.setPriority(NotificationCompat.PRIORITY_HIGH)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, transactionObject.groupId), 0));

		return notification.show();
	}

	public DynamicNotification notifyPrepareFiles(TransactionObject.Group group)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, group.groupId);

		Intent cancelIntent = new Intent(mContext, CommunicationService.class)
				.setAction(CommunicationService.ACTION_CANCEL_INDEXING)
				.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId())
				.putExtra(CommunicationService.EXTRA_GROUP_ID, group.groupId);

		PendingIntent negativeIntent = PendingIntent.getService(mContext, AppUtils.getUniqueNumber(), cancelIntent, 0);

		notification.setChannelId(NOTIFICATION_CHANNEL_LOW)
				.setSmallIcon(android.R.drawable.stat_sys_download)
				.setContentTitle(mContext.getString(R.string.text_preparingFiles))
				.setContentText(mContext.getString(R.string.text_savingDetails))
				.setAutoCancel(false)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.butn_cancel), negativeIntent)
				.setContentIntent(PendingIntent.getActivity(mContext, AppUtils.getUniqueNumber(), new Intent(mContext, TransactionActivity.class)
						.setAction(TransactionActivity.ACTION_LIST_TRANSFERS)
						.putExtra(TransactionActivity.EXTRA_GROUP_ID, group.groupId), 0));

		return notification.show();
	}

	public DynamicNotification notifyStuckThread(TransactionObject transaction)
	{
		boolean isIncoming = TransactionObject.Type.INCOMING.equals(transaction.type);

		DynamicNotification notification = new DynamicNotification(mContext, mManager, transaction.groupId);
		Intent killIntent = new Intent(mContext, isIncoming ? ServerService.class : ClientService.class).setAction(TransactionService.ACTION_CANCEL_KILL);

		notification.setChannelId(NOTIFICATION_CHANNEL_LOW)
				.setSmallIcon(android.R.drawable.stat_sys_warning)
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
