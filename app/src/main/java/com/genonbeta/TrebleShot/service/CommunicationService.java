package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolCommunication;
import com.genonbeta.CoolSocket.CoolJsonCommunication;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.DeviceRegistry;
import com.genonbeta.TrebleShot.database.Transaction;
import com.genonbeta.TrebleShot.helper.AwaitedFileReceiver;
import com.genonbeta.TrebleShot.helper.AwaitedFileSender;
import com.genonbeta.TrebleShot.helper.JsonResponseHandler;
import com.genonbeta.TrebleShot.helper.NetworkDevice;
import com.genonbeta.TrebleShot.helper.NetworkDeviceInfoLoader;
import com.genonbeta.TrebleShot.helper.NotificationUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;

public class CommunicationService extends Service
{
	public static final String TAG = "CommunicationService";

	public static final String ACTION_FILE_TRANSFER = "com.genonbeta.TrebleShot.action.FILE_TRANSFER";
	public static final String ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.action.CLIPBOARD";
	public static final String ACTION_IP = "com.genonbeta.TrebleShot.action.IP";

	public static final String EXTRA_DEVICE_IP = "extraDeviceIp";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_GROUP_ID = "extraGroupId";
	public static final String EXTRA_IS_ACCEPTED = "extraAccepted";
	public static final String EXTRA_HALF_RESTRICT = "extraHalfRestrict";
	public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";

	private CommunicationServer mCommunicationServer = new CommunicationServer();
	private NetworkDeviceInfoLoader mInfoLoader = new NetworkDeviceInfoLoader();
	private NotificationUtils mNotification;
	private SharedPreferences mPreferences;
	private String mReceivedClipboardIndex;
	private Transaction mTransaction;
	private DeviceRegistry mDeviceRegistry;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		if (!mCommunicationServer.start())
			stopSelf();

