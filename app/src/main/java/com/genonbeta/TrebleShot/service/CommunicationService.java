package com.genonbeta.TrebleShot.service;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.collection.ArrayMap;

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
import com.genonbeta.TrebleShot.exception.TransferGroupNotFoundException;
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
import com.genonbeta.TrebleShot.util.TransferUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.SQLiteDatabase;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.io.DocumentFile;
import com.genonbeta.android.framework.io.LocalDocumentFile;
import com.genonbeta.android.framework.io.StreamInfo;
import com.genonbeta.android.framework.util.Interrupter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StreamCorruptedException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import fi.iki.elonen.NanoHTTPD;

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
    public static final String ACTION_TOGGLE_SEAMLESS_MODE = "com.genonbeta.TrebleShot.transaction.action.TOGGLE_SEAMLESS_MODE";
    public static final String ACTION_REVOKE_ACCESS_PIN = "com.genonbeta.TrebleShot.transaction.action.REVOKE_ACCESS_PIN";
    public static final String ACTION_TOGGLE_HOTSPOT = "com.genonbeta.TrebleShot.transaction.action.TOGGLE_HOTSPOT";
    public static final String ACTION_REQUEST_HOTSPOT_STATUS = "com.genonbeta.TrebleShot.transaction.action.REQUEST_HOTSPOT_STATUS";
    public static final String ACTION_HOTSPOT_STATUS = "com.genonbeta.TrebleShot.transaction.action.HOTSPOT_STATUS";
    public static final String ACTION_DEVICE_ACQUAINTANCE = "com.genonbeta.TrebleShot.transaction.action.DEVICE_ACQUAINTANCE";
    public static final String ACTION_SERVICE_STATUS = "com.genonbeta.TrebleShot.transaction.action.SERVICE_STATUS";
    public static final String ACTION_SERVICE_CONNECTION_TRANSFER_QUEUE = "com.genonbeta.TrebleShot.transaction.action.SERVICE_CONNECTION_TRANSFER_QUEUE";
    public static final String ACTION_TASK_STATUS_CHANGE = "com.genonbeta.TrebleShot.transaction.action.TASK_STATUS_CHANGE";
    public static final String ACTION_TASK_RUNNING_LIST_CHANGE = "com.genonbeta.TrebleShot.transaction.action.TASK_RUNNNIG_LIST_CHANGE";
    public static final String ACTION_REQUEST_TASK_STATUS_CHANGE = "com.genonbeta.TrebleShot.transaction.action.REQUEST_TASK_STATUS_CHANGE";
    public static final String ACTION_REQUEST_TASK_RUNNING_LIST_CHANGE = "com.genonbeta.TrebleShot.transaction.action.REQUEST_TASK_RUNNING_LIST_CHANGE";
    public static final String ACTION_INCOMING_TRANSFER_READY = "com.genonbeta.TrebleShot.transaction.action.INCOMING_TRANSFER_READY";
    public static final String ACTION_TRUSTZONE_STATUS = "com.genonbeta.TrebleShot.transaction.action.TRUSTZONE_STATUS";
    public static final String ACTION_REQUEST_TRUSTZONE_STATUS = "com.genonbeta.TrebleShot.transaction.action.REQUEST_TRUSTZONE_STATUS";
    public static final String ACTION_TOGGLE_WEBSHARE = "com.genonbeta.TrebleShot.transaction.action.TOGGLE_WEBSHARE";
    public static final String ACTION_WEBSHARE_STATUS = "com.genonbeta.TrebleShot.transaction.action.WEBSHARE_STATUS";
    public static final String ACTION_REQUEST_WEBSHARE_STATUS = "com.genonbeta.TrebleShot.transaction.action.REQUEST_WEBSHARE_STATUS";

    public static final String EXTRA_DEVICE_ID = "extraDeviceId";
    public static final String EXTRA_STATUS_STARTED = "extraStatusStarted";
    public static final String EXTRA_CONNECTION_ADAPTER_NAME = "extraConnectionAdapterName";
    public static final String EXTRA_REQUEST_ID = "extraRequestId";
    public static final String EXTRA_CLIPBOARD_ID = "extraTextId";
    public static final String EXTRA_GROUP_ID = "extraGroupId";
    public static final String EXTRA_IS_ACCEPTED = "extraAccepted";
    public static final String EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted";
    public static final String EXTRA_HOTSPOT_ENABLED = "extraHotspotEnabled";
    public static final String EXTRA_HOTSPOT_DISABLING = "extraHotspotDisabling";
    public static final String EXTRA_HOTSPOT_NAME = "extraHotspotName";
    public static final String EXTRA_HOTSPOT_KEY_MGMT = "extraHotspotKeyManagement";
    public static final String EXTRA_HOTSPOT_PASSWORD = "extraHotspotPassword";
    public static final String EXTRA_TASK_CHANGE_TYPE = "extraTaskChangeType";
    public static final String EXTRA_TASK_LIST_RUNNING = "extraTaskListRunning";
    public static final String EXTRA_DEVICE_LIST_RUNNING = "extraDeviceListRunning";
    public static final String EXTRA_TOGGLE_WEBSHARE_START_ALWAYS = "extraToggleWebShareStartAlways";

    public static final int TASK_STATUS_ONGOING = 0;
    public static final int TASK_STATUS_STOPPED = 1;

    private List<ProcessHolder> mActiveProcessList = new ArrayList<>();
    private CommunicationServer mCommunicationServer = new CommunicationServer();
    private WebShareServer mWebShareServer = null;
    private SeamlessServer mSeamlessServer = new SeamlessServer();
    private Map<Long, Interrupter> mOngoingIndexList = new ArrayMap<>();
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
    private boolean mPinAccess = false;

    @Override
    public IBinder onBind(Intent intent)
    {
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
        mWifiLock = ((WifiManager) getApplicationContext()
                .getSystemService(Service.WIFI_SERVICE))
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


        try {
            mWebShareServer = new WebShareServer(this, AppConfig.SERVER_PORT_WEBSHARE);
            mWebShareServer.setAsyncRunner(new WebShareServer.BoundRunner(
                    Executors.newFixedThreadPool(AppConfig.WEB_SHARE_CONNECTION_MAX)));
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start Web Share Server");
        }
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

                                activeConnection.receive();
                                activeConnection.getSocket().close();
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
                long clipboardId = intent.getLongExtra(EXTRA_CLIPBOARD_ID, -1);
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
                    && intent.hasExtra(EXTRA_GROUP_ID)
                    && intent.hasExtra(EXTRA_DEVICE_ID)) {
                int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
                long groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1);
                String deviceId = intent.getStringExtra(CommunicationService.EXTRA_DEVICE_ID);

                ProcessHolder processHolder = findProcessById(groupId, deviceId);

                if (processHolder == null) {
                    notifyTaskStatusChange(groupId, deviceId, TASK_STATUS_STOPPED);
                    getNotificationHelper().getUtils().cancel(notificationId);
                } else {
                    processHolder.notification = getNotificationHelper().notifyStuckThread(processHolder);

                    if (!processHolder.builder.getTransferProgress().isInterrupted()) {
                        processHolder.builder.getTransferProgress().interrupt();
                    } else {
                        try {
                            if (processHolder.builder instanceof CoolTransfer.Receive.Builder) {
                                CoolTransfer.Receive.Builder receiveBuilder = (CoolTransfer.Receive.Builder) processHolder.builder;

                                if (receiveBuilder.getServerSocket() != null)
                                    receiveBuilder.getServerSocket().close();
                            }
                        } catch (Exception e) {
                            // do nothing
                        }

                        try {
                            if (processHolder.activeConnection != null
                                    && processHolder.activeConnection.getSocket() != null)
                                processHolder.activeConnection.getSocket().close();
                        } catch (IOException e) {
                            // do nothing
                        }

                        try {
                            if (processHolder.builder.getSocket() != null)
                                processHolder.builder.getSocket().close();
                        } catch (IOException e) {
                            // do nothing
                        }
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

                mDestroyApproved = !startRequested && !hasOngoingTasks() && (mWebShareServer == null
                        || !mWebShareServer.isAlive());

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
            } else if (ACTION_REQUEST_TASK_STATUS_CHANGE.equals(intent.getAction())
                    && intent.hasExtra(EXTRA_GROUP_ID)
                    && intent.hasExtra(EXTRA_DEVICE_ID)) {
                long groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1);
                String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);

                notifyTaskStatusChange(groupId, deviceId, findProcessById(groupId, deviceId) == null
                        ? TASK_STATUS_STOPPED
                        : TASK_STATUS_ONGOING);
            } else if (ACTION_REQUEST_TASK_RUNNING_LIST_CHANGE.equals(intent.getAction())) {
                notifyTaskRunningListChange();
            } else if (ACTION_REVOKE_ACCESS_PIN.equals(intent.getAction())) {
                revokePinAccess();
                refreshServiceState();
            } else if (ACTION_REQUEST_TRUSTZONE_STATUS.equals(intent.getAction())) {
                sendTrustZoneStatus();
            } else if (ACTION_REQUEST_WEBSHARE_STATUS.equals(intent.getAction())) {
                sendWebShareStatus();
            } else if (ACTION_TOGGLE_WEBSHARE.equals(intent.getAction())) {
                if (intent.hasExtra(EXTRA_TOGGLE_WEBSHARE_START_ALWAYS))
                    setWebShareEnabled(intent.getBooleanExtra(EXTRA_TOGGLE_WEBSHARE_START_ALWAYS,
                            false), true);
                else
                    toggleWebShare();
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

        {
            ContentValues values = new ContentValues();

            values.put(AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB, 0);
            getDatabase().update(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP)
                    .setWhere(String.format("%s = ?", AccessDatabase.FIELD_TRANSFERGROUP_ISSHAREDONWEB),
                            String.valueOf(1)), values);
        }

        setWebShareEnabled(false, false);
        sendTrustZoneStatus();

        if (getHotspotUtils().unloadPreviousConfig()) {
            getHotspotUtils().disable();
            Log.d(TAG, "onDestroy(): Stopping hotspot (previously started)");
        }

        if (getWifiLock() != null && getWifiLock().isHeld()) {
            getWifiLock().release();
            Log.d(TAG, "onDestroy(): Releasing Wi-Fi lock");
        }

        revokePinAccess();
        stopForeground(true);

        synchronized (getOngoingIndexList()) {
            for (Interrupter interrupter : getOngoingIndexList().values()) {
                interrupter.interrupt(false);
                Log.d(TAG, "onDestroy(): Ongoing indexing stopped: " + interrupter.toString());
            }
        }

        synchronized (getActiveProcessList()) {
            for (ProcessHolder processHolder : getActiveProcessList())
                if (processHolder.builder != null) {
                    processHolder.builder.getTransferProgress().interrupt();
                    Log.d(TAG, "onDestroy(): Killing sending process: " + processHolder.builder.toString());
                }
        }
    }

    public synchronized boolean addProcess(ProcessHolder processHolder)
    {
        return getActiveProcessList().add(processHolder);
    }

    public synchronized boolean removeProcess(ProcessHolder processHolder)
    {
        return getActiveProcessList().remove(processHolder);
    }

    public boolean hasOngoingTasks()
    {
        return mCommunicationServer.getConnections().size() > 0
                || getOngoingIndexList().size() > 0
                || getActiveProcessList().size() > 0
                || mHotspotUtils.isStarted();
    }

    public ProcessHolder findProcessById(long groupId, String deviceId)
    {
        synchronized (getActiveProcessList()) {
            for (ProcessHolder processHolder : getActiveProcessList())
                if (processHolder.groupId == groupId
                        && deviceId.equals(processHolder.deviceId))
                    return processHolder;
        }

        return null;
    }

    public synchronized List<ProcessHolder> getActiveProcessList()
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

    public synchronized Map<Long, Interrupter> getOngoingIndexList()
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

    public boolean isProcessRunning(int groupId, String deviceId)
    {
        return findProcessById(groupId, deviceId) != null;
    }

    public void notifyTaskStatusChange(long groupId, String deviceId, int state)
    {
        Intent intent = new Intent(ACTION_TASK_STATUS_CHANGE)
                .putExtra(EXTRA_TASK_CHANGE_TYPE, state)
                .putExtra(EXTRA_GROUP_ID, groupId)
                .putExtra(EXTRA_DEVICE_ID, deviceId);

        sendBroadcast(intent);
    }

    public void notifyTaskRunningListChange()
    {
        List<Long> taskList = new ArrayList<>();
        ArrayList<String> deviceList = new ArrayList<>();

        synchronized (getActiveProcessList()) {
            for (ProcessHolder processHolder : getActiveProcessList()) {
                if (processHolder.groupId != 0
                        && processHolder.deviceId != null) {
                    taskList.add(processHolder.groupId);
                    deviceList.add(processHolder.deviceId);
                }
            }
        }

        long[] taskArray = new long[taskList.size()];

        for (int i = 0; i < taskList.size(); i++)
            taskArray[i] = taskList.get(i);

        sendBroadcast(new Intent(ACTION_TASK_RUNNING_LIST_CHANGE)
                .putExtra(EXTRA_TASK_LIST_RUNNING, taskArray)
                .putStringArrayListExtra(EXTRA_DEVICE_LIST_RUNNING, deviceList));
    }

    public void refreshServiceState()
    {
        updateServiceState(mSeamlessMode);
    }

    public void revokePinAccess()
    {
        getDefaultPreferences().edit()
                .putInt(Keyword.NETWORK_PIN, -1)
                .apply();
    }

    public void sendHotspotStatusDisabling()
    {
        sendBroadcast(new Intent(ACTION_HOTSPOT_STATUS)
                .putExtra(EXTRA_HOTSPOT_ENABLED, false)
                .putExtra(EXTRA_HOTSPOT_DISABLING, true));
    }

    public void sendHotspotStatus(WifiConfiguration wifiConfiguration)
    {
        Intent statusIntent = new Intent(ACTION_HOTSPOT_STATUS)
                .putExtra(EXTRA_HOTSPOT_ENABLED, wifiConfiguration != null)
                .putExtra(EXTRA_HOTSPOT_DISABLING, false);

        if (wifiConfiguration != null) {
            statusIntent.putExtra(EXTRA_HOTSPOT_NAME, wifiConfiguration.SSID)
                    .putExtra(EXTRA_HOTSPOT_PASSWORD, wifiConfiguration.preSharedKey)
                    .putExtra(EXTRA_HOTSPOT_KEY_MGMT, NetworkUtils.getAllowedKeyManagement(wifiConfiguration));
        }

        sendBroadcast(statusIntent);
    }

    public void sendWebShareStatus()
    {
        sendBroadcast(new Intent(ACTION_WEBSHARE_STATUS)
                .putExtra(EXTRA_STATUS_STARTED, mWebShareServer.isAlive()));
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
                sendHotspotStatusDisabling();
        }
    }

    public void sendTrustZoneStatus()
    {
        sendBroadcast(new Intent(ACTION_TRUSTZONE_STATUS)
                .putExtra(EXTRA_STATUS_STARTED, mSeamlessMode));
    }

    public void startFileReceiving(long groupId, String deviceId) throws TransferGroupNotFoundException, DeviceNotFoundException, ConnectionNotFoundException, AssigneeNotFoundException
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
        boolean broadcastStatus = mSeamlessMode != seamlessMode;
        mSeamlessMode = seamlessMode;
        mPinAccess = getDefaultPreferences().getInt(Keyword.NETWORK_PIN, -1) != -1;

        if (broadcastStatus)
            sendTrustZoneStatus();

        startForeground(CommunicationNotificationHelper.SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID,
                getNotificationHelper().getCommunicationServiceNotification(mSeamlessMode, mPinAccess,
                        mWebShareServer != null && mWebShareServer.isAlive()).build());
    }

    public void setWebShareEnabled(boolean enable, boolean updateServiceState)
    {
        boolean canStart = !mWebShareServer.isAlive();

        if (!enable && !canStart)
            mWebShareServer.stop();
        else if (enable && canStart)
            try {
                mWebShareServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            } catch (IOException e) {
                e.printStackTrace();
            }

        if (updateServiceState)
            updateServiceState(mSeamlessMode);
        sendWebShareStatus();
    }

    public void toggleWebShare()
    {
        setWebShareEnabled(!mWebShareServer.isAlive(), true);
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

            // the problem with the programming is that nobody seems to care what they do even though
            // what they do always concern others. This is the new world order and only thing you can
            // do about is to what until 

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

                final int networkPin = getDefaultPreferences().getInt(Keyword.NETWORK_PIN, -1);
                final boolean isSecureConnection = networkPin != -1
                        && responseJSON.has(Keyword.DEVICE_SECURE_KEY)
                        && responseJSON.getInt(Keyword.DEVICE_SECURE_KEY) == networkPin;

                String deviceSerial = null;

                AppUtils.applyDeviceToJSON(CommunicationService.this, replyJSON);

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

                if (deviceSerial != null) {
                    NetworkDevice device = new NetworkDevice(deviceSerial);

                    try {
                        getDatabase().reconstruct(device);

                        if (isSecureConnection)
                            device.isRestricted = false;

                        if (!device.isRestricted)
                            shouldContinue = true;
                    } catch (Exception e1) {
                        e1.printStackTrace();

                        device = NetworkDeviceLoader.load(true, getDatabase(), activeConnection.getClientAddress(), null);

                        if (device == null)
                            throw new Exception("Could not reach to the opposite server");

                        device.isTrusted = false;

                        if (isSecureConnection)
                            device.isRestricted = false;

                        getDatabase().publish(device);

                        shouldContinue = true;

                        if (device.isRestricted)
                            getNotificationHelper().notifyConnectionRequest(device);
                    }

                    final NetworkDevice.Connection connection = NetworkDeviceLoader.processConnection(getDatabase(), device, activeConnection.getClientAddress());
                    final NetworkDevice finalDevice = device;
                    final boolean isSeamlessAvailable = (mSeamlessMode && device.isTrusted)
                            || (isSecureConnection && getDefaultPreferences().getBoolean("qr_trust", false));

                    if (!shouldContinue)
                        replyJSON.put(Keyword.ERROR, Keyword.ERROR_NOT_ALLOWED);
                    else if (responseJSON.has(Keyword.REQUEST)) {
                        if (isSecureConnection && !mPinAccess)
                            // Probably pin access has just activated, so we should update the service state
                            refreshServiceState();

                        switch (responseJSON.getString(Keyword.REQUEST)) {
                            case (Keyword.REQUEST_TRANSFER):
                                if (responseJSON.has(Keyword.FILES_INDEX) && responseJSON.has(Keyword.TRANSFER_GROUP_ID) && getOngoingIndexList().size() < 1) {
                                    final String jsonIndex = responseJSON.getString(Keyword.FILES_INDEX);
                                    final long groupId = responseJSON.getLong(Keyword.TRANSFER_GROUP_ID);

                                    result = true;

                                    getSelfExecutor().submit(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            final JSONArray jsonArray;
                                            final Interrupter interrupter = new Interrupter();
                                            TransferGroup group = new TransferGroup(groupId);
                                            TransferGroup.Assignee assignee = new TransferGroup.Assignee(group, finalDevice, connection);
                                            final DynamicNotification notification = getNotificationHelper().notifyPrepareFiles(group, finalDevice);

                                            notification.setProgress(0, 0, true);

                                            try {
                                                jsonArray = new JSONArray(jsonIndex);
                                            } catch (Exception e) {
                                                notification.cancel();
                                                e.printStackTrace();
                                                return;
                                            }

                                            notification.setProgress(0, 0, false);
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

                                            long uniqueId = System.currentTimeMillis(); // The uniqueIds
                                            List<TransferObject> pendingRegistry = new ArrayList<>();

                                            for (int i = 0; i < jsonArray.length(); i++) {
                                                if (interrupter.interrupted())
                                                    break;

                                                try {
                                                    if (!(jsonArray.get(i) instanceof JSONObject))
                                                        continue;

                                                    JSONObject requestIndex = jsonArray.getJSONObject(i);

                                                    if (requestIndex != null && requestIndex.has(Keyword.INDEX_FILE_NAME) && requestIndex.has(Keyword.INDEX_FILE_SIZE) && requestIndex.has(Keyword.INDEX_FILE_MIME) && requestIndex.has(Keyword.TRANSFER_REQUEST_ID)) {
                                                        transferObject = new TransferObject(
                                                                requestIndex.getLong(Keyword.TRANSFER_REQUEST_ID),
                                                                groupId,
                                                                assignee.deviceId,
                                                                requestIndex.getString(Keyword.INDEX_FILE_NAME),
                                                                "." + (uniqueId++) + "." + AppConfig.EXT_FILE_PART,
                                                                requestIndex.getString(Keyword.INDEX_FILE_MIME),
                                                                requestIndex.getLong(Keyword.INDEX_FILE_SIZE),
                                                                TransferObject.Type.INCOMING);

                                                        if (requestIndex.has(Keyword.INDEX_DIRECTORY))
                                                            transferObject.directory = requestIndex.getString(Keyword.INDEX_DIRECTORY);

                                                        pendingRegistry.add(transferObject);
                                                    }
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }

                                            SQLiteDatabase.ProgressUpdater progressUpdater = new SQLiteDatabase.ProgressUpdater()
                                            {
                                                long lastNotified = System.currentTimeMillis();

                                                @Override
                                                public void onProgressChange(int total, int current)
                                                {
                                                    if ((System.currentTimeMillis() - lastNotified) > 1000) {
                                                        lastNotified = System.currentTimeMillis();
                                                        notification.updateProgress(total, current, false);
                                                    }
                                                }

                                                @Override
                                                public boolean onProgressState()
                                                {
                                                    return !interrupter.interrupted();
                                                }
                                            };

                                            if (pendingRegistry.size() > 0) {
                                                if (usePublishing)
                                                    getDatabase().publish(pendingRegistry, progressUpdater);
                                                else
                                                    getDatabase().insert(pendingRegistry, progressUpdater);
                                            }

                                            notification.cancel();

                                            synchronized (getOngoingIndexList()) {
                                                getOngoingIndexList().remove(group.groupId);
                                            }

                                            if (interrupter.interrupted())
                                                getDatabase().remove(group);
                                            else if (transferObject != null && pendingRegistry.size() > 0) {
                                                sendBroadcast(new Intent(ACTION_INCOMING_TRANSFER_READY)
                                                        .putExtra(EXTRA_GROUP_ID, groupId)
                                                        .putExtra(EXTRA_DEVICE_ID, finalDevice.deviceId));

                                                if (isSeamlessAvailable)
                                                    try {
                                                        startFileReceiving(group.groupId, finalDevice.deviceId);
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                else
                                                    getNotificationHelper().notifyTransferRequest(transferObject, finalDevice, pendingRegistry.size());
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
                                        getDatabase().reconstruct(group);
                                        getDatabase().reconstruct(assignee);

                                        if (!isAccepted)
                                            getDatabase().remove(assignee);

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
                            case (Keyword.REQUEST_START_TRANSFER):
                                if (!device.isTrusted)
                                    replyJSON.put(Keyword.ERROR, Keyword.ERROR_REQUIRE_TRUSTZONE);
                                else if (responseJSON.has(Keyword.TRANSFER_GROUP_ID)) {
                                    int groupId = responseJSON.getInt(Keyword.TRANSFER_GROUP_ID);

                                    try {
                                        TransferGroup group = new TransferGroup(groupId);
                                        getDatabase().reconstruct(group);

                                        ProcessHolder process = findProcessById(groupId, device.deviceId);

                                        if (process == null) {
                                            startFileReceiving(groupId, device.deviceId);
                                            result = true;
                                        } else
                                            responseJSON.put(Keyword.ERROR,
                                                    Keyword.ERROR_NOT_ACCESSIBLE);
                                    } catch (Exception e) {
                                        // do nothing
                                        responseJSON.put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND);
                                    }
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

            TransferInstance transferInstance = null;
            processHolder.activeConnection = activeConnection;
            processHolder.type = TransferObject.Type.OUTGOING;
            processHolder.builder = new CoolTransfer.Send.Builder<>();
            processHolder.builder.setExtra(processHolder);

            synchronized (getActiveProcessList()) {
                getActiveProcessList().add(processHolder);
            }

            try {
                ActiveConnection.Response mainRequest = activeConnection.receive();
                Log.d(TAG, "SeamlessServer.onConnected(): receive: " + mainRequest.response);
                JSONObject mainRequestJSON = new JSONObject(mainRequest.response);
                String deviceId = mainRequestJSON.has(Keyword.TRANSFER_DEVICE_ID)
                        ? mainRequestJSON.getString(Keyword.TRANSFER_DEVICE_ID)
                        : null;
                int groupId = mainRequestJSON.getInt(Keyword.TRANSFER_GROUP_ID);

                activeConnection.setId(groupId);

                {
                    JSONObject reply = new JSONObject()
                            .put(Keyword.RESULT, false);

                    try {
                        if (deviceId != null) {
                            NetworkDevice otherDevice = new NetworkDevice(deviceId);
                            getDatabase().reconstruct(otherDevice);

                            NetworkDevice.Connection connection = NetworkDeviceLoader.processConnection(getDatabase(), otherDevice, activeConnection.getClientAddress());

                            transferInstance = new TransferInstance.Builder()
                                    .supply(connection)
                                    .supply(otherDevice)
                                    .build(getDatabase(), groupId, deviceId, true);
                        } else
                            transferInstance = new TransferInstance(getDatabase(), groupId, activeConnection.getClientAddress(), false);

                        processHolder.groupId = transferInstance.getGroup().groupId;
                        processHolder.deviceId = transferInstance.getAssignee().deviceId;

                        reply.put(Keyword.RESULT, true);
                    } catch (TransferGroupNotFoundException e) {
                        reply.put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND);
                        e.printStackTrace();
                        return;
                    } catch (DeviceNotFoundException e) {
                        reply.put(Keyword.ERROR, Keyword.ERROR_NOT_ALLOWED);
                        e.printStackTrace();
                        return;
                    } finally {
                        activeConnection.reply(reply.toString());
                        Log.d(TAG, "SeamlessServer.onConnected(): reply: " + reply.toString());
                    }
                }

                {
                    // It is a good practice to update the transfer method
                    // when the connection address is not
                }

                notifyTaskStatusChange(processHolder.groupId, processHolder.deviceId, TASK_STATUS_ONGOING);
                notifyTaskRunningListChange();

                while (activeConnection.getSocket() != null
                        && activeConnection.getSocket().isConnected()) {
                    processHolder.builder.reset();

                    // This will set the previous

                    {
                        Send.Builder<ProcessHolder> sendBuilder = (Send.Builder<ProcessHolder>) processHolder.builder;
                        ActiveConnection.Response response = activeConnection.receive();
                        Log.d(TAG, "SeamlessServer.onConnected(): receive: " + response.response);

                        if (response.response == null || response.totalLength < 1) {
                            Log.d(TAG, "SeamlessServer.onConnected(): NULL response was received exiting loop");
                            return;
                        }

                        JSONObject request = new JSONObject(response.response);
                        JSONObject reply = new JSONObject();

                        try {
                            if (request.has(Keyword.RESULT) && !request.getBoolean(Keyword.RESULT)) {
                                // the assignee for this transfer has received the files. We can remove it
                                if (request.has(Keyword.TRANSFER_JOB_DONE) && request.getBoolean(Keyword.TRANSFER_JOB_DONE))
                                    Log.d(TAG, "SeamlessServer.onConnected(): Receiver notified us that it has received all the pending transfers: " + processHolder.deviceId);
                                else
                                    processHolder.builder.getTransferProgress().interrupt();

                                return;
                            } else if (!processHolder.builder.getTransferProgress().isInterrupted()) {
                                processHolder.transferObject = new TransferObject(
                                        request.getInt(Keyword.TRANSFER_REQUEST_ID),
                                        processHolder.deviceId,
                                        processHolder.type);

                                getDatabase().reconstruct(processHolder.transferObject);

                                processHolder.transferObject.accessPort = request.getInt(Keyword.TRANSFER_SOCKET_PORT);

                                if (request.has(Keyword.SKIPPED_BYTES)) {
                                    processHolder.transferObject.skippedBytes = request.getLong(Keyword.SKIPPED_BYTES);
                                    Log.d(TAG, "SeamlessServes.onConnected(): Has skipped bytes: " + processHolder.transferObject.skippedBytes);
                                }

                                // This changes the state of the object to pending from any other
                                getDatabase().update(processHolder.transferObject);
                                StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), Uri.parse(processHolder.transferObject.file));

                                reply.put(Keyword.RESULT, true);

                                if (streamInfo.size >= 0
                                        && streamInfo.size != processHolder.transferObject.fileSize) {
                                    reply.put(Keyword.SIZE_CHANGED, streamInfo.size);
                                    processHolder.transferObject.fileSize = streamInfo.size;
                                }

                                getNotificationHelper().notifyFileTransaction(processHolder);
                                Log.d(TAG, "SeamlessServer.onConnected(): Proceeding to send");

                                sendBuilder.setServerIp(activeConnection.getClientAddress())
                                        .setInputStream(streamInfo.openInputStream())
                                        .setPort(processHolder.transferObject.accessPort)
                                        .setFileSize(streamInfo.size)
                                        .setBuffer(new byte[AppConfig.BUFFER_LENGTH_DEFAULT])
                                        .setExtra(processHolder);
                            } else if (processHolder.builder.getTransferProgress().isInterrupted()) {
                                reply.put(Keyword.RESULT, false)
                                        .put(Keyword.TRANSFER_JOB_DONE, false);
                                return;
                            }
                        } catch (ReconstructionFailedException e) {
                            reply.put(Keyword.RESULT, false);
                            reply.put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND);
                            reply.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);

                            processHolder.transferObject.flag = TransferObject.Flag.REMOVED;
                        } catch (FileNotFoundException | StreamCorruptedException | StreamInfo.FolderStateException e) {
                            Log.d(TAG, "SeamlessServer.onConnected(): File is not accessible ? " + processHolder.transferObject.friendlyName);

                            reply.put(Keyword.RESULT, false);
                            reply.put(Keyword.ERROR, Keyword.ERROR_NOT_ACCESSIBLE);
                            reply.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);

                            processHolder.transferObject.flag = TransferObject.Flag.INTERRUPTED;

                            e.printStackTrace();
                        } catch (Exception e) {
                            Log.d(TAG, "SeamlessServer.onConnected(): Exception is handled: " + e.toString());

                            reply.put(Keyword.RESULT, false);
                            reply.put(Keyword.ERROR, Keyword.ERROR_UNKNOWN);
                            reply.put(Keyword.FLAG, Keyword.FLAG_GROUP_EXISTS);

                            processHolder.transferObject.flag = TransferObject.Flag.INTERRUPTED;

                            e.printStackTrace();
                        } finally {
                            if (reply.length() > 0) {
                                activeConnection.reply(reply.toString());
                                Log.d(TAG, "SeamlessServer.onConnected(): reply: " + reply.toString());
                            }
                        }

                        if (reply.has(Keyword.RESULT) && reply.getBoolean(Keyword.RESULT))
                            mSend.send(sendBuilder, true);
                    }

                    // We are now updating instances always at the end because it will be
                    // changed by the process itself naturally.
                    if (processHolder.transferObject != null) {
                        Log.d(TAG, "SeamlessServer.onConnected(): Updating file instances to " + processHolder.transferObject.flag.toString());
                        getDatabase().update(processHolder.transferObject);
                    }

                    // By rejecting to provide information when there is something wrong other
                    // than on
                    // e of the sides requested to interrupt receiver will try to attempt to
                    // restart the process. This is why interruption is checked. One more loop
                    // will allow us to gather information because the interruption may be requested
                    // while the transfer is going on meaning there was not a chance to gather the proper
                    // information.
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (!activeConnection.getSocket().isClosed())
                        activeConnection.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (transferInstance != null
                        && !CoolTransfer.Flag.CONTINUE.equals(processHolder.builder.getFlag())
                        && !processHolder.builder.getTransferProgress().isInterrupted())
                    mNotificationHelper.notifyConnectionError(transferInstance, TransferObject.Type.OUTGOING, null);
                else if (processHolder.notification != null)
                    processHolder.notification.cancel();

                synchronized (getActiveProcessList()) {
                    getActiveProcessList().remove(processHolder);

                    if (processHolder.groupId != 0 && processHolder.deviceId != null)
                        notifyTaskStatusChange(processHolder.groupId, processHolder.deviceId, TASK_STATUS_STOPPED);

                    notifyTaskRunningListChange();
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
            NetworkDevice thisDevice = AppUtils.getLocalDevice(CommunicationService.this);
            boolean retry = false;

            ProcessHolder processHolder = new ProcessHolder();

            processHolder.type = TransferObject.Type.INCOMING;
            processHolder.groupId = mTransfer.getGroup().groupId;
            processHolder.deviceId = mTransfer.getAssignee().deviceId;
            processHolder.activeConnection = new CoolSocket.ActiveConnection(AppConfig.DEFAULT_SOCKET_TIMEOUT);
            processHolder.builder = new CoolTransfer.Receive.Builder<>();

            processHolder.builder.setExtra(processHolder);

            synchronized (getActiveProcessList()) {
                getActiveProcessList().add(processHolder);
            }

            notifyTaskStatusChange(processHolder.groupId, processHolder.deviceId, TASK_STATUS_ONGOING);
            notifyTaskRunningListChange();

            try {
                try {
                    // this will first connect to CommunicationService to make sure the connection
                    // is okay
                    CommunicationBridge.Client handshakeClient
                            = new CommunicationBridge.Client(getDatabase());
                    handshakeClient.setDevice(mTransfer.getDevice());
                    CoolSocket.ActiveConnection initialConnection = handshakeClient
                            .communicate(mTransfer.getDevice(), mTransfer.getConnection());

                    initialConnection.reply(new JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_HANDSHAKE).toString());
                    Log.d(TAG, "SeamlessClientHandler.onConnect(): reply: empty");

                    JSONObject resultObject = new JSONObject(initialConnection.receive().response);
                    Log.d(TAG, "SeamlessClientHandler.onConnect(): Initial connection response: " + resultObject.toString());

                    if (!resultObject.getBoolean(Keyword.RESULT))
                        throw new Exception("Server rejected the request");
                } catch (Exception e) {
                    getNotificationHelper().notifyConnectionError(mTransfer, TransferObject.Type.INCOMING, null);
                    return;
                }

                try {
                    processHolder.activeConnection
                            .connect(new InetSocketAddress(mTransfer.getConnection().ipAddress, AppConfig.SERVER_PORT_SEAMLESS));
                } catch (Exception e) {
                    getNotificationHelper().notifyConnectionError(mTransfer, TransferObject.Type.INCOMING, null);
                    return;
                }

                try {
                    processHolder.activeConnection.reply(new JSONObject()
                            .put(Keyword.TRANSFER_GROUP_ID, processHolder.groupId)
                            .put(Keyword.TRANSFER_DEVICE_ID, thisDevice.deviceId)
                            .toString());
                    Log.d(TAG, "SeamlessClientHandler.onConnect(): reply: empty");

                    CoolSocket.ActiveConnection.Response response = processHolder.activeConnection.receive();
                    JSONObject request = new JSONObject(response.response);
                    Log.d(TAG, "SeamlessClientHandler.onConnect(): receive: " + response.response);

                    if (!request.getBoolean(Keyword.RESULT)) {
                        Log.d(TAG, "SeamlessClientHandler.onConnect(): false result, it will exit.");

                        String errorCode = request.has(Keyword.ERROR)
                                ? request.getString(Keyword.ERROR)
                                : null;

                        if (Keyword.ERROR_NOT_FOUND.equals(errorCode)) {
                            ContentValues contentValues = new ContentValues();
                            contentValues.put(AccessDatabase.FIELD_TRANSFER_FLAG, TransferObject.Flag.REMOVED.toString());

                            getDatabase().update(TransferUtils.createTransferSelection(
                                    processHolder.groupId,
                                    processHolder.deviceId,
                                    TransferObject.Flag.DONE,
                                    false), contentValues);
                        }

                        getNotificationHelper().notifyConnectionError(mTransfer, TransferObject.Type.INCOMING, errorCode);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                {
                    while (processHolder.activeConnection.getSocket() != null
                            && processHolder.activeConnection.getSocket().isConnected()) {
                        processHolder.builder.reset();

                        // Exiting directly will not be a problem since, after this phase, there will
                        // be a response how things went.
                        if (processHolder.builder.getTransferProgress().isInterrupted())
                            break;

                        try {
                            TransferObject firstAvailableTransfer = TransferUtils
                                    .fetchValidTransfer(CommunicationService.this,
                                            processHolder.groupId,
                                            processHolder.deviceId,
                                            processHolder.type);

                            if (firstAvailableTransfer == null) {
                                Log.d(TAG, "SeamlessClientHandler(): Exiting because there is no pending file instance left");
                                break;
                            }

                            processHolder.transferObject = firstAvailableTransfer;
                            processHolder.currentFile = FileUtils.getIncomingTransactionFile(getApplicationContext(), processHolder.transferObject, mTransfer.getGroup());
                            StreamInfo streamInfo = StreamInfo.getStreamInfo(getApplicationContext(), processHolder.currentFile.getUri());

                            getNotificationHelper().notifyFileTransaction(processHolder);

                            {
                                Receive.Builder<ProcessHolder> receiveBuilder = (Receive.Builder<ProcessHolder>) processHolder.builder;

                                receiveBuilder.setOutputStream(streamInfo.openOutputStream())
                                        .setServerSocket(new ServerSocket(0))
                                        .setTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT)
                                        .setBuffer(new byte[AppConfig.BUFFER_LENGTH_DEFAULT])
                                        .setFileSize(processHolder.transferObject.fileSize)
                                        .setExtra(processHolder);

                                Receive.Handler handler = mReceive.prepare(receiveBuilder);
                                long currentSize = processHolder.currentFile.length();
                                processHolder.transferObject.skippedBytes = currentSize;

                                {
                                    JSONObject jsonObject = new JSONObject();

                                    jsonObject.put(Keyword.TRANSFER_REQUEST_ID, processHolder.transferObject.requestId);
                                    jsonObject.put(Keyword.TRANSFER_GROUP_ID, processHolder.transferObject.groupId);
                                    jsonObject.put(Keyword.TRANSFER_SOCKET_PORT, receiveBuilder.getServerSocket().getLocalPort());
                                    jsonObject.put(Keyword.RESULT, true);

                                    if (currentSize > 0)
                                        jsonObject.put(Keyword.SKIPPED_BYTES, currentSize);

                                    handler.getExtra().activeConnection.reply(jsonObject.toString());
                                    Log.d(TAG, "Receive.onTaskPrepareSocket(): reply: " + jsonObject.toString());
                                }

                                {
                                    JSONObject response = new JSONObject(handler.getExtra().activeConnection.receive().response);
                                    Log.d(TAG, "Receive.onTaskPrepareSocket(): receive: " + response.toString());

                                    if (!response.getBoolean(Keyword.RESULT)) {
                                        if (response.has(Keyword.TRANSFER_JOB_DONE)
                                                && !response.getBoolean(Keyword.TRANSFER_JOB_DONE)) {
                                            handler.getTransferProgress().interrupt();
                                            Log.d(TAG, "Receive.onTaskPrepareSocket(): Transfer should be closed, babe!");
                                            break;
                                        } else if (response.has(Keyword.FLAG) && Keyword.FLAG_GROUP_EXISTS.equals(response.getString(Keyword.FLAG))) {
                                            if (response.has(Keyword.ERROR) && response.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_FOUND)) {
                                                handler.getExtra().transferObject.flag = TransferObject.Flag.REMOVED;
                                                Log.d(TAG, "Receive.onTaskPrepareSocket(): Sender says it does not have the file defined");
                                            } else if (response.has(Keyword.ERROR) && response.getString(Keyword.ERROR).equals(Keyword.ERROR_NOT_ACCESSIBLE)) {
                                                handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
                                                Log.d(TAG, "Receive.onTaskPrepareSocket(): Sender says it can't open the file");
                                            } else if (response.has(Keyword.ERROR) && response.getString(Keyword.ERROR).equals(Keyword.ERROR_UNKNOWN)) {
                                                handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
                                                Log.d(TAG, "Receive.onTaskPrepareSocket(): Sender says an unknown error occurred");
                                            }
                                        }
                                    } else {
                                        long sizeChanged = response.has(Keyword.SIZE_CHANGED)
                                                ? response.getLong(Keyword.SIZE_CHANGED)
                                                : -1;

                                        boolean sizeActuallyChanged = sizeChanged > -1 &&
                                                handler.getExtra().transferObject.fileSize != sizeChanged;

                                        boolean canContinue = !sizeActuallyChanged || currentSize < 1;

                                        if (sizeActuallyChanged) {
                                            Log.d(TAG, "Receive.onTaskPrepareSocket(): Sender says the file has a new size");
                                            handler.getExtra().transferObject.fileSize = response.getLong(Keyword.SIZE_CHANGED);
                                        }

                                        if (!canContinue) {
                                            Log.d(TAG, "Receive.onTaskPrepareSocket(): The change may broke the previous file which has a length. Cannot take the risk.");
                                            handler.getExtra().transferObject.flag = TransferObject.Flag.REMOVED;
                                        } else
                                            mReceive.receive(handler, true);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            retry = true;

                            if (!processHolder.recoverInterruptions) {
                                TransferUtils.recoverIncomingInterruptions(CommunicationService.this, processHolder.groupId);
                                processHolder.recoverInterruptions = true;
                            }

                            break;
                        } finally {
                            if (processHolder.transferObject != null) {
                                // We are now updating instances always at the end because it will be
                                // changed by the process itself naturally
                                Log.d(TAG, "SeamlessClientHandler.onConnect(): Updating file instances to " + processHolder.transferObject.flag.toString());
                                getDatabase().update(processHolder.transferObject);
                            }
                        }
                    }
                }

                // Check if all the pending files are flagged with Flag.DONE
                try {
                    DocumentFile savePath = FileUtils.getSavePath(getApplicationContext(), mTransfer.getGroup());
                    boolean isJobDone = CoolTransfer.Flag.CONTINUE.equals(processHolder.builder.getFlag());
                    boolean hasLeftFiles = getDatabase().getFirstFromTable(TransferUtils.createTransferSelection(
                            processHolder.groupId,
                            processHolder.deviceId,
                            TransferObject.Flag.DONE,
                            false)) == null;

                    processHolder.activeConnection.reply(new JSONObject()
                            .put(Keyword.RESULT, false)
                            .put(Keyword.TRANSFER_JOB_DONE, isJobDone && hasLeftFiles)
                            .toString());
                    Log.d(TAG, "SeamlessClientHandler.onConnect(): reply: done ?? " + (isJobDone && hasLeftFiles));

                    if (!retry)
                        if (!processHolder.builder.getTransferProgress().isInterrupted()) {
                            // If retry requested, don't show a notification because this method will loop
                            if (isJobDone) {
                                getNotificationHelper().notifyFileReceived(processHolder, mTransfer.getDevice(), savePath);
                                Log.d(TAG, "SeamlessClientHandler.onConnect(): Notify user");
                            } else {
                                getNotificationHelper().notifyReceiveError(processHolder);
                                Log.d(TAG, "SeamlessClientHandler.onConnect(): Some files was not received");
                            }
                        } else {
                            // If there was an error it should be handled by showing another error notification
                            // most of which are seemingly potential headache in the future
                            processHolder.notification.cancel();
                            Log.d(TAG, "SeamlessClientHandler.onConnect(): Removing notification an error is already notified");
                        }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (processHolder.activeConnection != null && !processHolder.activeConnection.getSocket().isClosed())
                        processHolder.activeConnection.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                synchronized (getActiveProcessList()) {
                    getActiveProcessList().remove(processHolder);
                }

                notifyTaskStatusChange(mTransfer.getGroup().groupId, mTransfer.getAssignee().deviceId, TASK_STATUS_STOPPED);
                notifyTaskRunningListChange();

                Log.d(TAG, "We have exited");

                if (retry && processHolder.attemptsLeft > 0
                        && !processHolder.builder.getTransferProgress().isInterrupted()) {
                    try {
                        startFileReceiving(processHolder.groupId, processHolder.deviceId);
                        processHolder.attemptsLeft--;
                    } catch (Exception e) {
                        Log.d(TAG, "SeamlessClientHandler.onConnect(): Restart is requested, but transfer instance failed to reconstruct");
                    }
                }
            }
        }
    }

    public class Receive extends CoolTransfer.Receive<ProcessHolder>
    {
        @Override
        public Flag onError(TransferHandler<ProcessHolder> handler, Exception error)
        {
            if (error != null)
                error.printStackTrace();

            handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
            getNotificationHelper().notifyReceiveError(handler.getExtra().transferObject);

            return Flag.CANCEL_ALL;
        }

        @Override
        public void onDestroy(TransferHandler<ProcessHolder> handler)
        {
            if (handler.getTransferProgress().isInterrupted()
                    && TransferObject.Flag.IN_PROGRESS.equals(handler.getExtra().transferObject.flag))
                handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
        }

        @Override
        public void onNotify(TransferHandler<ProcessHolder> handler, int percentage)
        {
            // Some bytes have been received, meaning we can handle another file recovery (useful for big files)
            handler.getExtra().recoverInterruptions = false;

            handler.getExtra().notification.setContentText(getString(R.string.text_remainingTime, TimeUtils.getDuration(handler.getTransferProgress().getTimeRemaining())));
            handler.getExtra().notification.updateProgress(100, percentage, false);

            handler.getExtra().transferObject.flag = TransferObject.Flag.IN_PROGRESS;
            handler.getExtra().transferObject.flag.setBytesValue(handler.getTransferProgress().getCurrentTransferredByte());

            getDatabase().update(handler.getExtra().transferObject);

            // We have transferred bytes now, so reset the counter; cuz it works
            handler.getExtra().attemptsLeft = 2;
        }

        @Override
        public void onTaskEnd(TransferHandler<ProcessHolder> handler)
        {
            try {
                handler.getExtra().currentFile.sync();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (handler.getFileSize() == handler.getExtra().currentFile.length()) {
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
            } else {
                handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
                handler.setFlag(Flag.CANCEL_CURRENT);
            }
        }

        @Override
        public Flag onTaskPrepareSocket(TransferHandler<ProcessHolder> handler)
        {
            return Flag.CONTINUE;
        }

        @Override
        public Flag onTaskPrepareSocket(final TransferHandler<ProcessHolder> handler, final ServerSocket serverSocket)
        {
            return Flag.CONTINUE;
        }

        @Override
        public Flag onPrepare(TransferHandler<ProcessHolder> handler)
        {
            if (handler.getTransferProgress().getTotalByte() == 0) {
                TransferGroup.Index indexInstance = new TransferGroup.Index();

                getDatabase().calculateTransactionSize(handler.getExtra().transferObject.groupId, indexInstance);

                handler.getTransferProgress().setTotalByte(indexInstance.incoming - indexInstance.incomingCompleted);
            }

            return Flag.CONTINUE;
        }

        @Override
        public Flag onTaskOrientateStreams(TransferHandler<ProcessHolder> handler, InputStream inputStream, OutputStream outputStream)
        {
            if (handler.getExtra().transferObject.skippedBytes > 0)
                try {
                    handler.skipBytes(handler.getExtra().transferObject.skippedBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    return Flag.CONTINUE;
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
    }

    public class Send extends CoolTransfer.Send<ProcessHolder>
    {
        @Override
        public Flag onError(TransferHandler<ProcessHolder> handler, Exception error)
        {
            if (error != null)
                error.printStackTrace();

            handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;

            return Flag.CANCEL_ALL;
        }

        @Override
        public void onNotify(TransferHandler<ProcessHolder> handler, int percentage)
        {
            handler.getExtra().notification.setContentText(getString(R.string.text_remainingTime, TimeUtils.getDuration(handler.getTransferProgress().getTimeRemaining())));
            handler.getExtra().notification.updateProgress(100, percentage, false);

            handler.getExtra().transferObject.flag = TransferObject.Flag.IN_PROGRESS;
            handler.getExtra().transferObject.flag.setBytesValue(handler.getTransferProgress().getCurrentTransferredByte());

            getDatabase().update(handler.getExtra().transferObject);
        }

        @Override
        public void onTaskEnd(TransferHandler<ProcessHolder> handler)
        {
            handler.getExtra().transferObject.flag = handler.getTransferProgress().getCurrentTransferredByte() == handler.getFileSize()
                    ? TransferObject.Flag.DONE
                    : TransferObject.Flag.INTERRUPTED;
        }

        @Override
        public void onDestroy(TransferHandler<ProcessHolder> handler)
        {
            if (handler.getTransferProgress().isInterrupted()
                    && TransferObject.Flag.IN_PROGRESS.equals(handler.getExtra().transferObject.flag))
                handler.getExtra().transferObject.flag = TransferObject.Flag.INTERRUPTED;
        }

        @Override
        public Flag onTaskPrepareSocket(TransferHandler<ProcessHolder> handler)
        {
            return Flag.CONTINUE;
        }

        @Override
        public Flag onPrepare(TransferHandler<ProcessHolder> handler)
        {
            if (handler.getTransferProgress().getTotalByte() == 0) {
                TransferGroup.Index indexInstance = new TransferGroup.Index();

                getDatabase().calculateTransactionSize(handler.getExtra().transferObject.groupId, indexInstance);
                handler.getTransferProgress().setTotalByte(indexInstance.outgoing - indexInstance.outgoingCompleted);
            }

            return Flag.CONTINUE;
        }


        @Override
        public Flag onTaskOrientateStreams(TransferHandler<ProcessHolder> handler, InputStream inputStream, OutputStream outputStream)
        {
            super.onTaskOrientateStreams(handler, inputStream, outputStream);

            if (handler.getExtra().transferObject.skippedBytes > 0)
                try {
                    handler.skipBytes(handler.getExtra().transferObject.skippedBytes);
                } catch (IOException e) {
                    e.printStackTrace();
                    return Flag.CONTINUE;
                }

            return Flag.CONTINUE;
        }
    }

    public class ProcessHolder
    {
        public CoolTransfer.ParentBuilder<ProcessHolder> builder;
        public CoolSocket.ActiveConnection activeConnection;
        public DynamicNotification notification;
        public TransferObject transferObject;
        public DocumentFile currentFile;
        public TransferObject.Type type;
        public String deviceId;
        public boolean recoverInterruptions = false;
        public long groupId;
        public int attemptsLeft = 2;
    }
}
