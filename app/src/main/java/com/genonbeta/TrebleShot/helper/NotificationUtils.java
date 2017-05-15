package com.genonbeta.TrebleShot.helper;

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
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TrebleShotActivity;
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
	public static final int NOTIFICATION_ID_SERVICE = 232434;
	public static final int NOTIFICATION_ID_PING = 232438;

	private Context mContext;
	private NotificationManagerCompat mManager;
	private SharedPreferences mPreferences;

	public NotificationUtils(Context context)
	{
		this.mContext = context;
		this.mManager = NotificationManagerCompat.from(context);
		this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
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

	public DynamicNotification notifyService()
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, NOTIFICATION_ID_SERVICE);

		notification.setSmallIcon(R.mipmap.p2p)
				.setContentTitle(mContext.getString(R.string.app_name))
				.setContentText(mContext.getString(R.string.ongoing))
				.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, TrebleShotActivity.class), 0))
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.stop_service), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, CommunicationService.class).setAction(CommunicationService.ACTION_STOP_SERVICE), 0))
				.addAction(android.R.drawable.ic_menu_revert, mContext.getString(R.string.lock), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, CommunicationService.class).setAction(CommunicationService.ACTION_STOP_SERVICE).putExtra(CommunicationService.EXTRA_SERVICE_LOCK_REQUEST, true), 0));

		return notification.show();
	}

	public DynamicNotification notifyConnectionRequest(String clientIp)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, ApplicationHelper.getUniqueNumber());

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_IP);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, clientIp);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.connection_permission));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, mContext.getString(R.string.allow_device_to_connect));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		Log.i(TAG, "clientIp = " + clientIp);

		notification.setSmallIcon(android.R.drawable.stat_notify_error)
				.setContentTitle(mContext.getString(R.string.connection_permission))
				.setContentText(mContext.getString(R.string.allow_device_to_connect))
				.setContentInfo(clientIp)
				.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(R.string.accept), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.reject), negativeIntent)
				.setTicker(mContext.getString(R.string.connection_permission));

		return notification.show();
	}

	public DynamicNotification notifyTransferRequest(NetworkDevice device, boolean halfRestriction, AwaitedFileReceiver receiver)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, receiver.acceptId);

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER);

		acceptIntent.putExtra(CommunicationService.EXTRA_ACCEPT_ID, receiver.acceptId);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, receiver.ip);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());
		acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

		int acceptText;
		int rejectText;

		if (halfRestriction)
		{
			acceptIntent.putExtra(CommunicationService.EXTRA_HALF_RESTRICT, true);
			rejectIntent.putExtra(CommunicationService.EXTRA_HALF_RESTRICT, true);

			acceptText = R.string.receive_restricted;
			rejectText = R.string.reject_restricted;
		}
		else
		{
			acceptText = R.string.accept;
			rejectText = R.string.reject;
		}

		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.receive_the_file_que));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, receiver.fileName);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		Log.i(TAG, "clientIp = " + device.ip + "; fileName = " + receiver.fileName + " ; fileType = " + receiver.fileMimeType + " ; requestId = " + receiver.requestId);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.receive_the_file_que))
				.setContentText(receiver.fileName)
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(acceptText), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(rejectText), negativeIntent)
				.setTicker(mContext.getString(R.string.receive_the_file_que))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyTransferRequest(NetworkDevice device, boolean halfRestriction, int acceptId, int numberOfFiles)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, acceptId);

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER);
		acceptIntent.putExtra(CommunicationService.EXTRA_ACCEPT_ID, acceptId);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, device.ip);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_IS_ACCEPTED, false);

		int acceptText;
		int rejectText;

		if (halfRestriction)
		{
			acceptIntent.putExtra(CommunicationService.EXTRA_HALF_RESTRICT, true);
			rejectIntent.putExtra(CommunicationService.EXTRA_HALF_RESTRICT, true);

			acceptText = R.string.receive_restricted;
			rejectText = R.string.reject_restricted;
		}
		else
		{
			acceptText = R.string.accept;
			rejectText = R.string.reject;
		}

		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.receive_the_file_que));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, mContext.getString(R.string.multi_transfer_que, String.valueOf(numberOfFiles)));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.receive_the_file_que))
				.setContentText(mContext.getString(R.string.multi_transfer_que, String.valueOf(numberOfFiles)))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(acceptText), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(rejectText), negativeIntent)
				.setTicker(mContext.getString(R.string.receive_the_file_que))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyFileSending(AwaitedFileSender sender, NetworkDevice device)
	{
		return notifyFileTransaction(sender, device, false);
	}

	public DynamicNotification notifyFileReceiving(AwaitedFileReceiver receiver, NetworkDevice device)
	{
		return notifyFileTransaction(receiver, device, true);
	}

	private DynamicNotification notifyFileTransaction(AwaitedTransaction transaction, NetworkDevice device, boolean isIncoming)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, transaction.acceptId);
		Intent cancelIntent = new Intent(mContext, isIncoming ? ServerService.class : ClientService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		cancelIntent.setAction(AbstractTransactionService.ACTION_CANCEL_JOB);
		cancelIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, transaction.ip);
		cancelIntent.putExtra(CommunicationService.EXTRA_REQUEST_ID, transaction.requestId);
		cancelIntent.putExtra(CommunicationService.EXTRA_ACCEPT_ID, transaction.acceptId);
		cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), cancelIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(isIncoming ? R.string.cancel_receiving : R.string.cancel_sending));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, transaction.fileName);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);

		notification.setSmallIcon(isIncoming ? android.R.drawable.stat_sys_download : android.R.drawable.stat_sys_upload)
				.setContentTitle(transaction.fileName)
				.setContentText(mContext.getString(isIncoming ? R.string.receiving_msg : R.string.sending_msg))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
				.setOngoing(true)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(isIncoming ? R.string.cancel_receiving : R.string.cancel_sending), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), cancelIntent, 0));

		return notification.show();
	}

	public DynamicNotification notifyClipboardRequest(NetworkDevice device, CharSequence text)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, ApplicationHelper.getUniqueNumber());

		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_CLIPBOARD);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, device.ip);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, notification.getNotificationId());

		Intent rejectIntent = ((Intent) acceptIntent.clone());

		acceptIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, true);
		rejectIntent.putExtra(CommunicationService.EXTRA_CLIPBOARD_ACCEPTED, false);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.copy_to_clipboard_question));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, text);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.received_text))
				.setContentText(mContext.getString(R.string.copy_to_clipboard_question))
				.setContentInfo(device.user)
				.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
				.setDefaults(getNotificationSettings())
				.setDeleteIntent(negativeIntent)
				.addAction(android.R.drawable.ic_menu_send, mContext.getString(R.string.accept), positiveIntent)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.reject), negativeIntent)
				.setTicker(mContext.getString(R.string.received_text_info))
				.setPriority(NotificationCompat.PRIORITY_HIGH);

		return notification.show();
	}

	public DynamicNotification notifyFileReceived(AwaitedFileReceiver receiver, File file, NetworkDevice device)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, receiver.acceptId);
		Intent openIntent = new Intent(Intent.ACTION_VIEW);

		if (Build.VERSION.SDK_INT > 22)
			openIntent.setDataAndType(FileProvider.getUriForFile(mContext, mContext.getApplicationContext().getPackageName() + ".provider", file), FileUtils.getFileContentType(file.getAbsolutePath()))
					.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		else
			openIntent.setDataAndType(Uri.fromFile(file), FileUtils.getFileContentType(file.getAbsolutePath()));

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(receiver.fileName)
				.setContentText(mContext.getString(R.string.file_received_msg))
				.setContentInfo(device.user)
				.setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), openIntent, 0));

		return notification.show();
	}

	public DynamicNotification notifyFileReceived(AwaitedFileReceiver receiver, int numberOfFiles)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, receiver.acceptId);

		notification.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setContentTitle(mContext.getString(R.string.multiple_receive))
				.setContentText(mContext.getString(R.string.multiple_receive_done_summary, String.valueOf(numberOfFiles)))
				.setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, TrebleShotActivity.class)
						.setAction(TrebleShotActivity.ACTION_OPEN_RECEIVED_FILES), 0));

		return notification.show();
	}

	public DynamicNotification notifyPing(NetworkDevice device)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, NOTIFICATION_ID_PING);

		notification.setSmallIcon(android.R.drawable.stat_notify_chat)
				.setContentTitle(mContext.getString(R.string.poke_notify))
				.setContentText(mContext.getString(R.string.sent_signal_msg))
				.setContentInfo(device.user)
				.setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, TrebleShotActivity.class), 0))
				.setDefaults(getNotificationSettings())
				.setTicker(mContext.getString(R.string.poke_notify));

		return notification.show();
	}

	public DynamicNotification notifyReceiveError(AwaitedFileReceiver receiver)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, receiver.acceptId);
		Intent openIntent = new Intent(mContext, TrebleShotActivity.class)
				.setAction(TrebleShotActivity.ACTION_OPEN_ONGOING_LIST);

		notification.setSmallIcon(android.R.drawable.stat_notify_error)
				.setContentTitle(mContext.getString(R.string.error))
				.setContentText(mContext.getString(R.string.file_receiving_error_msg, receiver.fileName))
				.setAutoCancel(true)
				.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), openIntent, 0));

		return notification.show();
	}

	public DynamicNotification notifyStuckThread(AwaitedTransaction transaction, boolean isIncoming)
	{
		DynamicNotification notification = new DynamicNotification(mContext, mManager, transaction.acceptId);
		Intent killIntent = new Intent(mContext, isIncoming ? ServerService.class : ClientService.class).setAction(AbstractTransactionService.ACTION_CANCEL_KILL);

		// TODO: 4/29/17 Don't leave it like this
		notification.setSmallIcon(android.R.drawable.stat_sys_warning)
				.setOngoing(true)
				.setContentTitle(mContext.getString(R.string.stop_progress))
				.setContentText(mContext.getString(R.string.cancel_progress))
				.setProgress(0, 0, true)
				.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.kill_immediately), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), killIntent, 0));

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
