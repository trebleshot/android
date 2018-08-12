package com.genonbeta.TrebleShot.service;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.util.ArrayMap;
import android.util.Log;
import android.widget.Toast;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.CoolSocket.CoolTransfer;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Service;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.exception.AssigneeNotFoundException;
import com.genonbeta.TrebleShot.exception.ConnectionNotFoundException;
import com.genonbeta.TrebleShot.exception.DeviceNotFoundException;
import com.genonbeta.TrebleShot.exception.TransactionGroupNotFoundException;
import com.genonbeta.TrebleShot.fragment.FileListFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.object.TextStreamObject;
import com.genonbeta.TrebleShot.object.TransferGroup;
import com.genonbeta.TrebleShot.object.TransferInstance;
import com.genonbeta.TrebleShot.object.TransferObject;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.CommunicationBridge;
import com.genonbeta.TrebleShot.util.CommunicationNotificationHelper;
import com.genonbeta.TrebleShot.util.DynamicNotification;
import com.genonbeta.TrebleShot.util.FileUtils;
import com.genonbeta.TrebleShot.util.HotspotUtils;
import com.genonbeta.TrebleShot.util.NetworkDeviceLoader;
import com.genonbeta.TrebleShot.util.NetworkUtils;
import com.genonbeta.TrebleShot.util.NotificationUtils;
import com.genonbeta.TrebleShot.util.NsdDiscovery;
import com.genonbeta.TrebleShot.util.TimeUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.database.CursorItem;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.io.StreamInfo;
import com.genonbeta.android.framework.util.Interrupter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
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
	public static final String ACTION_CANCEL_JOB = "com.genonbeta.TrebleShot.transaction.action.CANCEL_JOB";
	public static final String ACTION_CANCEL_KILL = "com.genonbeta.TrebleShot.transaction.action.CANCEL_KILL";
	public static final String ACTION_TOGGLE_SEAMLESS_MODE = "com.genonbeta.TrebleShot.transaction.action.TOGGLE_SEAMLESS_MODE";
	public static final String ACTION_TOGGLE_HOTSPOT = "com.genonbeta.TrebleShot.transaction.action.TOGGLE_HOTSPOT";
	public static final String ACTION_REQUEST_HOTSPOT_STATUS = "com.genonbeta.TrebleShot.transaction.action.REQUEST_HOTSPOT_STATUS";
	public static final String ACTION_HOTSPOT_STATUS = "com.genonbeta.TrebleShot.transaction.action.HOTSPOT_STATUS";
	public static final String ACTION_DEVICE_ACQUAINTANCE = "com.genonbeta.TrebleShot.transaction.action.DEVICE_ACQUAINTANCE";
	public static final String ACTION_SERVICE_STATUS = "com.genonbeta.TrebleShot.transaction.action.SERVICE_STATUS";
	public static final String ACTION_SERVICE_CONNECTION_TRANSFER_QUEUE = "com.genonbeta.TrebleShot.transaction.action.SERVICE_CONNECTION_TRANSFER_QUEUE";

	public static final String EXTRA_DEVICE_ID = "extraDeviceId";
	public static final String EXTRA_STATUS_STARTED = "extraStatusStarted";
	public static final String EXTRA_CONNECTION_ADAPTER_NAME = "extraConnectionAdapterName";
	public static final String EXTRA_REQUEST_ID = "extraRequestId";
	public static final String EXTRA_CLIPBOARD_ID = "extraTextId";
	public static final String EXTRA_GROUP_ID = "extraGroupId";
	public static final String EXTRA_IS_ACCEPTED = "extraAccepted";
	public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";
	public static final String EXTRA_HOTSPOT_ENABLED = "extraHotspotEnabled";
	public static final String EXTRA_HOTSPOT_NAME = "extraHotspotName";
	public static final String EXTRA_HOTSPOT_KEY_MGMT = "extraHotspotKeyManagement";
	public static final String EXTRA_HOTSPOT_PASSWORD = "extraHotspotPassword";

	private ArrayList<ProcessHolder> mActiveProcessList = new ArrayList<>();
	private CommunicationServer mCommunicationServer = new CommunicationServer();
	private SeamlessServer mSeamlessServer = new SeamlessServer();
	private ArrayMap<Long, Interrupter> mOngoingIndexList = new ArrayMap<>();
	private Receive mReceive = new Receive();
	private Send mSend = new Send();
	private ExecutorService mSelfExecutor = Executors.newFixedThreadPool(10);
	private NsdDiscovery mNsdDiscovery;
	private CommunicationNotificationHelper mNotificationHelper;
	private WifiManager.WifiLock mWifiLock;
	private MediaScannerConnection mMediaScanner;
	private HotspotUtils mHotspotUtils;

	private boolean mDestroyApproved = false;
	private boolean mSeamlessMode = false;

	@Override
	public IBinder onBind(Intent intent)
	{
		if (intent != null)
			if (ACTION_SERVICE_CONNECTION_TRANSFER_QUEUE.equals(intent.getAction()))
				return new Binder()
				{
					@Override
					protected boolean onTransact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags) throws RemoteException
					{

						reply.writeString("I mean it works");
						return true;
					}
				};

		return null;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();

		mNotificationHelper = new CommunicationNotificationHelper(getNotificationUtils());
		mNsdDiscovery = new NsdDiscovery(getApplicationContext(), getDatabase(), getDefaultPreferences());
		mMediaScanner = new MediaScannerConnection(this, null);
		mHotspotUtils = HotspotUtils.getInstance(this);
		mWifiLock = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE))
				.createWifiLock(TAG);

		mReceive.setNotifyDelay(AppConfig.DEFAULT_NOTIFICATION_DELAY);
		mSend.setNotifyDelay(AppConfig.DEFAULT_NOTIFICATION_DELAY);

		mMediaScanner.connect();
		mNsdDiscovery.registerService();

		if (getWifiLock() != null)
			getWifiLock().acquire();

		updateServiceState(getDefaultPreferences().getBoolean("trust_always", false));

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

					if (getDefaultPreferences().getBoolean("hotspot_trust", false))
						updateServiceState(true);
				}
			});
	}

	@Override
	public int onStartCommand(Intent intent, int flags, final int startId)
	{
		super.onStartCommand(intent, flags, startId);

		if (intent != null)
			Log.d(TAG, "onStart() : action = " + intent.getAction());

		if (intent != null && AppUtils.checkRunningConditions(this)) {
			if (ACTION_FILE_TRANSFER.equals(intent.getAction())) {
				final String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
				final long groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1);
				final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				final boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);

				getNotificationHelper().getUtils().cancel(notificationId);

				try {
					final TransferInstance transferInstance = new TransferInstance(getDatabase(), groupId, deviceId, true);

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
						startFileReceiving(groupId, deviceId);
					else
						getDatabase().remove(transferInstance.getGroup());
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
					getDatabase().reconstruct(device);
					device.isRestricted = !isAccepted;
					getDatabase().update(device);
				} catch (Exception e) {
					e.printStackTrace();
					return START_NOT_STICKY;
				}
			} else if (ACTION_CANCEL_INDEXING.equals(intent.getAction())) {
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				long groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1);

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
					&& intent.hasExtra(EXTRA_GROUP_ID)
					&& intent.hasExtra(EXTRA_DEVICE_ID)) {
				long groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1);
				String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);

				try {
					ProcessHolder process = findProcessById(groupId, deviceId);

					if (process == null)
						startFileReceiving(groupId, deviceId);
					else
						Toast.makeText(this, getString(R.string.mesg_groupOngoingNotice, process.transferObject.friendlyName), Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else if (ACTION_CANCEL_JOB.equals(intent.getAction())
					|| ACTION_CANCEL_KILL.equals(intent.getAction())) {
				int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
				long groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1);
				String deviceId = intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID);

				ProcessHolder processHolder = findProcessById(groupId, deviceId);

				if (processHolder == null || ACTION_CANCEL_KILL.equals(intent.getAction()))
					getNotificationHelper().getUtils().cancel(notificationId);

				if (processHolder != null) {
					if (ACTION_CANCEL_KILL.equals(intent.getAction())) {
						try {
							if (processHolder.transferHandler instanceof CoolTransfer.Receive.Handler) {
								CoolTransfer.Receive.Handler receiveHandler = ((CoolTransfer.Receive.Handler) processHolder.transferHandler);

								if (receiveHandler.getServerSocket() != null)
									receiveHandler.getServerSocket().close();
							}

							if (processHolder.activeConnection.getSocket() != null)
								processHolder.activeConnection.getSocket().close();

							if (processHolder.transferHandler.getSocket() != null)
								processHolder.transferHandler.getSocket().close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
						processHolder.notification = getNotificationHelper().notifyStuckThread(processHolder);

						if (processHolder.transferHandler != null)
							processHolder.transferHandler.interrupt();
					}
				}
			} else if (ACTION_TOGGLE_SEAMLESS_MODE.equals(intent.getAction())) {
				updateServiceState(!mSeamlessMode);
			} else if (ACTION_TOGGLE_HOTSPOT.equals(intent.getAction())
					&& (Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this))) {
				setupHotspot();
			} else if (ACTION_REQUEST_HOTSPOT_STATUS.equals(intent.getAction())) {
				sendHotspotStatus(getHotspotUtils().getConfiguration());
			} else if (ACTION_SERVICE_STATUS.equals(intent.getAction())
					&& intent.hasExtra(EXTRA_STATUS_STARTED)) {
				boolean startRequested = intent.getBooleanExtra(EXTRA_STATUS_STARTED, false);

				mDestroyApproved = !startRequested && !hasOngoingTasks();

				if (mDestroyApproved)
					new Handler(Looper.getMainLooper()).postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							if (mDestroyApproved
									&& !getHotspotUtils().isStarted()
									&& !hasOngoingTasks()
									&& getDefaultPreferences().getBoolean("kill_service_on_exit", false)) {
								stopSelf();
								Log.d(TAG, "onStartCommand(): Destroy state has been applied");
							}
						}
					}, 3000);
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
		mNsdDiscovery.unregisterService();

		if (getHotspotUtils().unloadPreviousConfig()) {
			getHotspotUtils().disable();
			Log.d(TAG, "onDestroy(): Stopping hotspot (previously started)");
		}

		if (getWifiLock() != null && getWifiLock().isHeld()) {
			getWifiLock().release();
			Log.d(TAG, "onDestroy(): Releasing Wi-Fi lock");
		}

		stopForeground(true);

		synchronized (getOngoingIndexList()) {
			for (Interrupter interrupter : getOngoingIndexList().values()) {
				interrupter.interrupt(false);
				Log.d(TAG, "onDestroy(): Ongoing indexing stopped: " + interrupter.toString());
			}
		}

		synchronized (getActiveProcessList()) {
			for (ProcessHolder processHolder : getActiveProcessList())
				if (processHolder.transferHandler != null) {
					processHolder.transferHandler.interrupt();
					Log.d(TAG, "onDestroy(): Killing sending process: " + processHolder.transferHandler.toString());
				}
		}
	}

	public boolean hasOngoingTasks()
	{
		return mCommunicationServer.getConnections().size() > 0
				|| getOngoingIndexList().size() > 0
				|| getActiveProcessList().size() > 0;
	}

	public ProcessHolder findProcessById(long groupId, String deviceId)
	{
		synchronized (getActiveProcessList()) {
			for (ProcessHolder processHolder : getActiveProcessList())
				if (processHolder.group.groupId == groupId
						&& deviceId.equals(processHolder.assignee.deviceId))
					return processHolder;
		}

		return null;
	}

	public synchronized ArrayList<ProcessHolder> getActiveProcessList()
	{
		return mActiveProcessList;
	}

	public HotspotUtils getHotspotUtils()
	{
		return mHotspotUtils;
	}

	public CommunicationNotificationHelper getNotificationHelper()
	{
		return mNotificationHelper;
	}

	public synchronized ArrayMap<Long, Interrupter> getOngoingIndexList()
	{
		return mOngoingIndexList;
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
		return getDefaultPreferences().getBoolean("qr_trust", false);
	}

	public boolean isProcessRunning(int groupId, String deviceId)
	{
		return findProcessById(groupId, deviceId) != null;
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
		boolean overrideTrustZone = getDefaultPreferences().getBoolean("hotspot_trust", false);

		// On Oreo devices, we will use platform specific code.
		if (overrideTrustZone && (!isEnabled || Build.VERSION.SDK_INT < 26)) {
			updateServiceState(isEnabled);
			Log.d(TAG, "setupHotspot(): Start with TrustZone");
		}

		if (isEnabled)
			getHotspotUtils().enableConfigured(AppUtils.getHotspotName(this), null);
		else {
			getHotspotUtils().disable();

			if (Build.VERSION.SDK_INT >= 26)
				sendHotspotStatus(null);
		}
	}

	public void startFileReceiving(long groupId, String deviceId) throws TransactionGroupNotFoundException, DeviceNotFoundException, ConnectionNotFoundException, AssigneeNotFoundException
	{
		// it should create its own devices
		startFileReceiving(new TransferInstance(getDatabase(), groupId, deviceId, true));
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

				final boolean isSecureConnection = responseJSON.has(Keyword.DEVICE_SECURE_KEY)
						&& responseJSON.getInt(Keyword.DEVICE_SECURE_KEY) == getDefaultPreferences().getInt(Keyword.NETWORK_PIN, -1);

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

				final boolean seamlessActive = mSeamlessMode || (isQRFastMode() && isSecureConnection);

				if (deviceSerial != null) {
					NetworkDevice device = new NetworkDevice(deviceSerial);

					try {
						getDatabase().reconstruct(device);

						if (isSecureConnection) {
							device.isTrusted = true;
							device.isRestricted = false;
						}

						if (!device.isRestricted)
							shouldContinue = true;
					} catch (Exception e1) {
						e1.printStackTrace();

						device = NetworkDeviceLoader.load(true, getDatabase(), activeConnection.getClientAddress(), null);

						if (device == null)
							throw new Exception("Could not reach to the opposite server");

						device.isRestricted = !isSecureConnection;
						device.isTrusted = isSecureConnection;

						getDatabase().publish(device);

						shouldContinue = true;

						if (device.isRestricted)
							getNotificationHelper().notifyConnectionRequest(device);
					}

					final NetworkDevice.Connection connection = NetworkDeviceLoader.processConnection(getDatabase(), device, activeConnection.getClientAddress());

					if (!shouldContinue)
						replyJSON.put(Keyword.ERROR, Keyword.ERROR_NOT_ALLOWED);
					else if (responseJSON.has(Keyword.REQUEST)) {
						switch (responseJSON.getString(Keyword.REQUEST)) {
							case (Keyword.REQUEST_TRANSFER):
								if (responseJSON.has(Keyword.FILES_INDEX) && responseJSON.has(Keyword.TRANSFER_GROUP_ID) && getOngoingIndexList().size() < 1) {
									String jsonIndex = responseJSON.getString(Keyword.FILES_INDEX);
									final JSONArray jsonArray = new JSONArray(jsonIndex);
									final long groupId = responseJSON.getLong(Keyword.TRANSFER_GROUP_ID);
									final NetworkDevice finalDevice = device;

									result = true;

									getSelfExecutor().submit(new Runnable()
									{
										@Override
										public void run()
										{
											Interrupter interrupter = new Interrupter();
											TransferGroup group = new TransferGroup(groupId);
											TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, finalDevice, connection);

											boolean usePublishing = false;

											try {
												getDatabase().reconstruct(group);
												usePublishing = true;
											} catch (Exception e) {
												e.printStackTrace();
											}

											TransferObject transferObject = null;

											getDatabase().publish(group);
											getDatabase().publish(assignee);

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

														transferObject = new TransferObject(
																requestIndex.getLong(Keyword.TRANSFER_REQUEST_ID),
																groupId,
																requestIndex.getString(Keyword.INDEX_FILE_NAME),
																"." + UUID.randomUUID() + ".tshare",
																requestIndex.getString(Keyword.INDEX_FILE_MIME),
																requestIndex.getLong(Keyword.INDEX_FILE_SIZE),
																TransferObject.Type.INCOMING);

														if (requestIndex.has(Keyword.INDEX_DIRECTORY))
															transferObject.directory = requestIndex.getString(Keyword.INDEX_DIRECTORY);

														if (usePublishing)
															getDatabase().publish(transferObject);
														else
															getDatabase().insert(transferObject);
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
												getDatabase().remove(group);
											else if (transferObject != null && count > 0) {
												if (seamlessActive && finalDevice.isTrusted)
													try {
														startFileReceiving(group.groupId, finalDevice.deviceId);
													} catch (Exception e) {
														e.printStackTrace();
													}
												else
													getNotificationHelper().notifyTransferRequest(transferObject, finalDevice, count);

											}
										}
									});
								}
								break;
							case (Keyword.REQUEST_RESPONSE):
								if (responseJSON.has(Keyword.TRANSFER_GROUP_ID)) {
									int groupId = responseJSON.getInt(Keyword.TRANSFER_GROUP_ID);
									boolean isAccepted = responseJSON.getBoolean(Keyword.TRANSFER_IS_ACCEPTED);

									TransferGroup group = new TransferGroup(groupId);
									TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, device);

									try {
										getDatabase().reconstruct(assignee);

										if (!isAccepted)
											getDatabase().remove(assignee.isClone
													? assignee // if it is clone (later added device to transfer group)
													: group);

										result = true;
									} catch (Exception e) {
									}
								}
								break;
							case (Keyword.REQUEST_CLIPBOARD):
								if (responseJSON.has(Keyword.TRANSFER_CLIPBOARD_TEXT)) {
									TextStreamObject textStreamObject = new TextStreamObject(AppUtils.getUniqueNumber(), responseJSON.getString(Keyword.TRANSFER_CLIPBOARD_TEXT));

									getDatabase().publish(textStreamObject);
									getNotificationHelper().notifyClipboardRequest(device, textStreamObject);

									result = true;
								}
								break;
							case (Keyword.REQUEST_ACQUAINTANCE):
								sendBroadcast(new Intent(ACTION_DEVICE_ACQUAINTANCE)
										.putExtra(EXTRA_DEVICE_ID, device.deviceId)
										.putExtra(EXTRA_CONNECTION_ADAPTER_NAME, connection.adapterName));

								result = true;
								break;
							case (Keyword.REQUEST_HANDSHAKE):
								result = true;
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

			synchronized (getActiveProcessList()) {
				getActiveProcessList().add(processHolder);
			}

			try {
				ActiveConnection.Response mainRequest = activeConnection.receive();

				int groupId = new JSONObject(mainRequest.response)
						.getInt(Keyword.TRANSFER_GROUP_ID);

				activeConnection.setId(groupId);

				TransferInstance transferInstance = new TransferInstance(getDatabase(), groupId, activeConnection.getClientAddress(), false);

				activeConnection.reply(new JSONObject().put(Keyword.RESULT, true).toString());

				processHolder.group = transferInstance.getGroup();
				processHolder.assignee = transferInstance.getAssignee();
				processHolder.activeConnection = activeConnection;

				while (true) {
					ActiveConnection.Response currentResponse = activeConnection.receive();

					if (currentResponse.response == null || currentResponse.totalLength < 1) {
						Log.d(TAG, "SeamlessServer.onConnected(): NULL response was received exiting loop");
						break;
					}

					JSONObject currentRequest = new JSONObject(currentResponse.response);
					JSONObject currentReply = new JSONObject();

					if (currentRequest.has(Keyword.RESULT) && !currentRequest.getBoolean(Keyword.RESULT)) {
						// the assignee for this transfer has received the files. We can remove it
						if (!currentRequest.has(Keyword.TRANSFER_JOB_DONE) || currentRequest.getBoolean(Keyword.TRANSFER_JOB_DONE)) {
							Log.d(TAG, "SeamlessServer.onConnected(): Removing assignee: " + processHolder.assignee.deviceId);
							getDatabase().remove(processHolder.assignee);

							ContentValues values = new ContentValues();
							values.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransferObject.Flag.PENDING.toString());

							getDatabase().update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
									.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(transferInstance.getGroup().groupId)), values);
						}

						break;
					}

					StreamInfo streamInfo = null;

					try {
						processHolder.transferObject = new TransferObject(currentRequest.getInt(Keyword.TRANSFER_REQUEST_ID));

						getDatabase().reconstruct(processHolder.transferObject);

						processHolder.transferObject.accessPort = currentRequest.getInt(Keyword.TRANSFER_SOCKET_PORT);

						if (currentRequest.has(Keyword.SKIPPED_BYTES)) {
							processHolder.transferObject.skippedBytes = currentRequest.getInt(Keyword.SKIPPED_BYTES);
							Log.d(TAG, "SeamlessServes.onConnected(): Has skipped bytes: " + processHolder.transferObject.skippedBytes);
						}

						getDatabase().update(processHolder.transferObject);

						currentReply.put(Keyword.RESULT, true);

						try {
							streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), Uri.parse(processHolder.transferObject.file));
						} catch (Exception e) {
							Log.d(TAG, "SeamlessServer.onConnected(): File is not accessible ? " + processHolder.transferObject.friendlyName);

							currentReply.put(Keyword.RESULT, false);
							currentReply.put(Keyword.ERROR, Keyword.ERROR_NOT_ACCESSIBLE);
							currentReply.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);

							processHolder.transferObject.flag = TransferObject.Flag.INTERRUPTED;
						}
					} catch (Exception e) {
						Log.d(TAG, "SeamlessServer.onConnected(): Exception is handled: " + e.toString());

						currentReply.put(Keyword.RESULT, false);
						currentReply.put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND);
						currentReply.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);

						processHolder.transferObject.flag = TransferObject.Flag.INTERRUPTED;
					} finally {
						activeConnection.reply(currentReply.toString());
					}

					if (streamInfo != null) {
						Log.d(TAG, "SeamlessServer.onConnected(): Proceeding to send");

						getNotificationHelper().notifyFileTransaction(processHolder);

						mSend.send(activeConnection.getClientAddress(), processHolder.transferObject.accessPort, streamInfo.openInputStream(), streamInfo.size, AppConfig.BUFFER_LENGTH_DEFAULT, processHolder, true);
					}


					// We are now updating instances always at the end because it will be
					// changed by the process itself naturally
					if (processHolder.transferObject != null) {
						Log.d(TAG, "SeamlessServer.onConnected(): Updating file instances to " + processHolder.transferObject.flag.toString());
						getDatabase().update(processHolder.transferObject);
					}

					if (processHolder.transferHandler != null
							&& processHolder.transferHandler.getFlag().equals(CoolTransfer.Flag.CANCEL_ALL)) {
						Log.d(TAG, "SeamlessServer.onConnected(): Cancelled all");
						break;
					}
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

					Log.d(TAG, "SeamlessServer.onConnected(): Exception status is notified");
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

				synchronized (getActiveProcessList()) {
					getActiveProcessList().remove(processHolder);
				}
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

			synchronized (getActiveProcessList()) {
				getActiveProcessList().add(processHolder);
			}

			try {
				Boolean initialConnectionOkay = CommunicationBridge.connect(getDatabase(), Boolean.class, new CommunicationBridge.Client.ConnectionHandler()
				{
					@Override
					public void onConnect(CommunicationBridge.Client client)
					{
						client.setDevice(mTransfer.getDevice());

						try {
							CoolSocket.ActiveConnection initialConnection = client.communicate(mTransfer.getDevice(), mTransfer.getConnection());

							initialConnection.reply(new JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_HANDSHAKE).toString());

							JSONObject resultObject = new JSONObject(initialConnection.receive().response);

							Log.d(TAG, "SeamlessClientHandler.onConnect(): Initial connection response: " + resultObject.toString());

							client.setReturn(resultObject.getBoolean(Keyword.RESULT));

							return;
						} catch (Exception e) {
							e.printStackTrace();
						}

						client.setReturn(false);
					}
				});

				if (initialConnectionOkay == null || !initialConnectionOkay) {
					Log.d(TAG, "SeamlessClientHandler.onConnect(): Initial connection failed.");
					throw new Exception("Initial connection failed");
				}

				activeConnection = client.connect(new InetSocketAddress(mTransfer.getConnection().ipAddress, AppConfig.SERVER_PORT_SEAMLESS), AppConfig.DEFAULT_SOCKET_TIMEOUT);

				activeConnection.reply(new JSONObject()
						.put(Keyword.TRANSFER_GROUP_ID, mTransfer.getGroup().groupId)
						.toString());

				CoolSocket.ActiveConnection.Response mainRequest = activeConnection.receive();

				processHolder.activeConnection = activeConnection;
				processHolder.group = mTransfer.getGroup();
				processHolder.assignee = mTransfer.getAssignee();

				JSONObject mainRequestJSON = new JSONObject(mainRequest.response);
				DocumentFile savePath = FileUtils.getSavePath(getApplicationContext(), getDefaultPreferences(), processHolder.group);

				if (!mainRequestJSON.getBoolean(Keyword.RESULT)) {
					Log.d(TAG, "SeamlessClientHandler.onConnect(): false result, it will exit.");

					String errorCode = mainRequestJSON.has(Keyword.ERROR)
							? mainRequestJSON.getString(Keyword.ERROR)
							: null;

					if (Keyword.ERROR_NOT_FOUND.equals(errorCode)) {
						ContentValues contentValues = new ContentValues();

						contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransferObject.Flag.REMOVED.toString());

						getDatabase().update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
								.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(processHolder.group.groupId)), contentValues);
					}

					getNotificationHelper().notifyConnectionError(mTransfer, errorCode);
				} else {
					while (true) {
						try {
							// Remove the previous object as it is completed.
							CursorItem receiverInstance = getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
									.setWhere(AccessDatabase.FIELD_TRANSFER_TYPE + "=? AND " + AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_FLAG + "=?",
											TransferObject.Type.INCOMING.toString(),
											String.valueOf(processHolder.group.groupId),
											TransferObject.Flag.PENDING.toString()));

						/*
						if (receiverInstance == null
								&& getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
								.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=?", String.valueOf(processHolder.group.groupId))) == null) {
							getDatabase().remove(processHolder.group);
							Log.d(TAG, "SeamlessClientHandler(): Removing group because there is no file instance left");
							break;
						}*/

							if (receiverInstance == null) {
								Log.d(TAG, "SeamlessClientHandler(): Exiting because there is no pending file instance left");
								break;
							}

							processHolder.transferObject = new TransferObject(receiverInstance);
							processHolder.currentFile = FileUtils.getIncomingTransactionFile(getApplicationContext(), getDefaultPreferences(), processHolder.transferObject, processHolder.group);

							getNotificationHelper().notifyFileTransaction(processHolder);

							StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), processHolder.currentFile.getUri());

							mReceive.receive(0, streamInfo.openOutputStream(), processHolder.transferObject.fileSize, AppConfig.BUFFER_LENGTH_DEFAULT, AppConfig.DEFAULT_SOCKET_TIMEOUT, processHolder, true);

							if (CoolTransfer.Flag.CANCEL_ALL.equals(processHolder.transferHandler.getFlag())) {
								Log.d(TAG, "SeamlessClientHandler.onConnect(): Cancel is requested. Exiting.");
								break;
							}
						} catch (Exception e) {
							// Throw the error again. It is for making sure that the latest known instance
							// of the object is saved properly on any condition
							throw e;
						} finally {
							if (processHolder.transferObject != null) {
								// We are now updating instances always at the end because it will be
								// changed by the process itself naturally
								Log.d(TAG, "SeamlessClientHandler.onConnect(): Updating file instances to " + processHolder.transferObject.flag.toString());
								getDatabase().update(processHolder.transferObject);
							}
						}
					}

					// Check if all the pending files are flagged with Flag.DONE
					boolean isJobDone = CoolTransfer.Flag.CONTINUE.equals(processHolder.transferHandler.getFlag());
					boolean hasLeftFiles = getDatabase().getFirstFromTable(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFER)
							.setWhere(AccessDatabase.FIELD_TRANSFER_GROUPID + "=? AND " + AccessDatabase.FIELD_TRANSFER_FLAG + " != ?",
									String.valueOf(processHolder.group.groupId), TransferObject.Flag.DONE.toString())) == null;

					activeConnection.reply(new JSONObject()
							.put(Keyword.RESULT, false)
							.put(Keyword.TRANSFER_JOB_DONE, isJobDone && hasLeftFiles)
							.toString());

					if (processHolder.transferHandler != null && isJobDone) {
						getNotificationHelper().notifyFileReceived(processHolder, mTransfer.getDevice(), savePath);
						Log.d(TAG, "SeamlessClientHandler.onConnect(): Notify user");
					} else {
						// If there was an error it should be handled by showing another error notification
						// most of which are seemingly potential headache in the future
						processHolder.notification.cancel();
						Log.d(TAG, "SeamlessClientHandler.onConnect(): Removing notification an error is already notified");
					}
				}
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

				synchronized (getActiveProcessList()) {
					getActiveProcessList().remove(processHolder);
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
			handleError(handler, error);
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
			handler.getExtra().transferObject.flag = TransferObject.Flag.DONE;
			DocumentFile currentFile = handler.getExtra().currentFile;

			if (currentFile.getParentFile() != null)
				try {
					handler.getExtra().currentFile = FileUtils.saveReceivedFile(currentFile.getParentFile(), currentFile, handler.getExtra().transferObject);
					handler.getExtra().transferObject.file = handler.getExtra().currentFile.getName();
					Log.d(TAG, "Receive.onTransferCompleted(): Saved as: " + handler.getExtra().currentFile.getName());
				} catch (IOException e) {
					e.printStackTrace();
				}
		}

		@Override
		public void onInterrupted(TransferHandler<ProcessHolder> handler)
		{
			handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
		}

		@Override
		public Flag onCloseStreams(TransferHandler<ProcessHolder> handler)
		{
			try {
				handler.getExtra().currentFile.sync();

				if (handler.getExtra().currentFile.length() == handler.getExtra().transferObject.fileSize)
					return super.onCloseStreams(handler);
			} catch (Exception e) {
				e.printStackTrace();
			}

			handler.interrupt();
			return Flag.CANCEL_ALL;
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

				jsonObject.put(Keyword.TRANSFER_REQUEST_ID, handler.getExtra().transferObject.requestId);
				jsonObject.put(Keyword.TRANSFER_GROUP_ID, handler.getExtra().transferObject.groupId);
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
						handler.getExtra().transferObject.flag = TransferObject.Flag.REMOVED;
						Log.d(TAG, "Receive.onStart(): Sender says it does not have the file defined");
					} else if (response.has(Keyword.ERROR) && response.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_ACCESSIBLE)) {
						handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
						Log.d(TAG, "Receive.onStart(): Sender says it can't open the file");
					}

					return Flag.CANCEL_CURRENT;
				}
			} catch (Exception e) {
				e.printStackTrace();
				onError(handler, e);
			}

			return Flag.CANCEL_ALL;
		}

		@Override
		public Flag onStart(TransferHandler<ProcessHolder> handler)
		{
			// set transfer handler here
			handler.linkTo(handler.getExtra().transferHandler);
			handler.getExtra().transferHandler = handler;

			if (handler.getTransferProgress().getTotalByte() == 0) {
				TransferGroup.Index indexInstance = new TransferGroup.Index();

				getDatabase().calculateTransactionSize(handler.getExtra().transferObject.groupId, indexInstance);

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
					mMediaScanner.scanFile(((LocalDocumentFile) currentFile).getFile().getAbsolutePath(), handler.getExtra().transferObject.fileMimeType);

				if (currentFile.getParentFile() != null)
					sendBroadcast(new Intent(FileListFragment.ACTION_FILE_LIST_CHANGED)
							.putExtra(FileListFragment.EXTRA_FILE_PARENT, currentFile.getParentFile().getUri())
							.putExtra(FileListFragment.EXTRA_FILE_NAME, currentFile.getName()));
			}
		}

		protected void handleError(TransferHandler<ProcessHolder> handler, Exception error)
		{
			error.printStackTrace();

			handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
			getNotificationHelper().notifyReceiveError(handler.getExtra().transferObject);
		}

	}

	public class Send extends CoolTransfer.Send<ProcessHolder>
	{
		@Override
		public Flag onError(TransferHandler<ProcessHolder> handler, Exception error)
		{
			error.printStackTrace();
			handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;

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
			handler.getExtra().transferObject.flag = TransferObject.Flag.DONE;
		}

		@Override
		public void onInterrupted(TransferHandler<ProcessHolder> handler)
		{
			handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
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
			handler.getExtra().transferHandler = handler;

			if (handler.getTransferProgress().getTotalByte() == 0) {
				TransferGroup.Index indexInstance = new TransferGroup.Index();

				getDatabase().calculateTransactionSize(handler.getExtra().transferObject.groupId, indexInstance);

				handler.getTransferProgress().setTotalByte(indexInstance.outgoing);
			}

			return Flag.CONTINUE;
		}


		@Override
		public void onOrientatingStreams(TransferHandler<ProcessHolder> handler, InputStream inputStream, OutputStream outputStream)
		{
			super.onOrientatingStreams(handler, inputStream, outputStream);

			if (handler.getExtra().transferObject.skippedBytes > 0)
				try {
					handler.skipBytes(handler.getExtra().transferObject.skippedBytes);
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
		public TransferObject transferObject;
		public DynamicNotification notification;
		public TransferGroup group;
		public TransferGroup.Assignee assignee;
		public DocumentFile currentFile;
	}
}
