package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.NetworkDevice;
import com.genonbeta.TrebleShot.util.NetworkDeviceInfoLoader;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.TrebleShot.util.TransactionObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class CommunicationService extends Service
{
	public static final String TAG = "CommunicationService";

	public static final String ACTION_FILE_TRANSFER = "com.genonbeta.TrebleShot.action.FILE_TRANSFER";
	public static final String ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.action.CLIPBOARD";
	public static final String ACTION_CANCEL_INDEXING = "com.genonbeta.TrebleShot.action.CANCEL_INDEXING";
	public static final String ACTION_IP = "com.genonbeta.TrebleShot.action.IP";

	public static final String EXTRA_DEVICE_ID = "extraDeviceId";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_GROUP_ID = "extraGroupId";
	public static final String EXTRA_IS_ACCEPTED = "extraAccepted";
	public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";

	private CommunicationServer mCommunicationServer = new CommunicationServer();
	private NetworkDeviceInfoLoader mInfoLoader = new NetworkDeviceInfoLoader();
	private ArrayMap<Integer, Interrupter> mOngoingIndexList = new ArrayMap<>();
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

					final NetworkDevice.Connection connection = new NetworkDevice.Connection(transactionGroup.deviceId, transactionGroup.connectionAdapter);
					mDatabase.reconstruct(connection);

					CoolSocket.connect(new CoolSocket.Client.ConnectionHandler()
					{
						@Override
						public void onConnect(CoolSocket.Client connect)
						{
							try {
								JSONObject jsonObject = new JSONObject();

								jsonObject.put(Keyword.SERIAL, localDevice.deviceId);
								jsonObject.put(Keyword.REQUEST, Keyword.REQUEST_RESPONSE);
								jsonObject.put(Keyword.GROUP_ID, groupId);
								jsonObject.put(Keyword.IS_ACCEPTED, isAccepted);

								CoolSocket.ActiveConnection activeConnection = connect.connect(new InetSocketAddress(connection.ipAddress, AppConfig.COMMUNICATION_SERVER_PORT), AppConfig.DEFAULT_SOCKET_TIMEOUT);
								activeConnection.reply(jsonObject.toString());
							} catch (JSONException e) {
								e.printStackTrace();
							} catch (TimeoutException e) {
								e.printStackTrace();
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});

					if (isAccepted)
						startService(new Intent(this, ServerService.class)
								.setAction(ServerService.ACTION_START_RECEIVING)
								.putExtra(EXTRA_GROUP_ID, groupId));
					else
						mDatabase.remove(transactionGroup);

				} catch (Exception e) {
					e.printStackTrace();

					if (isAccepted)
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
			} else if (ACTION_CANCEL_INDEXING.equals(intent.getAction())) {
				final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				final int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);

				mNotification.cancel(notificationId);

				Interrupter interrupter = getOngoingIndexList().get(groupId);

				if (interrupter != null)
					interrupter.interrupt();
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

	public class CommunicationServer extends CoolSocket
	{
		public CommunicationServer()
		{
			super(AppConfig.COMMUNICATION_SERVER_PORT);
			setSocketTimeout(AppConfig.DEFAULT_SOCKET_LARGE_TIMEOUT);
		}

		@Override
		protected void onConnected(ActiveConnection activeConnection)
		{
			if (getConnectionCountByAddress(activeConnection.getAddress()) > 3)
				return;

			try {
				ActiveConnection.Response clientRequest = activeConnection.receive();
				JSONObject replyJSON = clientRequest.totalLength > 0 ? new JSONObject(clientRequest.response) : new JSONObject();

				JSONObject deviceInformation = new JSONObject();
				JSONObject appInfo = new JSONObject();

				boolean result = false;
				boolean shouldContinue = false;

				NetworkDevice localDevice = AppUtils.getLocalDevice(getApplicationContext());

				deviceInformation.put(Keyword.SERIAL, localDevice.deviceId);
				deviceInformation.put(Keyword.BRAND, localDevice.brand);
				deviceInformation.put(Keyword.MODEL, localDevice.model);
				deviceInformation.put(Keyword.USER, localDevice.user);

				appInfo.put(Keyword.VERSION_CODE, localDevice.buildNumber);
				appInfo.put(Keyword.VERSION_NAME, localDevice.buildName);

				replyJSON.put(Keyword.APP_INFO, appInfo);
				replyJSON.put(Keyword.DEVICE_INFO, deviceInformation);

				if (replyJSON.has(Keyword.SERIAL)) {
					String serialNumber = replyJSON.getString(Keyword.SERIAL);
					NetworkDevice device = new NetworkDevice(serialNumber);

					try {
						mDatabase.reconstruct(device);

						if (!device.isRestricted)
							shouldContinue = true;
					} catch (Exception e1) {
						e1.printStackTrace();

						device = mInfoLoader.startLoading(true, mDatabase, activeConnection.getClientAddress());

						if (device == null)
							throw new Exception("Could not reach to the opposite server");

						device.isRestricted = true;

						mDatabase.publish(device);
						mNotification.notifyConnectionRequest(device);

						shouldContinue = false;
					}

					final NetworkDevice.Connection connection = new NetworkDevice.Connection(activeConnection.getClientAddress());

					try {
						mDatabase.reconstruct(connection);
					} catch (Exception e) {
						connection.adapterName = Keyword.UNKNOWN_INTERFACE;
					}

					connection.lastCheckedDate = System.currentTimeMillis();
					connection.deviceId = device.deviceId;

					mDatabase.publish(connection);

					if (shouldContinue && replyJSON.has(Keyword.REQUEST)) {
						switch (replyJSON.getString(Keyword.REQUEST)) {
							case (Keyword.REQUEST_TRANSFER):
								if (replyJSON.has(Keyword.FILES_INDEX) && replyJSON.has(Keyword.GROUP_ID) && getOngoingIndexList().size() < 1) {
									String jsonIndex = replyJSON.getString(Keyword.FILES_INDEX);
									final JSONArray jsonArray = new JSONArray(jsonIndex);
									final int groupId = replyJSON.getInt(Keyword.GROUP_ID);
									final NetworkDevice finalDevice = device;

									result = true;

									new Thread()
									{
										@Override
										public void run()
										{
											super.run();

											Interrupter interrupter = new Interrupter();
											TransactionObject.Group group = new TransactionObject.Group(groupId, finalDevice.deviceId, connection.adapterName);
											TransactionObject transactionObject = null;

											mDatabase.publish(group);

											getOngoingIndexList().put(group.groupId, interrupter);

											DynamicNotification notification = mNotification.notifyPrepareFiles(group);

											int count = 0;
											int total = jsonArray.length();
											long lastNotified = System.currentTimeMillis();

											for (int i = 0; i < jsonArray.length(); i++) {
												if (interrupter.interrupted())
													break;

												try {
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

														if (requestIndex.has(Keyword.DIRECTORY))
															transactionObject.directory = requestIndex.getString(Keyword.DIRECTORY);

														mDatabase.publish(transactionObject);
													}

												} catch (JSONException e) {
													e.printStackTrace();
												}

												if ((System.currentTimeMillis() - lastNotified) > 1000) {
													lastNotified = System.currentTimeMillis();
													notification.updateProgress(total, count, false);
												}
											}

											notification.cancel();
											getOngoingIndexList().remove(group.groupId);

											if (interrupter.interrupted())
												mDatabase.remove(group);
											else if (transactionObject != null && count > 0)
												mNotification.notifyTransferRequest(transactionObject, finalDevice, count);
										}
									}.start();
								}
								break;
							case (Keyword.REQUEST_RESPONSE):
								if (replyJSON.has(Keyword.GROUP_ID)) {
									int groupId = replyJSON.getInt(Keyword.GROUP_ID);
									boolean isAccepted = replyJSON.getBoolean(Keyword.IS_ACCEPTED);

									if (!isAccepted)
										mDatabase.remove(new TransactionObject.Group(groupId));

									result = true;
								}
								break;
							case (Keyword.REQUEST_SERVER_READY):
								if (replyJSON.has(Keyword.REQUEST_ID) && replyJSON.has(Keyword.GROUP_ID) && replyJSON.has(Keyword.SOCKET_PORT)) {
									int requestId = replyJSON.getInt(Keyword.REQUEST_ID);
									int groupId = replyJSON.getInt(Keyword.GROUP_ID);
									int socketPort = replyJSON.getInt(Keyword.SOCKET_PORT);

									TransactionObject.Group group = new TransactionObject.Group(groupId);
									mDatabase.reconstruct(group);

									try {
										TransactionObject transactionObject = new TransactionObject(requestId);
										mDatabase.reconstruct(transactionObject);

										transactionObject.accessPort = socketPort;

										if (replyJSON.has(Keyword.SKIPPED_BYTES))
											transactionObject.skippedBytes = replyJSON.getInt(Keyword.SKIPPED_BYTES);

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
										replyJSON.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);
									}
								}

								break;
							case (Keyword.REQUEST_CLIPBOARD):
								if (replyJSON.has(Keyword.CLIPBOARD_TEXT)) {
									mReceivedClipboardIndex = replyJSON.getString(Keyword.CLIPBOARD_TEXT);
									mNotification.notifyClipboardRequest(device, mReceivedClipboardIndex);

									result = true;
								}
								break;
						}
					}
				}

				replyJSON.put(Keyword.RESULT, result);

				activeConnection.reply(replyJSON.toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public synchronized ArrayMap<Integer, Interrupter> getOngoingIndexList()
	{
		return mOngoingIndexList;
	}
}
