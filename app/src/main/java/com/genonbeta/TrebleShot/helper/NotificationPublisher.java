package com.genonbeta.TrebleShot.helper;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TrebleShotActivity;
import com.genonbeta.TrebleShot.receiver.DialogEventReceiver;
import com.genonbeta.TrebleShot.service.ClientService;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.service.ServerService;

import java.io.File;
import android.inputmethodservice.*;

public class NotificationPublisher
{
	public static final String TAG = "NotificationPublisher";
	public static final String EXTRA_NOTIFICATION_ID = "notificationId";
	public static final int NOTIFICATION_SERVICE_STARTED = 232434;
	public static final int NOTIFICATION_OPPOSITE_DEVICE_PING = 232438;
	public static final int NOTIFICATION_ID_RECEIVED = 20000;
	public static final int NOTIFICATION_ID_SENT = 20001;
	public static final int NOTIFICATION_ID_RECEIVING = 20002;
	public static final int NOTIFICATION_ID_RECEIVE_ERROR = 20003;
	
	private Context mContext;
	private NotificationManager mManager;
	private SharedPreferences mPreferences;
	
	public NotificationPublisher(Context context)
	{
		this.mContext = context;
		this.mManager = (NotificationManager) mContext.getSystemService(Service.NOTIFICATION_SERVICE);
		this.mPreferences = PreferenceManager.getDefaultSharedPreferences(this.mContext);
	}
	
	public NotificationManager getNotificationManager()
	{
		return this.mManager;
	}
	
	public SharedPreferences getSharedPreferences()
	{
		return this.mPreferences;
	}
	
