package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.MathUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceInfoLoader;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class CommunicationService extends Service
{
	public static final String TAG = "CommunicationService";

	public static final String ACTION_FILE_TRANSFER = "com.genonbeta.TrebleShot.action.FILE_TRANSFER";
	public static final String ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.action.CLIPBOARD";
	public static final String ACTION_CANCEL_INDEXING = "com.genonbeta.TrebleShot.action.CANCEL_INDEXING";
	public static final String ACTION_IP = "com.genonbeta.TrebleShot.action.IP";
	public static final String ACTION_END_SESSION = "com.genonbeta.TrebleShot.action.END_SESSION";
	public static final String ACTION_SEAMLESS_RECEIVE = "com.genonbeta.intent.action.SEAMLESS_START";
	public final static String ACTION_CANCEL_JOB = "com.genonbeta.TrebleShot.transaction.action.CANCEL_JOB";
	public final static String ACTION_CANCEL_KILL = "com.genonbeta.TrebleShot.transaction.action.CANCEL_KILL";
	public final static String ACTION_TOGGLE_SEAMLESS_MODE = "com.genonbeta.TrebleShot.transaction.action.TOGGLE_SEAMLESS_MODE";

	public static final String EXTRA_DEVICE_ID = "extraDeviceId";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_CLIPBOARD_ID = "extraTextId";
	public static final String EXTRA_GROUP_ID = "extraGroupId";
	public static final String EXTRA_IS_ACCEPTED = "extraAccepted";
	public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";

	private CommunicationServer mCommunicationServer = new CommunicationServer();
	private SeamlessServer mSeamlessServer = new SeamlessServer();
	private NetworkDeviceInfoLoader mInfoLoader = new NetworkDeviceInfoLoader();
	private ArrayMap<Integer, Interrupter> mOngoingIndexList = new ArrayMap<>();
	private NotificationUtils mNotificationUtils;
	private AccessDatabase mDatabase;
	private WifiManager.WifiLock mWifiLock;
	private MediaScannerConnection mMediaScanner;

	private Receive mReceive = new Receive();
	private Send mSend = new Send();

	private boolean mSeamlessMode = false;

	@Override
	public IBinder onBind(Intent intent)
	{
		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		if (!mCommunicationServer.start() || !mSeamlessServer.start())
			stopSelf();

		mNotificationUtils = new NotificationUtils(this);
		mDatabase = new AccessDatabase(this);
		mMediaScanner = new MediaScannerConnection(this, null);
		mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE))
				.createWifiLock(TAG);

		mReceive.setNotifyDelay(2000);
		mSend.setNotifyDelay(2000);

		mMediaScanner.connect();

		getWifiLock().acquire();
		updateServiceState(getNotificationUtils().getPreferences().getBoolean("trust_always", false));
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

				mNotificationUtils.cancel(notificationId);

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
						startFileReceiving(groupId);
					else
						mDatabase.remove(transactionGroup);

				} catch (Exception e) {
					e.printStackTrace();

					if (isAccepted)
						mNotificationUtils.showToast(R.string.mesg_somethingWentWrong);
				}
			} else if (ACTION_IP.equals(intent.getAction())) {
				String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
				boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				mNotificationUtils.cancel(notificationId);

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

				mNotificationUtils.cancel(notificationId);

				Interrupter interrupter = getOngoingIndexList().get(groupId);

				if (interrupter != null)
					interrupter.interrupt();
			} else if (ACTION_CLIPBOARD.equals(intent.getAction()) && intent.hasExtra(EXTRA_CLIPBOARD_ACCEPTED)) {
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				int clipboardId = intent.getIntExtra(EXTRA_CLIPBOARD_ID, -1);
				boolean isAccepted = intent.getBooleanExtra(EXTRA_CLIPBOARD_ACCEPTED, false);

				TextStreamObject textStreamObject = new TextStreamObject(clipboardId);

				mNotificationUtils.cancel(notificationId);

				try {
					getDatabase().reconstruct(textStreamObject);

					if (isAccepted) {
						((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("receivedText", textStreamObject.text));
						Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show();
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (ACTION_END_SESSION.equals(intent.getAction())) {
				stopSelf();
			} else if (ACTION_SEAMLESS_RECEIVE.equals(intent.getAction())
					&& intent.hasExtra(EXTRA_GROUP_ID)) {
				int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);

				try {
					CoolTransfer.TransferHandler<ProcessHolder> process = findProcessById(groupId);

					if (process == null)
						startFileReceiving(groupId);
					else
						Toast.makeText(this, getString(R.string.mesg_groupOngoingNotice, process.getExtra().transactionObject.friendlyName), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (ACTION_CANCEL_JOB.equals(intent.getAction())
					|| ACTION_CANCEL_KILL.equals(intent.getAction())) {
				int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				CoolTransfer.TransferHandler<ProcessHolder> handler = findProcessById(groupId);

				if (handler != null) {
					if (ACTION_CANCEL_KILL.equals(intent.getAction())) {
						try {
							if (handler instanceof CoolTransfer.Receive.Handler) {
								CoolTransfer.Receive.Handler receiveHandler = ((CoolTransfer.Receive.Handler) handler);

								if (receiveHandler.getServerSocket() != null)
									receiveHandler.getServerSocket().close();
							}

							if (handler.getExtra().activeConnection.getSocket() != null)
								handler.getExtra().activeConnection.getSocket().close();

							if (handler.getSocket() != null)
								handler.getSocket().close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						handler.getExtra().transactionObject.notification = getNotificationUtils().notifyStuckThread(handler.getExtra().transactionObject);
						handler.interrupt();
					}
				} else
					getNotificationUtils().cancel(notificationId);
			} else if (ACTION_TOGGLE_SEAMLESS_MODE.equals(intent.getAction())) {
				updateServiceState(!mSeamlessMode);
			}
		}

		return START_STICKY;
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();

		mCommunicationServer.stop();
		mSeamlessServer.stop();
		mMediaScanner.disconnect();

		getWifiLock().release();
		stopForeground(true);
	}

	public CoolTransfer.TransferHandler<ProcessHolder> findProcessById(int groupId)
	{
		for (CoolTransfer.TransferHandler<ProcessHolder> handler : mReceive.getProcessList())
			if (handler.getExtra().transactionObject.groupId == groupId)
				return handler;

		for (CoolTransfer.TransferHandler<ProcessHolder> handler : mSend.getProcessList())
			if (handler.getExtra().transactionObject.groupId == groupId)
				return handler;

		return null;
	}

	public AccessDatabase getDatabase()
	{
		return mDatabase;
	}

	public NotificationUtils getNotificationUtils()
	{
		return mNotificationUtils;
	}

	public synchronized ArrayMap<Integer, Interrupter> getOngoingIndexList()
	{
		return mOngoingIndexList;
	}

	public WifiManager.WifiLock getWifiLock()
	{
		return mWifiLock;
	}

	public boolean isProcessRunning(int groupId)
	{
		return findProcessById(groupId) != null;
	}

	public void startFileReceiving(int groupId) throws Exception
	{
		TransactionObject.Group transactionGroup = new TransactionObject.Group(groupId);
		getDatabase().reconstruct(transactionGroup);

		NetworkDevice networkDevice = new NetworkDevice(transactionGroup.deviceId);
		getDatabase().reconstruct(networkDevice);

		final NetworkDevice.Connection connection = new NetworkDevice.Connection(transactionGroup.deviceId, transactionGroup.connectionAdapter);
		getDatabase().reconstruct(connection);

		CoolSocket.connect(new SeamlessClientHandler(transactionGroup, connection, networkDevice));
	}

	public void updateServiceState(boolean seamlessMode)
	{
		mSeamlessMode = seamlessMode;

		startForeground(NotificationUtils.SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID, mNotificationUtils
				.getCommunicationServiceNotification(mSeamlessMode)
				.build());
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
				JSONObject responseJSON = clientRequest.totalLength > 0 ? new JSONObject(clientRequest.response) : new JSONObject();
				JSONObject replyJSON = new JSONObject();

				if (responseJSON.has(Keyword.REQUEST)
						&& Keyword.BACK_COMP_REQUEST_SEND_UPDATE.equals(responseJSON.getString(Keyword.REQUEST))) {
					activeConnection.reply(replyJSON.put(Keyword.RESULT, true).toString());
					UpdateUtils.sendUpdate(getApplicationContext(), activeConnection.getClientAddress());
					return;
				}

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

				if (responseJSON.has(Keyword.SERIAL)) {
					String serialNumber = responseJSON.getString(Keyword.SERIAL);
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
						mNotificationUtils.notifyConnectionRequest(device);

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

					mDatabase.remove(new NetworkDevice.Connection(device.deviceId, connection.adapterName)); // Remove old connection that has the connection adapter name and device id with old ip address
					mDatabase.publish(connection); // now register the new connection

					if (!shouldContinue)
						replyJSON.put(Keyword.ERROR, Keyword.NOT_ALLOWED);
					else if (responseJSON.has(Keyword.REQUEST)) {
						switch (responseJSON.getString(Keyword.REQUEST)) {
							case (Keyword.REQUEST_TRANSFER):
								if (responseJSON.has(Keyword.FILES_INDEX) && responseJSON.has(Keyword.GROUP_ID) && getOngoingIndexList().size() < 1) {
									String jsonIndex = responseJSON.getString(Keyword.FILES_INDEX);
									final JSONArray jsonArray = new JSONArray(jsonIndex);
									final int groupId = responseJSON.getInt(Keyword.GROUP_ID);
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

											DynamicNotification notification = mNotificationUtils.notifyPrepareFiles(group);

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
											else if (transactionObject != null && count > 0) {
												if (mSeamlessMode && finalDevice.isTrusted)
													try {
														startFileReceiving(group.groupId);
													} catch (Exception e) {
														e.printStackTrace();
													}
												else
													mNotificationUtils.notifyTransferRequest(transactionObject, finalDevice, count);

											}
										}
									}.start();
								}
								break;
							case (Keyword.REQUEST_RESPONSE):
								if (responseJSON.has(Keyword.GROUP_ID)) {
									int groupId = responseJSON.getInt(Keyword.GROUP_ID);
									boolean isAccepted = responseJSON.getBoolean(Keyword.IS_ACCEPTED);

									if (!isAccepted)
										mDatabase.remove(new TransactionObject.Group(groupId));

									result = true;
								}
								break;
							case (Keyword.REQUEST_CLIPBOARD):
								if (responseJSON.has(Keyword.CLIPBOARD_TEXT)) {
									TextStreamObject textStreamObject = new TextStreamObject(AppUtils.getUniqueNumber(), responseJSON.getString(Keyword.CLIPBOARD_TEXT));

									mDatabase.publish(textStreamObject);
									mNotificationUtils.notifyClipboardRequest(device, textStreamObject);

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

	private class SeamlessServer extends CoolSocket
	{
		public SeamlessServer()
		{
			super(AppConfig.SEAMLESS_SERVER_PORT);
		}

		@Override
		protected void onConnected(ActiveConnection activeConnection)
		{
			ProcessHolder processHolder = new ProcessHolder();

			try {
				ActiveConnection.Response mainRequest = activeConnection.receive();

				int groupId = new JSONObject(mainRequest.response)
						.getInt(Keyword.GROUP_ID);

				TransactionObject.Group transactionGroup = new TransactionObject.Group(groupId);
				getDatabase().reconstruct(transactionGroup);

				NetworkDevice networkDevice = new NetworkDevice(transactionGroup.deviceId);
				getDatabase().reconstruct(networkDevice);

				activeConnection.reply(new JSONObject().put(Keyword.RESULT, true).toString());

				processHolder.group = transactionGroup;
				processHolder.activeConnection = activeConnection;

				while (true) {
					ActiveConnection.Response currentResponse = activeConnection.receive();

					if(currentResponse.response == null || currentResponse.totalLength < 1)
						break;

					JSONObject currentRequest = new JSONObject(currentResponse.response);
					JSONObject currentReply = new JSONObject();

					try {
						processHolder.transactionObject = new TransactionObject(currentRequest.getInt(Keyword.REQUEST_ID));

						getDatabase().reconstruct(processHolder.transactionObject);

						processHolder.transactionObject.accessPort = currentRequest.getInt(Keyword.SOCKET_PORT);

						if (currentRequest.has(Keyword.SKIPPED_BYTES))
							processHolder.transactionObject.skippedBytes = currentRequest.getInt(Keyword.SKIPPED_BYTES);

						getDatabase().publish(processHolder.transactionObject);

						currentReply.put(Keyword.RESULT, true);

					} catch (Exception e) {
						currentReply.put(Keyword.RESULT, false);
						currentReply.put(Keyword.ERROR, Keyword.NOT_FOUND);
						currentReply.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);
					} finally {
						activeConnection.reply(currentReply.toString());

						if (currentReply.getBoolean(Keyword.RESULT)) {
							StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), Uri.parse(processHolder.transactionObject.file), true);

							processHolder.transferHandler = mSend.send(activeConnection.getClientAddress(), processHolder.transactionObject.accessPort, streamInfo.inputStream, streamInfo.size, AppConfig.DEFAULT_BUFFER_SIZE, processHolder, true);
						}
					}

					if (processHolder.transferHandler != null
							&& processHolder.transferHandler.getFlag().equals(CoolTransfer.Flag.CANCEL_ALL))
						break;
				}
			} catch (Exception e) {
				e.printStackTrace();

				try {
					JSONObject currentReply = new JSONObject();

					currentReply.put(Keyword.RESULT, false);
					currentReply.put(Keyword.ERROR, Keyword.NOT_FOUND);

					activeConnection.reply(currentReply.toString());
				} catch (TimeoutException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (JSONException e1) {
					e1.printStackTrace();
				}
			} finally {
				try {
					activeConnection.getSocket().close();
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (processHolder.transferHandler != null)
					processHolder.transferHandler.getExtra().transactionObject.notification.cancel();
			}
		}
	}

	private class SeamlessClientHandler implements CoolSocket.Client.ConnectionHandler
	{
		private TransactionObject.Group mGroup;
		private NetworkDevice.Connection mConnection;
		private NetworkDevice mDevice;

		public SeamlessClientHandler(TransactionObject.Group group, NetworkDevice.Connection connection, NetworkDevice device)
		{
			mGroup = group;
			mConnection = connection;
			mDevice = device;
		}

		@Override
		public void onConnect(CoolSocket.Client client)
		{
			ProcessHolder processHolder = new ProcessHolder();
			CoolSocket.ActiveConnection activeConnection = null;

			try {
				activeConnection = client.connect(new InetSocketAddress(mConnection.ipAddress, AppConfig.SEAMLESS_SERVER_PORT), AppConfig.DEFAULT_SOCKET_TIMEOUT);

				activeConnection.reply(new JSONObject()
						.put(Keyword.GROUP_ID, mGroup.groupId)
						.toString());

				CoolSocket.ActiveConnection.Response mainRequest = activeConnection.receive();

				processHolder.activeConnection = activeConnection;
				processHolder.group = mGroup;

				if (!new JSONObject(mainRequest.response).getBoolean(Keyword.RESULT))
					throw new IOException("Request was not accepted");

				while (true) {
					CursorItem receiverInstance = getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
							.setWhere(AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND " + AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_FLAG + " !=?  AND " + AccessDatabase.FIELD_TRANSFER_FLAG + " !=?",
									TransactionObject.Type.INCOMING.toString(),
									String.valueOf(processHolder.group.groupId),
									TransactionObject.Flag.INTERRUPTED.toString(),
									TransactionObject.Flag.REMOVED.toString()));

					if (receiverInstance == null
							&& getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
							.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(processHolder.group.groupId))) == null) {
						getDatabase().remove(processHolder.group);
						break;
					}

					processHolder.transactionObject = new TransactionObject(receiverInstance);
					File file = FileUtils.getIncomingTransactionFile(getApplicationContext(), processHolder.transactionObject, processHolder.group);
Log.d(TAG, "Received file: " + file);
					processHolder.transferHandler = mReceive.receive(0, file, processHolder.transactionObject.fileSize, AppConfig.DEFAULT_BUFFER_SIZE, AppConfig.DEFAULT_SOCKET_TIMEOUT, processHolder, true);

					if (CoolTransfer.Flag.CANCEL_ALL.equals(processHolder.transferHandler.getFlag()))
						break;
				}

				if (processHolder.transferHandler != null && CoolTransfer.Flag.CONTINUE.equals(processHolder.transferHandler.getFlag())) {
					if (processHolder.transferHandler.getGroupTransferredFileCount() == 1)
						getNotificationUtils().notifyFileReceived(processHolder.transferHandler.getExtra().transactionObject, mDevice, processHolder.transferHandler.getFile());
					else if (processHolder.transferHandler.getGroupTransferredFileCount() > 1) {
						String parentDir = processHolder.transferHandler.getFile().getParent();
						String savePath = processHolder.transferHandler.getExtra().transactionObject.directory != null
								&& processHolder.transferHandler.getExtra().transactionObject.directory.length() > 0
								? parentDir.substring(0, parentDir.length() - processHolder.transferHandler.getExtra().transactionObject.directory.length())
								: parentDir;

						getNotificationUtils().notifyFileReceived(processHolder.transferHandler.getExtra().transactionObject, savePath, processHolder.transferHandler.getGroupTransferredFileCount());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				try {
					if (activeConnection != null)
						activeConnection.getSocket().close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public class Receive extends CoolTransfer.Receive<ProcessHolder>
	{
		@Override
		public Flag onError(TransferHandler<ProcessHolder> handler, Exception error)
		{
			error.printStackTrace();

			handler.getExtra().transactionObject.flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().publish(handler.getExtra().transactionObject);
			getNotificationUtils().notifyReceiveError(handler.getExtra().transactionObject);

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onNotify(TransferHandler<ProcessHolder> handler, int percentage, int groupPercentage, long eta)
		{
			handler.getExtra().transactionObject.notification.setContentText(getString(R.string.text_remainingTime, TimeUtils.getDuration(eta)));
			handler.getExtra().transactionObject.notification.updateProgress(100, groupPercentage == -1 ? percentage : groupPercentage, false);
		}

		@Override
		public void onTransferCompleted(TransferHandler<ProcessHolder> handler)
		{
			getDatabase().remove(handler.getExtra().transactionObject);

			File finalFileLocation = FileUtils.getUniqueFile(new File(handler.getFile().getParent() + File.separator + handler.getExtra().transactionObject.friendlyName), true);

			handler.getFile().renameTo(finalFileLocation);
			handler.setFile(finalFileLocation);
		}

		@Override
		public void onInterrupted(TransferHandler<ProcessHolder> handler)
		{
			handler.getExtra().transactionObject.notification.cancel();
			handler.getExtra().transactionObject.flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().publish(handler.getExtra().transactionObject);
		}

		@Override
		public Flag onSocketReady(TransferHandler<ProcessHolder> handler)
		{
			return Flag.CONTINUE;
		}

		@Override
		public Flag onSocketReady(final TransferHandler<ProcessHolder> handler, final ServerSocket serverSocket)
		{
			try {
				JSONObject jsonObject = new JSONObject();

				jsonObject.put(Keyword.REQUEST_ID, handler.getExtra().transactionObject.requestId);
				jsonObject.put(Keyword.GROUP_ID, handler.getExtra().transactionObject.groupId);
				jsonObject.put(Keyword.SOCKET_PORT, serverSocket.getLocalPort());

				if (handler.getFile().length() > 0)
					jsonObject.put(Keyword.SKIPPED_BYTES, handler.getFile().length());

				handler.getExtra().activeConnection.reply(jsonObject.toString());

				JSONObject response = new JSONObject(handler.getExtra().activeConnection.receive().response);

				if (response.getBoolean(Keyword.RESULT))
					return Flag.CONTINUE;
				else if (response.has(Keyword.FLAG) && Keyword.FLAG_GROUP_EXISTS.equals(response.getString(Keyword.FLAG))) {
					if (response.has(Keyword.ERROR) && response.getString(Keyword.ERROR).equals(Keyword.NOT_FOUND)) {
						handler.getExtra().transactionObject.flag = TransactionObject.Flag.REMOVED;
						getDatabase().publish(handler.getExtra().transactionObject);
					}

					return Flag.CANCEL_CURRENT;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			return Flag.CANCEL_ALL;
		}

		@Override
		public Flag onStart(TransferHandler<ProcessHolder> handler)
		{
			handler.linkTo(handler.getExtra().transferHandler);

			if (handler.getGroupTransferredFileCount() == 0) {
				TransactionObject.Group.Index indexInstance = new TransactionObject.Group.Index();

				getDatabase().calculateTransactionSize(handler.getExtra().transactionObject.groupId, indexInstance);

				handler.setGroupTotalByte(indexInstance.incoming);
			}

			try {
				handler.getExtra().transactionObject.notification = getNotificationUtils().notifyFileTransaction(handler.getExtra().transactionObject);
				handler.getExtra().transactionObject.flag = TransactionObject.Flag.RUNNING;

				if (handler.getGroupTotalByte() > 0)
					onNotify(handler, 0, MathUtils.calculatePercentage(handler.getGroupTotalByte(), handler.getGroupTransferredByte()), handler.getTimeRemaining());
				else
					handler.getExtra().transactionObject.notification.show();

				getDatabase().publish(handler.getExtra().transactionObject);

				return Flag.CONTINUE;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onProcessListChanged
				(ArrayList<TransferHandler<ProcessHolder>> processList, TransferHandler<ProcessHolder> handler,
				 boolean isAdded)
		{
			super.onProcessListChanged(processList, handler, isAdded);

			if (!isAdded) {
				if (mMediaScanner.isConnected())
					mMediaScanner.scanFile(handler.getFile().getAbsolutePath(), handler.getExtra().transactionObject.fileMimeType);

				sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
						.putExtra(FileListFragment.EXTRA_PATH, handler.getFile().getParent()));
			}
		}
	}

	public class Send extends CoolTransfer.Send<ProcessHolder>
	{
		@Override
		public Flag onError(TransferHandler<ProcessHolder> handler, Exception error)
		{
			error.printStackTrace();

			handler.getExtra()
					.transactionObject
					.flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().publish(handler.getExtra().transactionObject);

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onNotify(TransferHandler<ProcessHolder> handler, int percentage, int groupPercentage, long eta)
		{
			handler.getExtra()
					.transactionObject
					.notification.setContentText(getString(R.string.text_remainingTime, TimeUtils.getDuration(eta)));

			handler.getExtra()
					.transactionObject
					.notification.updateProgress(100, groupPercentage == -1 ? percentage : groupPercentage, false);
		}

		@Override
		public void onTransferCompleted(TransferHandler<ProcessHolder> handler)
		{
			getDatabase().remove(handler.getExtra().transactionObject);

			if (getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
					.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(handler.getExtra().transactionObject.groupId))) == null)
				getDatabase().remove(new TransactionObject.Group(handler.getExtra().transactionObject.groupId));
		}

		@Override
		public void onInterrupted(TransferHandler<ProcessHolder> handler)
		{
		}

		@Override
		public Flag onSocketReady(TransferHandler<ProcessHolder> handler)
		{
			return Flag.CONTINUE;
		}

		@Override
		public Flag onStart(TransferHandler<ProcessHolder> handler)
		{
			handler.linkTo(handler.getExtra().transferHandler);

			if (handler.getGroupTransferredFileCount() == 0) {
				TransactionObject.Group.Index indexInstance = new TransactionObject.Group.Index();

				getDatabase().calculateTransactionSize(handler.getExtra().transactionObject.groupId, indexInstance);

				handler.setGroupTotalByte(indexInstance.outgoing);
			}

			try {
				handler.getExtra()
						.transactionObject
						.notification = getNotificationUtils().notifyFileTransaction(handler.getExtra().transactionObject);

				handler.getExtra()
						.transactionObject
						.flag = TransactionObject.Flag.RUNNING;

				getWifiLock().acquire();
				getDatabase().publish(handler.getExtra().transactionObject);

				if (handler.getGroupTotalByte() > 0)
					onNotify(handler, 0, MathUtils.calculatePercentage(handler.getGroupTotalByte(), handler.getGroupTransferredByte()), handler.getTimeRemaining());
				else
					handler.getExtra()
							.transactionObject
							.notification
							.show();

				return Flag.CONTINUE;
			} catch (Exception e) {
				e.printStackTrace();
			}

			return Flag.CANCEL_ALL;
		}


		@Override
		public void onOrientatingStreams(Handler handler, InputStream inputStream, OutputStream outputStream)
		{
			super.onOrientatingStreams(handler, inputStream, outputStream);

			if (handler.getExtra().transactionObject.skippedBytes > 0)
				try {
					handler.skipBytes(handler.getExtra().transactionObject.skippedBytes);
				} catch (IOException e) {
					handler.interrupt();
					e.printStackTrace();
				}
		}
	}

	private class ProcessHolder
	{
		public CoolTransfer.TransferHandler<ProcessHolder> transferHandler;
		public CoolSocket.ActiveConnection activeConnection;
		public TransactionObject transactionObject;
		public TransactionObject.Group group;
	}
}
