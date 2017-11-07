package com.genonbeta.TrebleShot.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.JsonResponseHandler;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.NetworkDeviceInfoLoader;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.TrebleShot.util.TransactionObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.Socket;
import java.util.UUID;

public class CommunicationService extends Service
{
	public static final String TAG = "CommunicationService";

	public static final String ACTION_FILE_TRANSFER = "com.genonbeta.TrebleShot.action.FILE_TRANSFER";
	public static final String ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.action.CLIPBOARD";
	public static final String ACTION_IP = "com.genonbeta.TrebleShot.action.IP";

	public static final String EXTRA_DEVICE_ID = "extraDeviceId";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_GROUP_ID = "extraGroupId";
	public static final String EXTRA_IS_ACCEPTED = "extraAccepted";
	public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";

	private CommunicationServer mCommunicationServer = new CommunicationServer();
	private NetworkDeviceInfoLoader mInfoLoader = new NetworkDeviceInfoLoader();
	private NotificationUtils mNotification;
	private String mReceivedClipboardIndex;
	private AccessDatabase mDatabase;

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
		mDatabase = new AccessDatabase(this);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			Log.d(TAG, "onStart() : action = " + intent.getAction());

		if (intent != null) {
			if (ACTION_FILE_TRANSFER.equals(intent.getAction())) {
				final int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				final boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);

				mNotification.cancel(notificationId);

				try {
					final NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

					TransactionObject.Group transactionGroup = new TransactionObject.Group(groupId);
					mDatabase.reconstruct(transactionGroup);

					NetworkDevice networkDevice = new NetworkDevice(transactionGroup.deviceId);
					mDatabase.reconstruct(networkDevice);

					NetworkDevice.Connection connection = new NetworkDevice.Connection(transactionGroup.deviceId, transactionGroup.connectionAdapter);
					mDatabase.reconstruct(connection);

					CoolCommunication.Messenger.send(connection.ipAddress, AppConfig.COMMUNATION_SERVER_PORT, null,
							new JsonResponseHandler()
							{
								@Override
								public void onJsonMessage(Socket socket, com.genonbeta.CoolSocket.CoolCommunication.Messenger.Process process, JSONObject json)
								{
									try {
										json.put(Keyword.SERIAL, localDevice.deviceId);
										json.put(Keyword.REQUEST, Keyword.REQUEST_RESPONSE);
										json.put(Keyword.GROUP_ID, groupId);
										json.put(Keyword.IS_ACCEPTED, isAccepted);

										Log.d(TAG, "We pushed the results hopefully: " + isAccepted);
									} catch (JSONException e) {
										e.printStackTrace();
									}
								}
							}
					);

					Log.d(TAG, "About to send the result. isAccepted: " + isAccepted);

					if (isAccepted)
						startService(new Intent(this, ServerService.class)
								.setAction(ServerService.ACTION_START_RECEIVING)
								.putExtra(EXTRA_GROUP_ID, groupId));
					else
						mDatabase.remove(transactionGroup);
				} catch (Exception e) {
					e.printStackTrace();

					mNotification.showToast(R.string.mesg_somethingWentWrong);
				}

			} else if (ACTION_IP.equals(intent.getAction())) {
				String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
				boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				NetworkDevice device = new NetworkDevice(deviceId);

				try {
					mDatabase.reconstruct(device);
					device.isRestricted = !isAccepted;
					mDatabase.publish(device);
				} catch (Exception e) {
					e.printStackTrace();
					return START_NOT_STICKY;
				}
			} else if (ACTION_CLIPBOARD.equals(intent.getAction()) && intent.hasExtra(EXTRA_CLIPBOARD_ACCEPTED)) {
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotification.cancel(notificationId);

				if (intent.getBooleanExtra(EXTRA_CLIPBOARD_ACCEPTED, false)) {
					((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("receivedText", mReceivedClipboardIndex));
					Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show();
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

		@SuppressLint("HardwareIds")
		@Override
		public void onJsonMessage(Socket socket, JSONObject receivedMessage, JSONObject response, String clientIp)
		{
			try {
				if (receivedMessage != null)
					Log.d(TAG, "receivedMessage = " + receivedMessage.toString());

				JSONObject deviceInformation = new JSONObject();
				JSONObject appInfo = new JSONObject();

				boolean result = false;
				boolean shouldContinue = false;

				PackageInfo packageInfo = getPackageManager().getPackageInfo(getApplicationInfo().packageName, 0);

				appInfo.put(Keyword.VERSION_CODE, packageInfo.versionCode);
				appInfo.put(Keyword.VERSION_NAME, packageInfo.versionName);

				NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

				deviceInformation.put(Keyword.SERIAL, localDevice.deviceId);
				deviceInformation.put(Keyword.BRAND, localDevice.brand);
				deviceInformation.put(Keyword.MODEL, localDevice.model);
				deviceInformation.put(Keyword.USER, localDevice.user);

				response.put(Keyword.APP_INFO, appInfo);
				response.put(Keyword.DEVICE_INFO, deviceInformation);

				if (receivedMessage.has(Keyword.SERIAL)) {
					String serialNumber = receivedMessage.getString(Keyword.SERIAL);
					NetworkDevice device = new NetworkDevice(serialNumber);

					try {
						mDatabase.reconstruct(device);

						if (!device.isRestricted)
							shouldContinue = true;
					} catch (Exception e1) {
						e1.printStackTrace();

						device.isRestricted = false;
						device = mInfoLoader.startLoading(true, mDatabase, clientIp);

						mDatabase.publish(device);
						mNotification.notifyConnectionRequest(device);

						shouldContinue = false;
					}

					if (shouldContinue && receivedMessage.has(Keyword.REQUEST)) {
						NetworkDevice.Connection connection = new NetworkDevice.Connection(clientIp);

						try {
							mDatabase.reconstruct(connection);
						} catch (Exception e) {
							connection.adapterName = Keyword.UNKNOWN_INTERFACE;
						}

						connection.deviceId = device.deviceId;
						mDatabase.publish(connection);

						switch (receivedMessage.getString(Keyword.REQUEST)) {
							case (Keyword.REQUEST_TRANSFER):
								if (receivedMessage.has(Keyword.FILES_INDEX) && receivedMessage.has(Keyword.GROUP_ID)) {
									String jsonIndex = receivedMessage.getString(Keyword.FILES_INDEX);
									JSONArray jsonArray = new JSONArray(jsonIndex);

									int count = 0;
									int groupId = receivedMessage.getInt(Keyword.GROUP_ID);

									TransactionObject.Group group = new TransactionObject.Group(groupId, device.deviceId, connection.adapterName);
									TransactionObject transactionObject = null;

									mDatabase.publish(group);


									for (int i = 0; i < jsonArray.length(); i++) {
										if (!(jsonArray.get(i) instanceof JSONObject))
											continue;

										JSONObject requestIndex = jsonArray.getJSONObject(i);

										if (requestIndex != null && requestIndex.has(Keyword.FILE_NAME) && requestIndex.has(Keyword.FILE_SIZE) && requestIndex.has(Keyword.FILE_MIME) && requestIndex.has(Keyword.REQUEST_ID)) {
											count++;
											transactionObject = new TransactionObject(
													requestIndex.getInt(Keyword.REQUEST_ID),
													groupId,
													requestIndex.getString(Keyword.FILE_NAME),
													"." + UUID.randomUUID() + ".tshare",
													requestIndex.getString(Keyword.FILE_MIME),
													requestIndex.getLong(Keyword.FILE_SIZE),
													TransactionObject.Type.INCOMING);

											mDatabase.publish(transactionObject);
										}
									}

									if (transactionObject != null && count > 0) {
										result = true;
										mNotification.notifyTransferRequest(transactionObject, device, count);
									}
								}
								break;
							case (Keyword.REQUEST_RESPONSE):
								if (receivedMessage.has(Keyword.GROUP_ID)) {
									int groupId = receivedMessage.getInt(Keyword.GROUP_ID);
									boolean isAccepted = receivedMessage.getBoolean(Keyword.IS_ACCEPTED);

									if (!isAccepted)
										mDatabase.remove(new TransactionObject.Group(groupId));

									result = true;
								}
								break;
							case (Keyword.REQUEST_SERVER_READY):
								if (receivedMessage.has(Keyword.REQUEST_ID) && receivedMessage.has(Keyword.GROUP_ID) && receivedMessage.has(Keyword.SOCKET_PORT)) {
									int requestId = receivedMessage.getInt(Keyword.REQUEST_ID);
									int groupId = receivedMessage.getInt(Keyword.GROUP_ID);
									int socketPort = receivedMessage.getInt(Keyword.SOCKET_PORT);

									TransactionObject.Group group = new TransactionObject.Group(groupId);
									mDatabase.reconstruct(group);

									try {
										TransactionObject transactionObject = new TransactionObject(requestId);
										mDatabase.reconstruct(transactionObject);

										transactionObject.accessPort = socketPort;

										if (receivedMessage.has(Keyword.SKIPPED_BYTES))
											transactionObject.skippedBytes = receivedMessage.getInt(Keyword.SKIPPED_BYTES);

										mDatabase.publish(transactionObject);

										if (!group.connectionAdapter.equals(connection.adapterName)) {
											group.connectionAdapter = connection.adapterName;
											mDatabase.publish(group);
										}

										startService(new Intent(getApplicationContext(), ClientService.class)
												.setAction(ClientService.ACTION_SEND)
												.putExtra(EXTRA_REQUEST_ID, requestId));

										result = true;
									} catch (Exception e) {
										response.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);
									}
								}

								break;
							case (Keyword.REQUEST_CLIPBOARD):
								if (receivedMessage.has(Keyword.CLIPBOARD_TEXT)) {
									mReceivedClipboardIndex = receivedMessage.getString(Keyword.CLIPBOARD_TEXT);
									mNotification.notifyClipboardRequest(device, mReceivedClipboardIndex);

									result = true;
								}
								break;
						}
					}
				}

				response.put(Keyword.RESULT, result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void onError(Exception exception)
		{
		}
	}
}
