package com.genonbeta.TrebleShot.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.TransactionGroupNotFoundException;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.io.DocumentFile;
import com.genonbeta.TrebleShot.io.LocalDocumentFile;
import com.genonbeta.TrebleShot.io.StreamInfo;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.object.TransactionObject;
import com.genonbeta.TrebleShot.object.TransferInstance;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.CommunicationNotificationHelper;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.Interrupter;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.TrebleShot.util.NsdDiscovery;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.security.Key;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
	public final static String ACTION_TOGGLE_HOTSPOT = "com.genonbeta.TrebleShot.transaction.action.TOGGLE_HOTSPOT";
	public final static String ACTION_REQUEST_HOTSPOT_STATUS = "com.genonbeta.TrebleShot.transaction.action.REQUEST_HOTSPOT_STATUS";
	public final static String ACTION_HOTSPOT_STATUS = "com.genonbeta.TrebleShot.transaction.action.HOTSPOT_STATUS";

	public static final String EXTRA_DEVICE_ID = "extraDeviceId";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_CLIPBOARD_ID = "extraTextId";
	public static final String EXTRA_GROUP_ID = "extraGroupId";
	public static final String EXTRA_IS_ACCEPTED = "extraAccepted";
	public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";
	public static final String EXTRA_HOTSPOT_ENABLED = "extraHotspotEnabled";
	public static final String EXTRA_HOTSPOT_NAME = "extraHotspotName";
	public static final String EXTRA_HOTSPOT_KEY_MGMT = "extraHotspotKeyManagement";
	public static final String EXTRA_HOTSPOT_PASSWORD = "extraHotspotPassword";

	private CommunicationServer mCommunicationServer = new CommunicationServer();
	private SeamlessServer mSeamlessServer = new SeamlessServer();
	private ArrayMap<Integer, Interrupter> mOngoingIndexList = new ArrayMap<>();
	private NsdDiscovery mNsdDiscovery;
	private CommunicationNotificationHelper mNotificationHelper;
	private AccessDatabase mDatabase;
	private WifiManager.WifiLock mWifiLock;
	private MediaScannerConnection mMediaScanner;
	private ExecutorService mSelfExecutor = Executors.newFixedThreadPool(10);
	private HotspotUtils mHotspotUtils;
	private Object mBlockingObject = new Object();

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

		mNotificationHelper = new CommunicationNotificationHelper(this);
		mDatabase = new AccessDatabase(this);
		mNsdDiscovery = new NsdDiscovery(getApplicationContext(), getDatabase());
		mMediaScanner = new MediaScannerConnection(this, null);
		mHotspotUtils = HotspotUtils.getInstance(this);
		mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE))
				.createWifiLock(TAG);

		mReceive.setNotifyDelay(AppConfig.DEFAULT_NOTIFICATION_DELAY);
		mReceive.setBlockingObject(mBlockingObject);

		mSend.setNotifyDelay(AppConfig.DEFAULT_NOTIFICATION_DELAY);
		mSend.setBlockingObject(mBlockingObject);

		mMediaScanner.connect();
		mNsdDiscovery.registerService();

		if (getWifiLock() != null)
			getWifiLock().acquire();

		updateServiceState(getPreferences().getBoolean("trust_always", false));

		if (!AppUtils.checkRunningConditions(this)
				|| !mCommunicationServer.start()
				|| !mSeamlessServer.start())
			stopSelf();

		if (getHotspotUtils() instanceof HotspotUtils.OreoAPI && Build.VERSION.SDK_INT >= 26)
			((HotspotUtils.OreoAPI) getHotspotUtils()).setSecondaryCallback(new WifiManager.LocalOnlyHotspotCallback()
			{
				@RequiresApi(api = Build.VERSION_CODES.O)
				@Override
				public void onStarted(WifiManager.LocalOnlyHotspotReservation reservation)
				{
					super.onStarted(reservation);

					sendHotspotStatus(reservation.getWifiConfiguration());

					if (getPreferences().getBoolean("hotspot_trust", false))
						updateServiceState(true);
				}
			});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			Log.d(TAG, "onStart() : action = " + intent.getAction());

		if (intent != null && AppUtils.checkRunningConditions(this)) {
			if (ACTION_FILE_TRANSFER.equals(intent.getAction())) {
				final int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				final boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);

				getNotificationHelper().getUtils().cancel(notificationId);

				try {
					final TransferInstance transferInstance = new TransferInstance(getDatabase(), groupId);

					CommunicationBridge.connect(getDatabase(), new CommunicationBridge.Client.ConnectionHandler()
					{
						@Override
						public void onConnect(CommunicationBridge.Client client)
						{
							try {
								CoolSocket.ActiveConnection activeConnection = client.communicate(transferInstance.getDevice(), transferInstance.getConnection());

								activeConnection.reply(new JSONObject()
										.put(Keyword.REQUEST, Keyword.REQUEST_RESPONSE)
										.put(Keyword.TRANSFER_GROUP_ID, groupId)
										.put(Keyword.TRANSFER_IS_ACCEPTED, isAccepted)
										.toString());
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					});

					if (isAccepted)
						startFileReceiving(groupId);
					else
						mDatabase.remove(transferInstance.getGroup());
				} catch (Exception e) {
					e.printStackTrace();

					if (isAccepted)
						getNotificationHelper().showToast(R.string.mesg_somethingWentWrong);
				}
			} else if (ACTION_IP.equals(intent.getAction())) {
				String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
				boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);

				getNotificationHelper().getUtils().cancel(notificationId);

				NetworkDevice device = new NetworkDevice(deviceId);

				try {
					mDatabase.reconstruct(device);
					device.isRestricted = !isAccepted;
					mDatabase.update(device);
				} catch (Exception e) {
					e.printStackTrace();
					return START_NOT_STICKY;
				}
			} else if (ACTION_CANCEL_INDEXING.equals(intent.getAction())) {
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				int groupId = intent.getIntExtra(EXTRA_GROUP_ID, -1);

				getNotificationHelper().getUtils().cancel(notificationId);

				Interrupter interrupter = getOngoingIndexList().get(groupId);

				if (interrupter != null)
					interrupter.interrupt();
			} else if (ACTION_CLIPBOARD.equals(intent.getAction()) && intent.hasExtra(EXTRA_CLIPBOARD_ACCEPTED)) {
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				int clipboardId = intent.getIntExtra(EXTRA_CLIPBOARD_ID, -1);
				boolean isAccepted = intent.getBooleanExtra(EXTRA_CLIPBOARD_ACCEPTED, false);

				TextStreamObject textStreamObject = new TextStreamObject(clipboardId);

				getNotificationHelper().getUtils().cancel(notificationId);

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

				if (handler == null || ACTION_CANCEL_KILL.equals(intent.getAction()))
					getNotificationHelper().getUtils().cancel(notificationId);

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
						handler.getExtra().notification = getNotificationHelper().notifyStuckThread(handler.getExtra().transactionObject);
						handler.interrupt();
					}
				}
			} else if (ACTION_TOGGLE_SEAMLESS_MODE.equals(intent.getAction())) {
				updateServiceState(!mSeamlessMode);
			} else if (ACTION_TOGGLE_HOTSPOT.equals(intent.getAction())
					&& (Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this))) {
				setupHotspot();
			} else if (ACTION_REQUEST_HOTSPOT_STATUS.equals(intent.getAction()))
				sendHotspotStatus(getHotspotUtils().getConfiguration());
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
		mNsdDiscovery.unregisterService();

		if (getHotspotUtils().unloadPreviousConfig())
			getHotspotUtils().disable();

		if (getWifiLock() != null && getWifiLock().isHeld())
			getWifiLock().release();

		stopForeground(true);

		synchronized (getOngoingIndexList()) {
			for (Interrupter interrupter : getOngoingIndexList().values())
				interrupter.interrupt(false);
		}

		synchronized (mReceive.getProcessList()) {
			for (CoolTransfer.TransferHandler<ProcessHolder> transferHandler : mReceive.getProcessList())
				transferHandler.interrupt();
		}

		synchronized (mSend.getProcessList()) {
			for (CoolTransfer.TransferHandler<ProcessHolder> transferHandler : mSend.getProcessList())
				transferHandler.interrupt();
		}
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

	public HotspotUtils getHotspotUtils()
	{
		return mHotspotUtils;
	}

	public CommunicationNotificationHelper getNotificationHelper()
	{
		return mNotificationHelper;
	}

	public synchronized ArrayMap<Integer, Interrupter> getOngoingIndexList()
	{
		return mOngoingIndexList;
	}

	public SharedPreferences getPreferences()
	{
		return getNotificationHelper().getUtils().getPreferences();
	}

	public ExecutorService getSelfExecutor()
	{
		return mSelfExecutor;
	}

	public WifiManager.WifiLock getWifiLock()
	{
		return mWifiLock;
	}

	public boolean isQRFastMode()
	{
		return getPreferences().getBoolean("qr_trust", false)
				&& (mHotspotUtils.isStarted());
	}

	public boolean isProcessRunning(int groupId)
	{
		return findProcessById(groupId) != null;
	}

	public void sendHotspotStatus(WifiConfiguration wifiConfiguration)
	{
		Intent statusIntent = new Intent(ACTION_HOTSPOT_STATUS)
				.putExtra(EXTRA_HOTSPOT_ENABLED, wifiConfiguration != null);

		if (wifiConfiguration != null) {
			statusIntent.putExtra(EXTRA_HOTSPOT_NAME, wifiConfiguration.SSID)
					.putExtra(EXTRA_HOTSPOT_PASSWORD, wifiConfiguration.preSharedKey)
					.putExtra(EXTRA_HOTSPOT_KEY_MGMT, NetworkUtils.getAllowedKeyManagement(wifiConfiguration));
		}

		sendBroadcast(statusIntent);
	}

	public void setupHotspot()
	{
		boolean isEnabled = !getHotspotUtils().isEnabled();
		boolean overrideTrustZone = getPreferences().getBoolean("hotspot_trust", false);

		// On Oreo devices, we will use platform specific code.
		if (overrideTrustZone && (!isEnabled || Build.VERSION.SDK_INT < 26))
			updateServiceState(isEnabled);

		if (isEnabled)
			getHotspotUtils().enableConfigured(AppUtils.getHotspotName(this), null);
		else {
			getHotspotUtils().disable();

			if (Build.VERSION.SDK_INT >= 26)
				sendHotspotStatus(null);
		}
	}

	public void startFileReceiving(int groupId) throws TransactionGroupNotFoundException, DeviceNotFoundException, ConnectionNotFoundException
	{
		startFileReceiving(new TransferInstance(getDatabase(), groupId));
	}

	public void startFileReceiving(TransferInstance transferInstance)
	{
		CoolSocket.connect(new SeamlessClientHandler(transferInstance));
	}

	public void updateServiceState(boolean seamlessMode)
	{
		mSeamlessMode = seamlessMode;

		startForeground(CommunicationNotificationHelper.SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID,
				getNotificationHelper().getCommunicationServiceNotification(mSeamlessMode).build());
	}

	public class CommunicationServer extends CoolSocket
	{
		public CommunicationServer()
		{
			super(AppConfig.SERVER_PORT_COMMUNICATION);
			setSocketTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT_LARGE);
		}

		@Override
		protected void onConnected(final ActiveConnection activeConnection)
		{
			if (getConnectionCountByAddress(activeConnection.getAddress()) > 3)
				return;

			try {
				ActiveConnection.Response clientRequest = activeConnection.receive();
				JSONObject responseJSON = analyzeResponse(clientRequest);
				JSONObject replyJSON = new JSONObject();

				if (responseJSON.has(Keyword.REQUEST)
						&& Keyword.BACK_COMP_REQUEST_SEND_UPDATE.equals(responseJSON.getString(Keyword.REQUEST))) {
					activeConnection.reply(replyJSON.put(Keyword.RESULT, true).toString());

					getSelfExecutor().submit(new Runnable()
					{
						@Override
						public void run()
						{
							try {
								UpdateUtils.sendUpdate(getApplicationContext(), activeConnection.getClientAddress());
							} catch (IOException e) {
								e.printStackTrace();
							}
						}
					});

					return;
				}

				boolean result = false;
				boolean shouldContinue = false;

				String deviceSerial = null;

				AppUtils.applyDeviceToJSON(AppUtils.getLocalDevice(getApplicationContext()), replyJSON);

				if (responseJSON.has(Keyword.HANDSHAKE_REQUIRED) && responseJSON.getBoolean(Keyword.HANDSHAKE_REQUIRED)) {
					pushReply(activeConnection, replyJSON, true);

					if (!responseJSON.has(Keyword.HANDSHAKE_ONLY) || !responseJSON.getBoolean(Keyword.HANDSHAKE_ONLY)) {
						if (responseJSON.has(Keyword.DEVICE_INFO_SERIAL))
							deviceSerial = responseJSON.getString(Keyword.DEVICE_INFO_SERIAL);

						clientRequest = activeConnection.receive();
						responseJSON = analyzeResponse(clientRequest);
					} else {
						return;
					}
				}

				final boolean qrConnection = responseJSON.has(Keyword.FLAG_TRANSFER_QR_CONNECTION)
						&& responseJSON.getBoolean(Keyword.FLAG_TRANSFER_QR_CONNECTION);

				final boolean seamlessActive = mSeamlessMode
						|| (isQRFastMode() && qrConnection);

				if (deviceSerial != null) {
					NetworkDevice device = new NetworkDevice(deviceSerial);

					try {
						mDatabase.reconstruct(device);

						if (!device.isRestricted)
							shouldContinue = true;
					} catch (Exception e1) {
						e1.printStackTrace();

						device = NetworkDeviceLoader.load(true, mDatabase, activeConnection.getClientAddress(), null);

						if (device == null)
							throw new Exception("Could not reach to the opposite server");

						device.isRestricted = true;

						mDatabase.publish(device);

						shouldContinue = true;

						getNotificationHelper().notifyConnectionRequest(device);
					}

					final NetworkDevice.Connection connection = NetworkDeviceLoader.processConnection(mDatabase, device, activeConnection.getClientAddress());

					if (!shouldContinue)
						replyJSON.put(Keyword.ERROR, Keyword.ERROR_NOT_ALLOWED);
					else if (responseJSON.has(Keyword.REQUEST)) {
						switch (responseJSON.getString(Keyword.REQUEST)) {
							case (Keyword.REQUEST_TRANSFER):
								if (responseJSON.has(Keyword.FILES_INDEX) && responseJSON.has(Keyword.TRANSFER_GROUP_ID) && getOngoingIndexList().size() < 1) {
									String jsonIndex = responseJSON.getString(Keyword.FILES_INDEX);
									final JSONArray jsonArray = new JSONArray(jsonIndex);
									final int groupId = responseJSON.getInt(Keyword.TRANSFER_GROUP_ID);
									final NetworkDevice finalDevice = device;

									result = true;

									getSelfExecutor().submit(new Runnable()
									{
										@Override
										public void run()
										{
											Interrupter interrupter = new Interrupter();
											TransactionObject.Group group = new TransactionObject.Group(groupId, finalDevice.deviceId, connection.adapterName);
											TransactionObject transactionObject = null;

											mDatabase.publish(group);

											synchronized (getOngoingIndexList()) {
												getOngoingIndexList().put(group.groupId, interrupter);
											}

											DynamicNotification notification = getNotificationHelper().notifyPrepareFiles(group);

											int count = 0;
											int total = jsonArray.length();
											long lastNotified = System.currentTimeMillis();

											for (int i = 0; i < total; i++) {
												if (interrupter.interrupted())
													break;

												try {
													if (!(jsonArray.get(i) instanceof JSONObject))
														continue;

													JSONObject requestIndex = jsonArray.getJSONObject(i);

													if (requestIndex != null && requestIndex.has(Keyword.INDEX_FILE_NAME) && requestIndex.has(Keyword.INDEX_FILE_SIZE) && requestIndex.has(Keyword.INDEX_FILE_MIME) && requestIndex.has(Keyword.TRANSFER_REQUEST_ID)) {
														count++;

														transactionObject = new TransactionObject(
																requestIndex.getInt(Keyword.TRANSFER_REQUEST_ID),
																groupId,
																requestIndex.getString(Keyword.INDEX_FILE_NAME),
																"." + UUID.randomUUID() + ".tshare",
																requestIndex.getString(Keyword.INDEX_FILE_MIME),
																requestIndex.getLong(Keyword.INDEX_FILE_SIZE),
																TransactionObject.Type.INCOMING);

														if (requestIndex.has(Keyword.INDEX_DIRECTORY))
															transactionObject.directory = requestIndex.getString(Keyword.INDEX_DIRECTORY);

														mDatabase.insert(transactionObject);
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

											synchronized (getOngoingIndexList()) {
												getOngoingIndexList().remove(group.groupId);
											}

											if (interrupter.interrupted())
												mDatabase.remove(group);
											else if (transactionObject != null && count > 0) {
												if (seamlessActive && finalDevice.isTrusted)
													try {
														startFileReceiving(group.groupId);
													} catch (Exception e) {
														e.printStackTrace();
													}
												else
													getNotificationHelper().notifyTransferRequest(transactionObject, finalDevice, count);

											}
										}
									});
								}
								break;
							case (Keyword.REQUEST_RESPONSE):
								if (responseJSON.has(Keyword.TRANSFER_GROUP_ID)) {
									int groupId = responseJSON.getInt(Keyword.TRANSFER_GROUP_ID);
									boolean isAccepted = responseJSON.getBoolean(Keyword.TRANSFER_IS_ACCEPTED);

									if (!isAccepted)
										mDatabase.remove(new TransactionObject.Group(groupId));

									result = true;
								}
								break;
							case (Keyword.REQUEST_CLIPBOARD):
								if (responseJSON.has(Keyword.TRANSFER_CLIPBOARD_TEXT)) {
									TextStreamObject textStreamObject = new TextStreamObject(AppUtils.getUniqueNumber(), responseJSON.getString(Keyword.TRANSFER_CLIPBOARD_TEXT));

									mDatabase.publish(textStreamObject);
									getNotificationHelper().notifyClipboardRequest(device, textStreamObject);

									result = true;
								}
								break;
						}
					}
				}

				pushReply(activeConnection, replyJSON, result);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		public JSONObject analyzeResponse(ActiveConnection.Response response) throws JSONException
		{
			return response.totalLength > 0 ? new JSONObject(response.response) : new JSONObject();
		}

		public void pushReply(ActiveConnection activeConnection, JSONObject reply, boolean result) throws JSONException, TimeoutException, IOException
		{
			activeConnection.reply(reply
					.put(Keyword.RESULT, result)
					.toString());
		}
	}

	private class SeamlessServer extends CoolSocket
	{
		public SeamlessServer()
		{
			super(AppConfig.SERVER_PORT_SEAMLESS);
			setSocketTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT);
		}

		@Override
		protected void onConnected(ActiveConnection activeConnection)
		{
			ProcessHolder processHolder = new ProcessHolder();

			try {
				ActiveConnection.Response mainRequest = activeConnection.receive();

				int groupId = new JSONObject(mainRequest.response)
						.getInt(Keyword.TRANSFER_GROUP_ID);

				TransferInstance transferInstance = new TransferInstance(getDatabase(), groupId, activeConnection.getClientAddress());

				activeConnection.reply(new JSONObject().put(Keyword.RESULT, true).toString());

				processHolder.group = transferInstance.getGroup();
				processHolder.activeConnection = activeConnection;

				while (true) {
					ActiveConnection.Response currentResponse = activeConnection.receive();

					if (currentResponse.response == null || currentResponse.totalLength < 1)
						break;

					JSONObject currentRequest = new JSONObject(currentResponse.response);
					JSONObject currentReply = new JSONObject();

					if (currentReply.has(Keyword.RESULT) && !currentReply.getBoolean(Keyword.RESULT))
						break;

					try {
						processHolder.transactionObject = new TransactionObject(currentRequest.getInt(Keyword.TRANSFER_REQUEST_ID));

						getDatabase().reconstruct(processHolder.transactionObject);

						processHolder.transactionObject.accessPort = currentRequest.getInt(Keyword.TRANSFER_SOCKET_PORT);

						if (currentRequest.has(Keyword.SKIPPED_BYTES))
							processHolder.transactionObject.skippedBytes = currentRequest.getInt(Keyword.SKIPPED_BYTES);

						getDatabase().update(processHolder.transactionObject);

						currentReply.put(Keyword.RESULT, true);
					} catch (Exception e) {
						currentReply.put(Keyword.RESULT, false);
						currentReply.put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND);
						currentReply.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);
					} finally {
						activeConnection.reply(currentReply.toString());

						if (currentReply.getBoolean(Keyword.RESULT)) {
							StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), Uri.parse(processHolder.transactionObject.file));

							getNotificationHelper().notifyFileTransaction(processHolder);

							processHolder.transferHandler = mSend.send(activeConnection.getClientAddress(), processHolder.transactionObject.accessPort, streamInfo.openInputStream(), streamInfo.size, AppConfig.BUFFER_LENGTH_DEFAULT, processHolder, true);
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

					if (e instanceof TransactionGroupNotFoundException)
						currentReply.put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND);
					else if (e instanceof DeviceNotFoundException)
						currentReply.put(Keyword.ERROR, Keyword.ERROR_NOT_ALLOWED);

					activeConnection.reply(currentReply.toString());
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			} finally {
				try {
					if (!activeConnection.getSocket().isClosed()) {
						activeConnection.getSocket().getOutputStream().close();
						activeConnection.getSocket().getInputStream().close();
						activeConnection.getSocket().close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

				if (processHolder.notification != null)
					processHolder.notification.cancel();
			}
		}
	}

	private class SeamlessClientHandler implements CoolSocket.Client.ConnectionHandler
	{
		private TransferInstance mTransfer;

		public SeamlessClientHandler(TransferInstance transferInstance)
		{
			mTransfer = transferInstance;
		}

		@Override
		public void onConnect(CoolSocket.Client client)
		{
			ProcessHolder processHolder = new ProcessHolder();
			CoolSocket.ActiveConnection activeConnection = null;

			try {
				activeConnection = client.connect(new InetSocketAddress(mTransfer.getConnection().ipAddress, AppConfig.SERVER_PORT_SEAMLESS), AppConfig.DEFAULT_SOCKET_TIMEOUT);

				activeConnection.reply(new JSONObject()
						.put(Keyword.TRANSFER_GROUP_ID, mTransfer.getGroup().groupId)
						.toString());

				CoolSocket.ActiveConnection.Response mainRequest = activeConnection.receive();

				processHolder.activeConnection = activeConnection;
				processHolder.group = mTransfer.getGroup();

				JSONObject mainRequestJSON = new JSONObject(mainRequest.response);
				DocumentFile savePath = FileUtils.getSavePath(getApplicationContext(), processHolder.group);

				if (!mainRequestJSON.getBoolean(Keyword.RESULT)) {
					String errorCode = mainRequestJSON.has(Keyword.ERROR)
							? mainRequestJSON.getString(Keyword.ERROR)
							: null;

					if (Keyword.ERROR_NOT_FOUND.equals(errorCode)) {
						ContentValues contentValues = new ContentValues();

						contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransactionObject.Flag.REMOVED.toString());

						getDatabase().update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
										.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(processHolder.group.groupId)),
								contentValues);
					}

					getNotificationHelper().notifyConnectionError(mTransfer, errorCode);
				} else {
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
						processHolder.currentFile = FileUtils.getIncomingTransactionFile(getApplicationContext(), processHolder.transactionObject, processHolder.group);

						getNotificationHelper().notifyFileTransaction(processHolder);

						StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), processHolder.currentFile.getUri());

						processHolder.transferHandler = mReceive.receive(0, streamInfo.openOutputStream(), processHolder.transactionObject.fileSize, AppConfig.BUFFER_LENGTH_DEFAULT, AppConfig.DEFAULT_SOCKET_TIMEOUT, processHolder, true);

						if (CoolTransfer.Flag.CANCEL_ALL.equals(processHolder.transferHandler.getFlag()))
							break;
					}

					if (processHolder.transferHandler != null && CoolTransfer.Flag.CONTINUE.equals(processHolder.transferHandler.getFlag()))
						getNotificationHelper().notifyFileReceived(processHolder, mTransfer.getDevice(), savePath);
				}

				activeConnection.reply(new JSONObject().put(Keyword.RESULT, false).toString());
			} catch (Exception e) {
				e.printStackTrace();
				getNotificationHelper().notifyConnectionError(mTransfer, null);
			} finally {
				try {
					if (activeConnection != null && !activeConnection.getSocket().isClosed()) {
						activeConnection.getSocket().getOutputStream().close();
						activeConnection.getSocket().getInputStream().close();
						activeConnection.getSocket().close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			Log.d(TAG, "We have exited");
		}
	}

	public class Receive extends CoolTransfer.Receive<ProcessHolder>
	{
		@Override
		public Flag onError(TransferHandler<ProcessHolder> handler, Exception error)
		{
			error.printStackTrace();

			handler.getExtra().transactionObject.flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().update(handler.getExtra().transactionObject);
			getNotificationHelper().notifyReceiveError(handler.getExtra().transactionObject);

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onNotify(TransferHandler<ProcessHolder> handler, int percentage)
		{
			handler.getExtra().notification.setContentText(getString(R.string.text_remainingTime, TimeUtils.getDuration(handler.getTransferProgress().getTimeRemaining())));
			handler.getExtra().notification.updateProgress(100, percentage, false);
		}

		@Override
		public void onTransferCompleted(TransferHandler<ProcessHolder> handler)
		{
			getDatabase().remove(handler.getExtra().transactionObject);

			DocumentFile currentFile = handler.getExtra().currentFile;

			if (currentFile.getParentFile() != null)
				try {
					handler.getExtra().currentFile = FileUtils.saveReceivedFile(currentFile.getParentFile(), currentFile, handler.getExtra().transactionObject);
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		@Override
		public void onInterrupted(TransferHandler<ProcessHolder> handler)
		{
			handler.getExtra().notification.cancel();
			handler.getExtra().transactionObject.flag = TransactionObject.Flag.INTERRUPTED;

			getDatabase().update(handler.getExtra().transactionObject);
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

				jsonObject.put(Keyword.TRANSFER_REQUEST_ID, handler.getExtra().transactionObject.requestId);
				jsonObject.put(Keyword.TRANSFER_GROUP_ID, handler.getExtra().transactionObject.groupId);
				jsonObject.put(Keyword.TRANSFER_SOCKET_PORT, serverSocket.getLocalPort());
				jsonObject.put(Keyword.RESULT, true);

				long currentSize = handler.getExtra().currentFile.length();

				if (currentSize > 0) {
					jsonObject.put(Keyword.SKIPPED_BYTES, currentSize);
					handler.skipBytes(currentSize);
				}

				handler.getExtra().activeConnection.reply(jsonObject.toString());

				JSONObject response = new JSONObject(handler.getExtra().activeConnection.receive().response);

				if (response.getBoolean(Keyword.RESULT))
					return Flag.CONTINUE;
				else if (response.has(Keyword.FLAG) && Keyword.FLAG_GROUP_EXISTS.equals(response.getString(Keyword.FLAG))) {
					if (response.has(Keyword.ERROR) && response.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_FOUND)) {
						handler.getExtra().transactionObject.flag = TransactionObject.Flag.REMOVED;
						getDatabase().update(handler.getExtra().transactionObject);
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

			if (handler.getTransferProgress().getTotalByte() == 0) {
				TransactionObject.Group.Index indexInstance = new TransactionObject.Group.Index();

				getDatabase().calculateTransactionSize(handler.getExtra().transactionObject.groupId, indexInstance);

				handler.getTransferProgress().setTotalByte(indexInstance.incoming);
			}

			return Flag.CONTINUE;
		}

		@Override
		public void onProcessListChanged(ArrayList<TransferHandler<ProcessHolder>> processList, TransferHandler<ProcessHolder> handler, boolean isAdded)
		{
			super.onProcessListChanged(processList, handler, isAdded);

			if (!isAdded) {
				DocumentFile currentFile = handler.getExtra().currentFile;

				if (currentFile instanceof LocalDocumentFile && mMediaScanner.isConnected())
					mMediaScanner.scanFile(((LocalDocumentFile) currentFile).getFile().getAbsolutePath(), handler.getExtra().transactionObject.fileMimeType);

				if (currentFile.getParentFile() != null)
					sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
							.putExtra(FileListFragment.EXTRA_FILE_PARENT, currentFile.getParentFile().getUri())
							.putExtra(FileListFragment.EXTRA_FILE_NAME, currentFile.getName()));
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

			getDatabase().update(handler.getExtra().transactionObject);

			return Flag.CANCEL_ALL;
		}

		@Override
		public void onNotify(TransferHandler<ProcessHolder> handler, int percentage)
		{
			handler.getExtra().notification.setContentText(getString(R.string.text_remainingTime, TimeUtils.getDuration(handler.getTransferProgress().getTimeRemaining())));
			handler.getExtra().notification.updateProgress(100, percentage, false);
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

			if (handler.getTransferProgress().getTotalByte() == 0) {
				TransactionObject.Group.Index indexInstance = new TransactionObject.Group.Index();

				getDatabase().calculateTransactionSize(handler.getExtra().transactionObject.groupId, indexInstance);

				handler.getTransferProgress().setTotalByte(indexInstance.outgoing);
			}

			return Flag.CONTINUE;
		}


		@Override
		public void onOrientatingStreams(TransferHandler<ProcessHolder> handler, InputStream inputStream, OutputStream outputStream)
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

	public class ProcessHolder
	{
		public CoolTransfer.TransferHandler<ProcessHolder> transferHandler;
		public CoolSocket.ActiveConnection activeConnection;
		public TransactionObject transactionObject;
		public DynamicNotification notification;
		public TransactionObject.Group group;
		public DocumentFile currentFile;
	}
}