	public void notifyConnectionRequest(String clientIp)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		int uniqueNotificationId = ApplicationHelper.getUniqueNumber();
		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);

		acceptIntent.setAction(CommunicationService.ACTION_ALLOW_IP);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, clientIp);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, uniqueNotificationId);

		Intent rejectIntent = ((Intent) acceptIntent.clone());
		rejectIntent.setAction(CommunicationService.ACTION_REJECT_IP);

		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), acceptIntent, 0);
		PendingIntent negativeIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), rejectIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.connection_permission));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, mContext.getString(R.string.allow_device_to_connect));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		Log.i(TAG, "clientIp = " + clientIp);

		builder.setSmallIcon(android.R.drawable.stat_notify_error)
			.setContentTitle(mContext.getString(R.string.connection_permission))
			.setContentText(mContext.getString(R.string.allow_device_to_connect))
			.setContentInfo(clientIp)
			.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
			.setDefaults(this.getNotificationDefaults())
			.setDeleteIntent(negativeIntent)
			.addAction(android.R.drawable.ic_menu_send, mContext.getString(R.string.accept), positiveIntent)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.reject), negativeIntent)
			.setTicker(mContext.getString(R.string.connection_permission));

		mManager.notify(uniqueNotificationId, builder.build());
	}
	
	public void notifyTransferRequest(int acceptId, NetworkDevice device, AwaitedFileReceiver receiver, boolean halfRestriction)
	{
		int uniqueNotificationId = ApplicationHelper.getUniqueNumber();
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);
				
		acceptIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER_ACCEPT);
		
		acceptIntent.putExtra(CommunicationService.EXTRA_ACCEPT_ID, acceptId);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, receiver.ip);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, uniqueNotificationId);
		
		Intent rejectIntent = ((Intent) acceptIntent.clone());
		rejectIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER_REJECT);
		
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
		
		builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
			.setContentTitle(mContext.getString(R.string.receive_the_file_que))
			.setContentText(receiver.fileName)
			.setContentInfo(device.user)
			.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
			.setDefaults(this.getNotificationDefaults())
			.setDeleteIntent(negativeIntent)
			.addAction(android.R.drawable.ic_menu_send, mContext.getString(acceptText), positiveIntent)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(rejectText), negativeIntent)
			.setTicker(mContext.getString(R.string.receive_the_file_que));
			
		mManager.notify(uniqueNotificationId, builder.build());
	}
	
	public void notifyMultiTransferRequest(int numberOfFiles, int acceptId, NetworkDevice device, boolean halfRestriction)
	{
		int uniqueNotificationId = ApplicationHelper.getUniqueNumber();
		
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		Intent acceptIntent = new Intent(mContext, CommunicationService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);
		
		acceptIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER_ACCEPT);
		acceptIntent.putExtra(CommunicationService.EXTRA_ACCEPT_ID, acceptId);
		acceptIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, device.ip);
		acceptIntent.putExtra(EXTRA_NOTIFICATION_ID, uniqueNotificationId);
		
		Intent rejectIntent = ((Intent) acceptIntent.clone());
		rejectIntent.setAction(CommunicationService.ACTION_FILE_TRANSFER_REJECT);

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
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, mContext.getString(R.string.multi_transfer_que, numberOfFiles));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_NEGATIVE_INTENT, negativeIntent);

		builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
			.setContentTitle(mContext.getString(R.string.receive_the_file_que))
			.setContentText(mContext.getString(R.string.multi_transfer_que, numberOfFiles))
			.setContentInfo(device.user)
			.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
			.setDefaults(this.getNotificationDefaults())
			.setDeleteIntent(negativeIntent)
			.addAction(android.R.drawable.ic_menu_send, mContext.getString(acceptText), positiveIntent)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(rejectText), negativeIntent)
			.setTicker(mContext.getString(R.string.receive_the_file_que));
		
		mManager.notify(uniqueNotificationId, builder.build());
	}
	
	public Notification notifyServiceStarted()
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);

		builder.setSmallIcon(R.mipmap.p2p)
			.setContentTitle(mContext.getString(R.string.app_name))
			.setContentText(mContext.getString(R.string.ongoing))
			.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, TrebleShotActivity.class), 0))
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.stop_service), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, CommunicationService.class).setAction(CommunicationService.ACTION_STOP_SERVICE), 0))
			.addAction(android.R.drawable.ic_menu_revert, mContext.getString(R.string.lock), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, CommunicationService.class).setAction(CommunicationService.ACTION_STOP_SERVICE).putExtra(CommunicationService.EXTRA_SERVICE_LOCK_REQUEST, true), 0));
		
		return builder.getNotification();
	}
	
	public NotificationCompat.Builder notifyFileSending(AwaitedFileSender sender, NetworkDevice device, int progress)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		Intent cancelIntent = new Intent(mContext, ClientService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);
		
		cancelIntent.setAction(ClientService.ACTION_CANCEL_SENDING);
		cancelIntent.putExtra(CommunicationService.EXTRA_REQUEST_ID, sender.requestId);	
		cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, sender.requestId);
		
		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), cancelIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.cancel_sending));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, sender.file.getName());
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		
		builder.setSmallIcon(android.R.drawable.stat_sys_upload)
			.setContentTitle(sender.file.getName())
			.setContentText(mContext.getString(R.string.sending_msg))
			.setContentInfo(device.user)
			.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
			.setProgress(100, progress, false)
			.setOngoing(true)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.cancel_sending), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), cancelIntent, 0));
			
		Log.d(TAG, "sending(): requestId = " + sender.requestId);
			
		mManager.notify(sender.requestId, builder.build());
		
		return builder;
	}
	
	public NotificationCompat.Builder notifyFileReceiving(AwaitedFileReceiver receiver, NetworkDevice device, int progress)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		Intent cancelIntent = new Intent(mContext, ServerService.class);
		Intent dialogIntent = new Intent(mContext, DialogEventReceiver.class);
		
		cancelIntent.setAction(ServerService.ACTION_CANCEL_RECEIVING);
		cancelIntent.putExtra(CommunicationService.EXTRA_DEVICE_IP, receiver.ip);
		cancelIntent.putExtra(CommunicationService.EXTRA_REQUEST_ID, receiver.requestId);
		cancelIntent.putExtra(CommunicationService.EXTRA_ACCEPT_ID, receiver.acceptId);
		cancelIntent.putExtra(EXTRA_NOTIFICATION_ID, NOTIFICATION_ID_RECEIVING);
		
		PendingIntent positiveIntent = PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), cancelIntent, 0);

		dialogIntent.setAction(DialogEventReceiver.ACTION_DIALOG);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_TITLE, mContext.getString(R.string.cancel_receiving));
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_MESSAGE, receiver.fileName);
		dialogIntent.putExtra(DialogEventReceiver.EXTRA_POSITIVE_INTENT, positiveIntent);
		
		builder.setSmallIcon(android.R.drawable.stat_sys_download)
			.setContentTitle(receiver.fileName)
			.setContentText(mContext.getString(R.string.receiving_msg))
			.setContentInfo(device.user)
			.setContentIntent(PendingIntent.getBroadcast(mContext, ApplicationHelper.getUniqueNumber(), dialogIntent, 0))
			.setProgress(100, progress, false)
			.setOngoing(true)
			.addAction(android.R.drawable.ic_menu_close_clear_cancel, mContext.getString(R.string.cancel_receiving), PendingIntent.getService(mContext, ApplicationHelper.getUniqueNumber(), cancelIntent, 0));

		Log.d(TAG, "receiving(): requestId = " + receiver.requestId);
		
		mManager.notify(NOTIFICATION_ID_RECEIVING, builder.build());

		return builder;
	}
	
	public void notifyOppositeDevicePing(NetworkDevice device)
	{
		NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext);
		
		builder.setSmallIcon(android.R.drawable.stat_notify_chat)
			.setContentTitle(mContext.getString(R.string.poke_notify))
			.setContentText(mContext.getString(R.string.sent_signal_msg))
			.setContentInfo(device.user)
			.setAutoCancel(true)
			.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), new Intent(mContext, TrebleShotActivity.class), 0))
			.setDefaults(this.getNotificationDefaults())
			.setTicker(mContext.getString(R.string.poke_notify));
		
		mManager.notify(NOTIFICATION_OPPOSITE_DEVICE_PING, builder.build());
	}
	
	public void notifyFileReceived(AwaitedFileReceiver receiver, File file, NetworkDevice device)
	{
		Notification.Builder builder = new Notification.Builder(mContext);
		Intent openIntent = new Intent(Intent.ACTION_VIEW);
		
		openIntent.setDataAndType(Uri.fromFile(file), FileUtils.getFileContentType(file.getAbsolutePath()));
		
		builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
			.setContentTitle(receiver.fileName)
			.setContentText(mContext.getString(R.string.file_received_msg))
			.setContentInfo(device.user)
			.setAutoCancel(true)
			.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), openIntent, 0));

		mManager.notify(NOTIFICATION_ID_RECEIVED, builder.build());
	}
	
	public void notifyFileReceivedMulti(int numberOfFiles)
	{
		Notification.Builder builder = new Notification.Builder(mContext);
		Intent openIntent = new Intent(mContext, TrebleShotActivity.class).setAction(TrebleShotActivity.OPEN_RECEIVED_FILES_ACTION);
		
		builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
			.setContentTitle(mContext.getString(R.string.multiple_receive))
			.setContentText(mContext.getString(R.string.multiple_receive_done_summary, numberOfFiles))
			.setAutoCancel(true)
			.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), openIntent, 0));
			
		mManager.notify(NOTIFICATION_ID_RECEIVED, builder.build());
	}
	
	public void notifyReceiveError(String fileName)
	{
		Notification.Builder builder = new Notification.Builder(mContext);
		Intent openIntent = new Intent(mContext, TrebleShotActivity.class).setAction(TrebleShotActivity.OPEN_RECEIVED_FILES_ACTION);

		builder.setSmallIcon(android.R.drawable.stat_notify_error)
			.setContentTitle(mContext.getString(R.string.error))
			.setContentText(mContext.getString(R.string.file_receiving_error_msg, fileName))
			.setAutoCancel(true)
			.setContentIntent(PendingIntent.getActivity(mContext, ApplicationHelper.getUniqueNumber(), openIntent, 0));

		mManager.notify(NOTIFICATION_ID_RECEIVE_ERROR, builder.build());
	}
	
	public int getNotificationDefaults()
	{
		int makeSound = (mPreferences.getBoolean("notification_sound", false)) ? NotificationCompat.DEFAULT_SOUND : 0;
		int vibrate = (mPreferences.getBoolean("notification_vibrate", false)) ? NotificationCompat.DEFAULT_VIBRATE : 0;
		int light = (mPreferences.getBoolean("notification_light", false)) ? NotificationCompat.DEFAULT_LIGHTS : 0;
		
		return makeSound|vibrate|light;
	}
	
	public void notify(int notificationId, Notification notification)
	{
		mManager.notify(notificationId, notification);
	}
	
	public void makeToast(String toastText)
	{
		Toast.makeText(mContext, toastText, Toast.LENGTH_SHORT).show();
	}
	
	public void makeToast(int toastTextRes)
	{
		Toast.makeText(mContext, toastTextRes, Toast.LENGTH_SHORT).show();
	}
	
	public void makeToast(String text, int lenght)
	{
		Toast.makeText(mContext, text, lenght).show();
	}
	
	public void makeToast(int textRes, int lenght)
	{
		Toast.makeText(mContext, textRes, lenght).show();
	}
	
	public void cancelNotification(int notificationId)
	{
		mManager.cancel(notificationId);
	}
}
