/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.service;

import android.app.PendingIntent;
import android.content.*;
import android.media.MediaScannerConnection;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Service;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.TrebleShot.task.IndexTransferTask;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import com.genonbeta.android.framework.util.Stoppable;
import com.genonbeta.android.framework.util.StoppableImpl;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

public class BackgroundService extends Service
{
    public static final String TAG = "CommunicationService";

    public static final String
            ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.action.CLIPBOARD",
            ACTION_DEVICE_ACQUAINTANCE = "com.genonbeta.TrebleShot.transaction.action.DEVICE_ACQUAINTANCE",
            ACTION_DEVICE_APPROVAL = "com.genonbeta.TrebleShot.action.DEVICE_APPROVAL",
            ACTION_END_SESSION = "com.genonbeta.TrebleShot.action.END_SESSION",
            ACTION_FILE_TRANSFER = "com.genonbeta.TrebleShot.action.FILE_TRANSFER",
            ACTION_INCOMING_TRANSFER_READY = "com.genonbeta.TrebleShot.transaction.action.INCOMING_TRANSFER_READY",
            ACTION_KILL_SIGNAL = "com.genonbeta.intent.action.KILL_SIGNAL",
            ACTION_PIN_USED = "com.genonbeta.TrebleShot.transaction.action.PIN_USED",
            ACTION_START_TRANSFER = "com.genonbeta.intent.action.START_TRANSFER",
            ACTION_STOP_TASK = "com.genonbeta.TrebleShot.transaction.action.CANCEL_JOB",
            ACTION_TASK_CHANGE = "com.genonbeta.TrebleShot.transaction.action.TASK_STATUS_CHANGE",
            EXTRA_DEVICE_LIST = "extraDeviceListRunning",
            EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted",
            EXTRA_CLIPBOARD_ID = "extraTextId",
            EXTRA_CONNECTION_ADAPTER_NAME = "extraConnectionAdapterName",
            EXTRA_DEVICE_ID = "extraDeviceId",
            EXTRA_DEVICE_PIN = "extraDevicePin",
            EXTRA_GROUP_ID = "extraGroupId",
            EXTRA_IDENTITY = "extraIdentity",
            EXTRA_IS_ACCEPTED = "extraAccepted",
            EXTRA_REQUEST_ID = "extraRequestId",
            EXTRA_TRANSFER_TYPE = "extraTransferType";

    public static final int
            TASK_STATUS_ONGOING = 0,
            TASK_STATUS_STOPPED = 1,
            ID_NOTIFICATION_FOREGROUND = 1103;