		mNotification = new NotificationUtils(this);
		mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
		mTransaction = new Transaction(this);
		mDeviceRegistry = new DeviceRegistry(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			Log.d(TAG, "onStart() : action = " + intent.getAction());

		if (intent != null)
		{
			if (ACTION_FILE_TRANSFER.equals(intent.getAction()))
			{
				final String ipAddress = intent.getStringExtra(EXTRA_DEVICE_IP);
				final int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				final boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);
				final boolean isHalfRestriction = intent.hasExtra(EXTRA_HALF_RESTRICT);

				mNotification.cancel(notificationId);

				if (!isHalfRestriction || isAccepted)
					mDeviceRegistry.updateRestriction(ipAddress, false);

				CoolCommunication.Messenger.send(ipAddress, AppConfig.COMMUNATION_SERVER_PORT, null,
						new JsonResponseHandler()
						{
							@Override
							public void onJsonMessage(Socket socket, com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json)
							{
								try
								{
									json.put(Keyword.REQUEST, Keyword.REQUEST_RESPONSE);
									json.put(Keyword.GROUP_ID, groupId);
									json.put(Keyword.IS_ACCEPTED, isAccepted);
								} catch (JSONException e)
								{
									e.printStackTrace();
								}
							}
						}
				);

				if (!mTransaction.transactionGroupExists(groupId))
				{
					mNotification.showToast(R.string.something_went_wrong);
					return START_NOT_STICKY;
				}

				if (isAccepted)
					startService(new Intent(this, ServerService.class)
							.setAction(ServerService.ACTION_START_RECEIVING)
							.putExtra(EXTRA_GROUP_ID, groupId));
				else
					mTransaction
							.edit()
							.removeTransactionGroup(groupId)
							.done();
			}
			else if (ACTION_IP.equals(intent.getAction()))
			{
				String ipAddress = intent.getStringExtra(EXTRA_DEVICE_IP);
				boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				if (!mDeviceRegistry.exists(ipAddress))
					return START_NOT_STICKY;

				mDeviceRegistry.updateRestriction(ipAddress, !isAccepted);
			}
			else if (ACTION_CLIPBOARD.equals(intent.getAction()) && intent.hasExtra(EXTRA_CLIPBOARD_ACCEPTED))
			{
				String ipAddress = intent.getStringExtra(EXTRA_DEVICE_IP);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				if (!mDeviceRegistry.exists(ipAddress))
					return START_NOT_STICKY;

				mDeviceRegistry.updateRestriction(ipAddress, false);

				if (intent.getBooleanExtra(EXTRA_CLIPBOARD_ACCEPTED, false))
				{
					((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("receivedText", mReceivedClipboardIndex));
					Toast.makeText(this, R.string.clipboard_text_copied, Toast.LENGTH_SHORT).show();
				}
			}
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mCommunicationServer.stop();
		stopForeground(true);
	}

	public class CommunicationServer extends CoolJsonCommunication
	{
		public CommunicationServer()
		{
			super(AppConfig.COMMUNATION_SERVER_PORT);
			setSocketTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);
		}

		@Override
		public void onJsonMessage(Socket socket, JSONObject receivedMessage, JSONObject response, String clientIp)
		{
			NetworkDevice device = new NetworkDevice(clientIp);

			try
			{
				if (receivedMessage != null)
					Log.d(TAG, "receivedMessage = " + receivedMessage.toString());

				JSONObject deviceInformation = new JSONObject();
				JSONObject appInfo = new JSONObject();
				boolean result = false;
				boolean shouldContinue = true;
				boolean halfRestriction = false;

				PackageInfo packageInfo = getPackageManager().getPackageInfo(getApplicationInfo().packageName, 0);

				appInfo.put(Keyword.VERSION_CODE, packageInfo.versionCode);
				appInfo.put(Keyword.VERSION_NAME, packageInfo.versionName);

				deviceInformation.put(Keyword.SERIAL, Build.SERIAL);
				deviceInformation.put(Keyword.BRAND, Build.BRAND);
				deviceInformation.put(Keyword.MODEL, Build.MODEL);
				deviceInformation.put(Keyword.USER, mPreferences.getString("device_name", Build.BOARD.toUpperCase()));

				response.put(Keyword.APP_INFO, appInfo);
				response.put(Keyword.DEVICE_INFO, deviceInformation);

				if (receivedMessage.has(Keyword.REQUEST) && !receivedMessage.getString(Keyword.REQUEST).equals(""))
					if (!mDeviceRegistry.exists(clientIp))
					{
						device.isRestricted = true;
						device = mInfoLoader.startLoading(true, mDeviceRegistry, device.ip);

						mDeviceRegistry.registerDevice(device);

						if (receivedMessage.getString(Keyword.REQUEST).equals(Keyword.REQUEST_TRANSFER))
						{
							shouldContinue = true;
							halfRestriction = true;
						}
						else
							mNotification.notifyConnectionRequest(device);
					}
					else
					{
						device = mDeviceRegistry.getNetworkDevice(clientIp);

						if (device.isRestricted)
							shouldContinue = false;
					}

				if (shouldContinue && receivedMessage.has(Keyword.REQUEST))
				{
					switch (receivedMessage.getString(Keyword.REQUEST))
					{
						case (Keyword.REQUEST_TRANSFER):
							if (receivedMessage.has(Keyword.FILES_INDEX) && receivedMessage.has(Keyword.GROUP_ID))
							{
								String jsonIndex = receivedMessage.getString(Keyword.FILES_INDEX);
								JSONArray jsonArray = new JSONArray(jsonIndex);

								int count = 0;
								int groupId = receivedMessage.getInt(Keyword.GROUP_ID);
								AwaitedFileReceiver heldReceiver = null;

								Transaction.EditingSession editingSession = mTransaction.edit();

								for (int i = 0; i < jsonArray.length(); i++)
								{
									if (!(jsonArray.get(i) instanceof JSONObject))
										continue;

									JSONObject requestIndex = jsonArray.getJSONObject(i);

									if (requestIndex != null && requestIndex.has(Keyword.FILE_NAME) && requestIndex.has(Keyword.FILE_SIZE) && requestIndex.has(Keyword.FILE_MIME) && requestIndex.has(Keyword.REQUEST_ID))
									{
										count++;
										heldReceiver = new AwaitedFileReceiver(device, requestIndex.getInt(Keyword.REQUEST_ID), groupId, requestIndex.getString(Keyword.FILE_NAME), requestIndex.getLong(Keyword.FILE_SIZE), requestIndex.getString(Keyword.FILE_MIME));
										editingSession.registerTransaction(heldReceiver);
									}
								}

								editingSession.done();

								if (heldReceiver != null && count > 0)
								{
									result = true;
									mDeviceRegistry.updateRestrictionByDeviceId(device, true);
									mNotification.notifyTransferRequest(halfRestriction, heldReceiver, device, count);
								}
							}
							break;
						case (Keyword.REQUEST_RESPONSE):
							if (receivedMessage.has(Keyword.GROUP_ID))
							{
								int groupId = receivedMessage.getInt(Keyword.GROUP_ID);
								boolean isAccepted = receivedMessage.getBoolean(Keyword.IS_ACCEPTED);

								if (!isAccepted)
									mTransaction
											.edit()
											.removeTransactionGroup(groupId)
											.done();

								result = true;
							}
							break;
						case (Keyword.REQUEST_SERVER_READY):
							if (receivedMessage.has(Keyword.REQUEST_ID) && receivedMessage.has(Keyword.GROUP_ID) && receivedMessage.has(Keyword.SOCKET_PORT))
							{
								int requestId = receivedMessage.getInt(Keyword.REQUEST_ID);
								int groupId = receivedMessage.getInt(Keyword.GROUP_ID);
								int socketPort = receivedMessage.getInt(Keyword.SOCKET_PORT);

								Transaction.EditingSession editingSession = mTransaction
										.edit()
										.updateAccessPort(requestId, socketPort);

								if (mTransaction.getAffectedRowCount() > 0)
								{
									if (receivedMessage.has(Keyword.FILE_SIZE))
									{
										ContentValues values = new ContentValues();
										values.put(Transaction.FIELD_TRANSFER_SIZE, receivedMessage.getLong(Keyword.FILE_SIZE));
										editingSession.updateTransaction(requestId, values);
									}

									AwaitedFileSender sender = new AwaitedFileSender(mTransaction.getTransaction(requestId));

									if (!clientIp.equals(sender.ip))
									{
										ContentValues values = new ContentValues();
										values.put(Transaction.FIELD_TRANSFER_USERIP, clientIp);
										editingSession.updateTransactionGroup(sender.groupId, values);
									}

									Intent starterIntent = new Intent(getApplicationContext(), ClientService.class)
											.setAction(ClientService.ACTION_SEND)
											.putExtra(EXTRA_REQUEST_ID, requestId);

									startService(starterIntent);
									result = true;
								}
								else if (mTransaction.transactionGroupExists(groupId))
									response.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);

								editingSession.done();
							}
							break;
						case (Keyword.REQUEST_CLIPBOARD):
							if (receivedMessage.has(Keyword.CLIPBOARD_TEXT))
							{
								mReceivedClipboardIndex = receivedMessage.getString(Keyword.CLIPBOARD_TEXT);

								mNotification.notifyClipboardRequest(device, mReceivedClipboardIndex);
								mDeviceRegistry.updateRestrictionByDeviceId(device, true);

								result = true;
							}
							break;
					}
				}

				response.put(Keyword.RESULT, result);
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		@Override
		protected void onError(Exception exception)
		{
		}
	}
}