    private final List<RunningTask> mTaskList = new ArrayList<>();
    private CommunicationServer mCommunicationServer = new CommunicationServer();
    private WebShareServer mWebShareServer;
    private ExecutorService mExecutor = Executors.newFixedThreadPool(10);
    private NsdDiscovery mNsdDiscovery;
    private NotificationHelper mNotificationHelper;
    private WifiManager.WifiLock mWifiLock;
    private MediaScannerConnection mMediaScanner;
    private HotspotUtils mHotspotUtils;
    private LocalBinder mBinder = new LocalBinder();
    private DynamicNotification mNotification;

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        WifiManager wifiManager = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE));

        mNotificationHelper = new NotificationHelper(getNotificationUtils());
        mNsdDiscovery = new NsdDiscovery(getApplicationContext(), getKuick(), getDefaultPreferences());
        mMediaScanner = new MediaScannerConnection(this, null);
        mHotspotUtils = HotspotUtils.getInstance(this);

        if (wifiManager != null)
            mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);

        mMediaScanner.connect();
        mNsdDiscovery.registerService();

        if (mWifiLock != null)
            mWifiLock.acquire();

        refreshServiceState();
        tryStartingServices();
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
                    final NetworkDevice device = new NetworkDevice(deviceId);
                    getKuick().reconstruct(device);

                    TransferGroup group = new TransferGroup(groupId);
                    getKuick().reconstruct(group);

                    TransferAssignee assignee = new TransferAssignee(groupId, deviceId, TransferObject.Type.INCOMING);
                    getKuick().reconstruct(assignee);

                    final DeviceConnection connection = new DeviceConnection(assignee);
                    getKuick().reconstruct(connection);

                    CommunicationBridge.connect(getKuick(), client -> {
                        try {
                            CoolSocket.ActiveConnection activeConnection = client.communicate(device, connection);

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
                    });

                    if (isAccepted)
                        FileTransferTask.startTransferAsClient(this, groupId, deviceId,
                                TransferObject.Type.INCOMING);
                    else {
                        getKuick().remove(group);
                        getKuick().broadcast();
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    if (isAccepted)
                        getNotificationHelper().showToast(R.string.mesg_somethingWentWrong);
                }
            } else if (ACTION_DEVICE_APPROVAL.equals(intent.getAction())) {
                String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
                boolean isAccepted = intent.getBooleanExtra(EXTRA_IS_ACCEPTED, false);
                int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
                int suggestedPin = intent.getIntExtra(EXTRA_DEVICE_PIN, -1);

                getNotificationHelper().getUtils().cancel(notificationId);

                NetworkDevice device = new NetworkDevice(deviceId);

                try {
                    getKuick().reconstruct(device);
                    device.isRestricted = !isAccepted;

                    if (isAccepted)
                        device.secureKey = suggestedPin;

                    getKuick().update(device);
                    getKuick().broadcast();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (ACTION_CLIPBOARD.equals(intent.getAction()) && intent.hasExtra(EXTRA_CLIPBOARD_ACCEPTED)) {
                int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
                long clipboardId = intent.getLongExtra(EXTRA_CLIPBOARD_ID, -1);
                boolean isAccepted = intent.getBooleanExtra(EXTRA_CLIPBOARD_ACCEPTED, false);

                TextStreamObject textStreamObject = new TextStreamObject(clipboardId);

                getNotificationHelper().getUtils().cancel(notificationId);

                try {
                    getKuick().reconstruct(textStreamObject);

                    if (isAccepted) {
                        ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                                ClipData.newPlainText("receivedText", textStreamObject.text));
                        Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (ACTION_END_SESSION.equals(intent.getAction())) {
                stopSelf();
            } else if (ACTION_START_TRANSFER.equals(intent.getAction()) && intent.hasExtra(EXTRA_GROUP_ID)
                    && intent.hasExtra(EXTRA_DEVICE_ID) && intent.hasExtra(EXTRA_TRANSFER_TYPE)) {
                long groupId = intent.getLongExtra(EXTRA_GROUP_ID, -1);
                String deviceId = intent.getStringExtra(EXTRA_DEVICE_ID);
                String typeString = intent.getStringExtra(EXTRA_TRANSFER_TYPE);

                try {
                    TransferObject.Type type = TransferObject.Type.valueOf(typeString);
                    FileTransferTask task = (FileTransferTask) findTaskBy(FileTransferTask.identityWith(groupId,
                            deviceId, type));

                    if (task == null)
                        FileTransferTask.startTransferAsClient(this, groupId, deviceId, type);
                    else
                        Toast.makeText(this, getString(R.string.mesg_groupOngoingNotice, task.object.name),
                                Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (ACTION_STOP_TASK.equals(intent.getAction()) && intent.hasExtra(EXTRA_IDENTITY)) {
                int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
                Identity identity = intent.getParcelableExtra(EXTRA_IDENTITY);

                try {
                    RunningTask task = findTaskBy(identity);

                    if (task == null) {
                        getNotificationHelper().getUtils().cancel(notificationId);
                    } else {
                        // FIXME: 16.03.2020 Should we use this notification?
                        //task.notification = getNotificationHelper().notifyStuckThread(task);

                        if (task.isInterrupted())
                            task.forceQuit();
                        else
                            task.interrupt(true);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
        mMediaScanner.disconnect();
        mNsdDiscovery.unregisterService();
        mWebShareServer.stop();

        {
            ContentValues values = new ContentValues();
            values.put(Kuick.FIELD_TRANSFERGROUP_ISSHAREDONWEB, 0);
            getKuick().update(new SQLQuery.Select(Kuick.TABLE_TRANSFERGROUP)
                    .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFERGROUP_ISSHAREDONWEB),
                            String.valueOf(1)), values);
        }

        if (getHotspotUtils().unloadPreviousConfig()) {
            getHotspotUtils().disable();
            Log.d(TAG, "onDestroy(): Stopping hotspot (previously started)");
        }

        if (getWifiLock() != null && getWifiLock().isHeld()) {
            getWifiLock().release();
            Log.d(TAG, "onDestroy(): Releasing Wi-Fi lock");
        }

        stopForeground(true);

        synchronized (mTaskList) {
            for (RunningTask task : mTaskList) {
                task.getStoppable().interrupt(false);
                Log.d(TAG, "onDestroy(): Ongoing indexing stopped: " + task.getTitle());
            }
        }

        AppUtils.generateNetworkPin(this);
        getKuick().broadcast();
    }

    public void attach(RunningTask task)
    {
        runInternal(task);
    }

    public boolean canStopService()
    {
        return mCommunicationServer.getConnections().size() > 0 || getTaskList().size() > 0
                || mHotspotUtils.isStarted() || mWebShareServer.hadClients();
    }

    public RunningTask findTaskBy(long hashCode)
    {
        return findTaskBy(Identity.withORs(hashCode));
    }

    public synchronized RunningTask findTaskBy(Identity identity)
    {
        synchronized (mTaskList) {
            for (RunningTask runningTask : getTaskList())
                if (runningTask.getIdentity().equals(identity))
                    return runningTask;
        }

        return null;
    }

    private HotspotUtils getHotspotUtils()
    {
        return mHotspotUtils;
    }

    public WifiConfiguration getHotspotConfig()
    {
        return getHotspotUtils().getConfiguration();
    }

    public MediaScannerConnection getMediaScanner()
    {
        return mMediaScanner;
    }

    public NotificationHelper getNotificationHelper()
    {
        return mNotificationHelper;
    }

    private ExecutorService getSelfExecutor()
    {
        return mExecutor;
    }

    public List<RunningTask> getTaskList()
    {
        return mTaskList;
    }

    public <T extends RunningTask> List<T> getTaskListOf(Class<T> clazz)
    {
        List<T> taskList = new ArrayList<>();
        synchronized (mTaskList) {
            for (RunningTask task : mTaskList)
                if (clazz.isInstance(task))
                    taskList.add((T) task);
        }
        return taskList;
    }

    private WifiManager.WifiLock getWifiLock()
    {
        return mWifiLock;
    }

    private boolean hasOngoingIndexing()
    {
        synchronized (mTaskList) {
            for (RunningTask task : mTaskList)
                if (task instanceof IndexTransferTask)
                    return true;
        }
        return false;
    }

    public static int hashIntent(@NonNull Intent intent)
    {
        StringBuilder builder = new StringBuilder()
                .append(intent.getComponent())
                .append(intent.getData())
                .append(intent.getPackage())
                .append(intent.getAction())
                .append(intent.getFlags())
                .append(intent.getType());

        if (intent.getExtras() != null)
            builder.append(intent.getExtras().toString());

        return builder.toString().hashCode();
    }

    private boolean isProcessRunning(long groupId, String deviceId, TransferObject.Type type)
    {
        return findTaskBy(FileTransferTask.identityWith(groupId, deviceId, type)) != null;
    }

    public void publishNotification(RunningTask runningTask)
    {
        if (runningTask.mNotification == null) {
            PendingIntent cancelIntent = PendingIntent.getService(this, AppUtils.getUniqueNumber(),
                    new Intent(this, BackgroundService.class)
                            .setAction(ACTION_KILL_SIGNAL)
                            .putExtra(EXTRA_IDENTITY, runningTask.getIdentity()), 0);

            runningTask.mNotification = getNotificationUtils().buildDynamicNotification(runningTask.hashCode(),
                    NotificationUtils.NOTIFICATION_CHANNEL_LOW);

            runningTask.mNotification.setSmallIcon(runningTask.getIconRes() == 0
                    ? R.drawable.ic_autorenew_white_24dp_static : runningTask.getIconRes())
                    .setContentTitle(getString(R.string.text_taskOngoing))
                    .addAction(R.drawable.ic_close_white_24dp_static,
                            getString(R.string.butn_cancel), cancelIntent);

            if (runningTask.mActivityIntent != null)
                runningTask.mNotification.setContentIntent(runningTask.mActivityIntent);
        }

        runningTask.mNotification.setContentTitle(runningTask.getTitle())
                .setContentText(runningTask.getStatusText());

        runningTask.mNotification.show();
    }

    public void publishForegroundNotification()
    {
        if (mNotification == null) {
            mNotification = getNotificationUtils().buildDynamicNotification(ID_NOTIFICATION_FOREGROUND,
                    NotificationUtils.NOTIFICATION_CHANNEL_LOW);
            mNotification.setSmallIcon(R.drawable.ic_autorenew_white_24dp_static)
                    .setContentTitle(getString(R.string.text_taskOngoing));
        }

        mNotification.setContentText(getString(R.string.text_workerService));
        startForeground(ID_NOTIFICATION_FOREGROUND, mNotification.build());
    }

    private void refreshServiceState()
    {
        startForeground(NotificationHelper.SERVICE_COMMUNICATION_FOREGROUND_NOTIFICATION_ID,
                getNotificationHelper().getCommunicationServiceNotification().build());
    }

    protected synchronized void registerWork(RunningTask runningTask)
    {
        synchronized (getTaskList()) {
            getTaskList().add(runningTask);
        }

        publishForegroundNotification();
        publishNotification(runningTask);
    }

    public void run(final RunningTask runningTask)
    {
        mExecutor.submit(() -> attach(runningTask));
    }

    private void runInternal(RunningTask runningTask)
    {
        runningTask.setService(BackgroundService.this);
        registerWork(runningTask);

        try {
            runningTask.run();
        } catch (Exception e) {
            e.printStackTrace();
        }

        unregisterWork(runningTask);
        runningTask.setService(null);
    }

    public void toggleHotspot()
    {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(this))
            return;

        if (getHotspotUtils().isEnabled())
            getHotspotUtils().disable();
        else
            getHotspotUtils().enableConfigured(AppUtils.getHotspotName(this), null);
    }

    /**
     * Some services like file transfer server, web share portal server involve writing and reading data.
     * So, it is best to avoid starting them when the app doesn't have the right permissions.
     */
    public boolean tryStartingServices()
    {
        if (mWebShareServer.isAlive() && mCommunicationServer.isServerAlive())
            return true;

        if (!AppUtils.checkRunningConditions(this)) {
            Log.d(TAG, "tryStartingServices: The app doesn't have the satisfactory permissions to start " +
                    "services.");
            return false;
        }

        if (!mCommunicationServer.isServerAlive() || !mCommunicationServer.start()) {
            Log.e(TAG, "tryStartingServices: Cannot start the service. server="
                    + mCommunicationServer.isServerAlive());
            return false;
        }

        try {
            mWebShareServer = new WebShareServer(this, AppConfig.SERVER_PORT_WEBSHARE);
            mWebShareServer.setAsyncRunner(new WebShareServer.BoundRunner(
                    Executors.newFixedThreadPool(AppConfig.WEB_SHARE_CONNECTION_MAX)));
            mWebShareServer.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        } catch (Throwable t) {
            Log.e(TAG, "Failed to start Web Share Server");
            return false;
        }

        return true;
    }

    protected synchronized void unregisterWork(RunningTask runningTask)
    {
        runningTask.mNotification.cancel();

        synchronized (getTaskList()) {
            getTaskList().remove(runningTask);

            if (getTaskList().size() <= 0)
                stopForeground(true);
        }
    }

    public class CommunicationServer extends CoolSocket
    {
        CommunicationServer()
        {
            super(AppConfig.SERVER_PORT_COMMUNICATION);
            setSocketTimeout(AppConfig.DEFAULT_SOCKET_TIMEOUT_LARGE);
        }

        @Override
        protected void onConnected(final ActiveConnection activeConnection)
        {
            // check if the same address has other connections and limit that to 5
            if (getConnectionCountByAddress(activeConnection.getAddress()) > 5)
                return;

            try {
                JSONObject responseJSON = analyzeResponse(activeConnection.receive());

                if (isUpdateRequest(activeConnection, responseJSON))
                    return;

                boolean result = false;
                boolean shouldContinue = false;
                boolean handshakeRequired = responseJSON.has(Keyword.HANDSHAKE_REQUIRED) && responseJSON.getBoolean(
                        Keyword.HANDSHAKE_REQUIRED);
                boolean handshakeOnly = responseJSON.has(Keyword.HANDSHAKE_ONLY)
                        && responseJSON.getBoolean(Keyword.HANDSHAKE_ONLY);
                final int activePin = getDefaultPreferences().getInt(Keyword.NETWORK_PIN, -1);
                final boolean hasPin = activePin != -1 && responseJSON.has(Keyword.DEVICE_PIN)
                        && activePin == responseJSON.getInt(Keyword.DEVICE_PIN);

                if (hasPin) // pin is known, should be changed. Warn the listeners.
                    sendBroadcast(new Intent(ACTION_PIN_USED));

                JSONObject replyJSON = new JSONObject();
                AppUtils.applyDeviceToJSON(BackgroundService.this, replyJSON);

                if (handshakeRequired) {
                    pushReply(activeConnection, replyJSON, true);

                    if (handshakeOnly)
                        return;
                }

                Log.d(TAG, "onConnected: hasPin: " + hasPin);
                NetworkDevice device;

                try {
                    device = NetworkDeviceLoader.loadFrom(getKuick(), responseJSON);
                } catch (JSONException e) {
                    // Deprecated: This is a fallback option to generate device information.
                    // Clients must send the device info with the requests asking no handshake or not only handshake.
                    device = new NetworkDevice(responseJSON.getString(Keyword.DEVICE_INFO_SERIAL));
                }

                if (device.clientVersion >= 1 && device.secureKey < 0) {
                    // Because the client didn't know whom it was talking to, it did not provide a key that might be
                    // exchanged between us before. Now we are asking for the key. Also, this does not work with
                    // the older client versions.
                    device.secureKey = new JSONObject(activeConnection.receive().response).getInt(
                            Keyword.DEVICE_INFO_KEY);
                    activeConnection.reply(Keyword.STUB);
                }

                try {
                    NetworkDevice existingInfo = new NetworkDevice(device.id);
                    getKuick().reconstruct(existingInfo);

                    device.applyPreferences(existingInfo); // apply known preferences

                    boolean keysMatch = existingInfo.secureKey == device.secureKey;
                    boolean needsBlocking = device.clientVersion >= 1 && !keysMatch && !hasPin;

                    // We don't update the device info. Instead, we request a check from the user.
                    // If she or he accepts the request, we update the old key with the new one.
                    if (!existingInfo.isRestricted && needsBlocking) {
                        Log.d(TAG, "onConnected: Notifying a PIN issue. Revoked the access for now.");
                        getNotificationHelper().notifyConnectionRequest(existingInfo, device.secureKey);

                        // Previously, the device had the access rights which should now be revoked, because
                        // the device does not have a matching key or valid PIN.
                        existingInfo.isRestricted = true;
                        getKuick().publish(existingInfo);
                    } else {
                        shouldContinue = true;

                        // The device does not have a matching key, but has a valid PIN. So we accept the new key it
                        // sent us and save it.
                        if (device.clientVersion >= 1 && !keysMatch && hasPin)
                            getKuick().publish(device);
                    }
                } catch (ReconstructionFailedException ignored) {
                    if (device.clientVersion < 1)
                        device = NetworkDeviceLoader.load(true, getKuick(),
                                activeConnection.getClientAddress(), null);

                    if (device == null || device.id == null || device.id.length() < 1)
                        throw new Exception("Device is not valid.");

                    device.isTrusted = hasPin;
                    device.isRestricted = !hasPin;

                    getKuick().publish(device);

                    shouldContinue = true; // For the first round, we let the client pass.

                    if (device.isRestricted)
                        getNotificationHelper().notifyConnectionRequest(device, device.secureKey);
                }

                if (handshakeRequired) {
                    responseJSON = analyzeResponse(activeConnection.receive());
                    replyJSON = new JSONObject();
                }

                DeviceConnection connection = NetworkDeviceLoader.processConnection(getKuick(), device,
                        activeConnection.getClientAddress());

                getKuick().broadcast();

                if (!shouldContinue || device.clientVersion < 1)
                    replyJSON.put(Keyword.ERROR, Keyword.ERROR_NOT_ALLOWED);
                else if (responseJSON.has(Keyword.REQUEST)) {
                    switch (responseJSON.getString(Keyword.REQUEST)) {
                        case (Keyword.REQUEST_TRANSFER):
                            if (responseJSON.has(Keyword.FILES_INDEX) && responseJSON.has(Keyword.TRANSFER_GROUP_ID)
                                    && !hasOngoingIndexing()) {
                                long groupId = responseJSON.getLong(Keyword.TRANSFER_GROUP_ID);
                                String jsonIndex = responseJSON.getString(Keyword.FILES_INDEX);
                                result = true;

                                run(new IndexTransferTask(groupId, jsonIndex, device, connection, hasPin));
                            }
                            break;
                        case (Keyword.REQUEST_RESPONSE):
                            if (responseJSON.has(Keyword.TRANSFER_GROUP_ID)) {
                                int groupId = responseJSON.getInt(Keyword.TRANSFER_GROUP_ID);
                                boolean isAccepted = responseJSON.getBoolean(Keyword.TRANSFER_IS_ACCEPTED);

                                TransferGroup group = new TransferGroup(groupId);
                                TransferAssignee assignee = new TransferAssignee(group, device,
                                        TransferObject.Type.OUTGOING);

                                try {
                                    getKuick().reconstruct(group);
                                    getKuick().reconstruct(assignee);

                                    if (!isAccepted) {
                                        getKuick().remove(assignee);
                                        getKuick().broadcast();
                                    }

                                    result = true;
                                } catch (Exception ignored) {
                                }
                            }
                            break;
                        case (Keyword.REQUEST_CLIPBOARD):
                            if (responseJSON.has(Keyword.TRANSFER_CLIPBOARD_TEXT)) {
                                TextStreamObject textStreamObject = new TextStreamObject(AppUtils.getUniqueNumber(),
                                        responseJSON.getString(Keyword.TRANSFER_CLIPBOARD_TEXT));

                                getKuick().publish(textStreamObject);
                                getKuick().broadcast();
                                getNotificationHelper().notifyClipboardRequest(device, textStreamObject);

                                result = true;
                            }
                            break;
                        case (Keyword.REQUEST_ACQUAINTANCE):
                            sendBroadcast(new Intent(ACTION_DEVICE_ACQUAINTANCE)
                                    .putExtra(EXTRA_DEVICE_ID, device.id)
                                    .putExtra(EXTRA_CONNECTION_ADAPTER_NAME, connection.adapterName));

                            result = true;
                            break;
                        case (Keyword.REQUEST_HANDSHAKE):
                            result = true;
                            break;
                        case (Keyword.REQUEST_TRANSFER_JOB):
                            if (responseJSON.has(Keyword.TRANSFER_GROUP_ID)) {
                                int groupId = responseJSON.getInt(Keyword.TRANSFER_GROUP_ID);
                                String typeValue = responseJSON.getString(Keyword.TRANSFER_TYPE);

                                try {
                                    TransferObject.Type type = TransferObject.Type.valueOf(typeValue);

                                    // The type is reversed to match our side
                                    if (TransferObject.Type.INCOMING.equals(type))
                                        type = TransferObject.Type.OUTGOING;
                                    else if (TransferObject.Type.OUTGOING.equals(type))
                                        type = TransferObject.Type.INCOMING;

                                    PreloadedGroup group = new PreloadedGroup(groupId);
                                    getKuick().reconstruct(group);

                                    Log.d(BackgroundService.TAG, "CommunicationServer.onConnected(): "
                                            + "groupId=" + groupId + " typeValue=" + typeValue);

                                    if (!isProcessRunning(groupId, device.id, type)) {
                                        FileTransferTask task = new FileTransferTask();
                                        task.activeConnection = activeConnection;
                                        task.group = group;
                                        task.device = device;
                                        task.type = type;
                                        task.assignee = new TransferAssignee(group, device, type);

                                        getKuick().reconstruct(task.assignee);

                                        if (TransferObject.Type.OUTGOING.equals(type)) {
                                            Log.d(TAG, "onConnected: Informing before starting to send.");

                                            pushReply(activeConnection, new JSONObject(), true);
                                            attach(task);

                                            result = true;
                                        } else if (TransferObject.Type.INCOMING.equals(type)) {
                                            JSONObject currentReply = new JSONObject();
                                            result = device.isTrusted;

                                            if (!result)
                                                currentReply.put(Keyword.ERROR, Keyword.ERROR_REQUIRE_TRUST);

                                            pushReply(activeConnection, currentReply, result);
                                            Log.d(TAG, "onConnected: Replied: " + currentReply.toString());
                                            Log.d(TAG, "onConnected: " + activeConnection.receive().response);

                                            if (result)
                                                attach(task);

                                            Log.d(TAG, "onConnected: " + activeConnection.receive().response);
                                        }
                                    } else
                                        responseJSON.put(Keyword.ERROR, Keyword.ERROR_NOT_ACCESSIBLE);
                                } catch (Exception e) {
                                    responseJSON.put(Keyword.ERROR, Keyword.ERROR_NOT_FOUND);
                                }
                            }
                            break;
                    }
                }

                pushReply(activeConnection, replyJSON, result);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        JSONObject analyzeResponse(ActiveConnection.Response response) throws JSONException
        {
            return response.totalLength > 0 ? new JSONObject(response.response) : new JSONObject();
        }

        void pushReply(ActiveConnection activeConnection, JSONObject reply, boolean result)
                throws JSONException, TimeoutException, IOException
        {
            activeConnection.reply(reply.put(Keyword.RESULT, result).toString());
        }

        private boolean isUpdateRequest(ActiveConnection activeConnection, JSONObject responseJSON)
                throws TimeoutException, JSONException, IOException
        {
            if (!responseJSON.has(Keyword.REQUEST))
                return false;

            JSONObject replyJSON = new JSONObject();
            String request = responseJSON.getString(Keyword.REQUEST);

            if (Keyword.REQUEST_UPDATE.equals(request)) {
                activeConnection.reply(replyJSON.put(Keyword.RESULT, true).toString());

                getSelfExecutor().submit(() -> {
                    try {
                        UpdateUtils.sendUpdate(getApplicationContext(), activeConnection.getClientAddress());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } else if (Keyword.REQUEST_UPDATE_V2.equals(request)) {
                NetworkDevice thisDevice = AppUtils.getLocalDevice(BackgroundService.this);
                File file = new File(getApplicationInfo().sourceDir);

                {
                    JSONObject reply = new JSONObject()
                            .put(Keyword.RESULT, true)
                            .put(Keyword.INDEX_FILE_SIZE, file.length())
                            .put(Keyword.APP_INFO_VERSION_CODE, thisDevice.versionCode);
                    activeConnection.reply(reply.toString());
                }

                {
                    ActiveConnection.Response responseObject = activeConnection.receive();
                    JSONObject response = new JSONObject(responseObject.response);

                    if (response.getBoolean(Keyword.RESULT) && response.getBoolean(Keyword.RESULT)) {
                        OutputStream outputStream = activeConnection.getSocket().getOutputStream();
                        FileInputStream inputStream = new FileInputStream(file);

                        byte[] buffer = new byte[AppConfig.BUFFER_LENGTH_DEFAULT];
                        int len;
                        long lastRead = 0;

                        while ((len = inputStream.read(buffer)) != -1) {
                            long currentTime = System.nanoTime();

                            if (len > 0) {
                                lastRead = currentTime;

                                outputStream.write(buffer, 0, len);
                                outputStream.flush();
                            }

                            if (currentTime - lastRead > AppConfig.DEFAULT_SOCKET_TIMEOUT * 1e6)
                                throw new TimeoutException("Did not read any bytes for 5secs.");
                        }

                        inputStream.close();
                    }
                }
            } else
                return false;

            return true;
        }
    }

    public interface AttachedTaskListener
    {
        void onAttachedToTask(BaseAttachableRunningTask task);

        void setTaskPosition(int ofTotal, int total);

        void updateTaskPosition(int addToOfTotal, int addToTotal);

        void updateTaskStatus(String text);
    }

    public abstract static class RunningTask extends StoppableJob implements Stoppable, Identifiable
    {
        private Stoppable mStoppable;
        private BackgroundService mService;
        private String mStatusText;
        private String mTitle;
        private int mIconRes;
        private long mLastNotified = 0;
        private int mHash = 0;
        private DynamicNotification mNotification;
        private PendingIntent mActivityIntent;

        protected abstract void onRun() throws InterruptedException;

        @Override
        public boolean addCloser(Closer closer)
        {
            return getStoppable().addCloser(closer);
        }

        public void forceQuit()
        {

        }

        @Override
        public List<Closer> getClosers()
        {
            return getStoppable().getClosers();
        }

        @Nullable
        public PendingIntent getContentIntent()
        {
            return mActivityIntent;
        }

        @Override
        public Identity getIdentity()
        {
            return Identity.withORs(hashCode());
        }

        public int getIconRes()
        {
            return mIconRes;
        }

        protected MediaScannerConnection getMediaScanner()
        {
            return getService().getMediaScanner();
        }

        protected NotificationHelper getNotificationHelper()
        {
            return getService().getNotificationHelper();
        }

        protected BackgroundService getService()
        {
            return mService;
        }

        public String getStatusText()
        {
            return mStatusText;
        }

        private Stoppable getStoppable()
        {
            if (mStoppable == null)
                mStoppable = new StoppableImpl();

            return mStoppable;
        }

        public String getTitle()
        {
            return mTitle;
        }

        @Override
        public boolean hasCloser(Closer closer)
        {
            return getStoppable().hasCloser(closer);
        }

        @Override
        public int hashCode()
        {
            return mHash != 0 ? mHash : super.hashCode();
        }

        public boolean interrupt()
        {
            return getStoppable().interrupt();
        }

        public boolean interrupt(boolean userAction)
        {
            return getStoppable().interrupt(userAction);
        }

        @Override
        public boolean isInterrupted()
        {
            return getStoppable().isInterrupted();
        }

        @Override
        public boolean isInterruptedByUser()
        {
            return getStoppable().isInterruptedByUser();
        }

        public boolean publishStatusText(String text)
        {
            mStatusText = text;
            long time = System.nanoTime();

            if (time - mLastNotified > 2e9) {
                mService.publishNotification(this);
                mLastNotified = time;

                return true;
            }
            return false;
        }

        @Override
        public boolean removeCloser(Closer closer)
        {
            return getStoppable().removeCloser(closer);
        }

        @Override
        public void reset()
        {
            getStoppable().reset();
        }

        @Override
        public void reset(boolean resetClosers)
        {
            getStoppable().reset(resetClosers);
        }

        @Override
        public void removeClosers()
        {
            getStoppable().removeClosers();
        }

        protected void run()
        {
            try {
                run(getStoppable());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        public boolean run(final Context context)
        {
            ServiceConnection serviceConnection = new ServiceConnection()
            {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service)
                {
                    AppUtils.startService(context, new Intent(context, BackgroundService.class));

                    BackgroundService workerService = ((LocalBinder) service).getService();
                    workerService.run(RunningTask.this);

                    context.unbindService(this);
                }

                @Override
                public void onServiceDisconnected(ComponentName name)
                {

                }
            };

            return context.bindService(new Intent(context, BackgroundService.class), serviceConnection,
                    Context.BIND_AUTO_CREATE);
        }

        public RunningTask setContentIntent(PendingIntent intent)
        {
            mActivityIntent = intent;
            return this;
        }

        public RunningTask setContentIntent(Context context, Intent intent)
        {
            mHash = hashIntent(intent);
            return setContentIntent(PendingIntent.getActivity(context, 0, intent, 0));
        }

        public RunningTask setIconRes(int iconRes)
        {
            mIconRes = iconRes;
            return this;
        }

        private void setService(@Nullable BackgroundService service)
        {
            mService = service;
        }

        public RunningTask setStoppable(Stoppable stoppable)
        {
            mStoppable = stoppable;
            return this;
        }

        public RunningTask setTitle(String title)
        {
            mTitle = title;
            return this;
        }
    }

    public interface Identifiable
    {
        Identity getIdentity();
    }

    public abstract static class BaseAttachableRunningTask extends RunningTask
    {
        public abstract void detachAnchor();
    }

    public abstract static class AttachableRunningTask<T extends AttachedTaskListener> extends BaseAttachableRunningTask
    {
        private T mAnchorListener;

        @Override
        public void detachAnchor()
        {
            mAnchorListener = null;
        }

        @Nullable
        public T getAnchorListener()
        {
            return mAnchorListener;
        }

        public AttachableRunningTask<T> setAnchorListener(T listener)
        {
            mAnchorListener = listener;
            listener.onAttachedToTask(this);
            return this;
        }

        @Override
        public boolean publishStatusText(String text)
        {
            if (mAnchorListener != null)
                mAnchorListener.updateTaskStatus(text);

            return super.publishStatusText(text);
        }
    }

    public class LocalBinder extends Binder
    {
        public BackgroundService getService()
        {
            return BackgroundService.this;
        }
    }
}
