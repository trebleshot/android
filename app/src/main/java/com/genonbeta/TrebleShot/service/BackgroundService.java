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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Service;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.*;
import com.genonbeta.TrebleShot.protocol.DeviceVerificationException;
import com.genonbeta.TrebleShot.protocol.communication.CommunicationException;
import com.genonbeta.TrebleShot.protocol.communication.ContentException;
import com.genonbeta.TrebleShot.protocol.communication.NotAllowedException;
import com.genonbeta.TrebleShot.task.FileTransferTask;
import com.genonbeta.TrebleShot.task.IndexTransferTask;
import com.genonbeta.TrebleShot.util.*;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import fi.iki.elonen.NanoHTTPD;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.CoolSocket;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.IOException;
import java.util.concurrent.Executors;

public class BackgroundService extends Service
{
    public static final String TAG = BackgroundService.class.getSimpleName();

    public static final String
            ACTION_CLIPBOARD = "com.genonbeta.TrebleShot.action.CLIPBOARD",
            ACTION_DEVICE_ACQUAINTANCE = "com.genonbeta.TrebleShot.transaction.action.DEVICE_ACQUAINTANCE",
            ACTION_DEVICE_KEY_CHANGE_APPROVAL = "com.genonbeta.TrebleShot.action.DEVICE_APPROVAL",
            ACTION_END_SESSION = "com.genonbeta.TrebleShot.action.END_SESSION",
            ACTION_FILE_TRANSFER = "com.genonbeta.TrebleShot.action.FILE_TRANSFER",
            ACTION_INCOMING_TRANSFER_READY = "com.genonbeta.TrebleShot.transaction.action.INCOMING_TRANSFER_READY",
            ACTION_PIN_USED = "com.genonbeta.TrebleShot.transaction.action.PIN_USED",
            ACTION_START_TRANSFER = "com.genonbeta.intent.action.START_TRANSFER",
            ACTION_STOP_ALL_TASKS = "com.genonbeta.TrebleShot.transaction.action.STOP_ALL_TASKS",
            EXTRA_CLIPBOARD_ACCEPTED = "extraClipboardAccepted",
            EXTRA_CLIPBOARD_ID = "extraTextId",
            EXTRA_DEVICE_ADDRESS = "extraDeviceAddress",
            EXTRA_DEVICE = "extraDevice",
            EXTRA_RECEIVE_KEY = "extraReceiveKey",
            EXTRA_SEND_KEY = "extraSendKey",
            EXTRA_TRANSFER = "extraTransfer",
            EXTRA_ACCEPTED = "extraAccepted",
            EXTRA_TRANSFER_ITEM_ID = "extraTransferItemId",
            EXTRA_TRANSFER_TYPE = "extraTransferType";

    private final CommunicationServer mCommunicationServer = new CommunicationServer();
    private final LocalBinder mBinder = new LocalBinder();

    private WifiManager.WifiLock mWifiLock;
    private App mApp;

    @Override
    public IBinder onBind(Intent intent)
    {
        return mBinder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        if (getApplication() instanceof App)
            mApp = (App) getApplication();
        else {
            Log.d(TAG, "The service is not able to work with a different app class.");
            stopSelf();
            return;
        }

        WifiManager wifiManager = ((WifiManager) getApplicationContext().getSystemService(Service.WIFI_SERVICE));

        if (wifiManager != null)
            mWifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG);

        mApp.getNsdDaemon().registerService();
        mApp.getNsdDaemon().startDiscovering();

        if (mWifiLock != null)
            mWifiLock.acquire();

        takeForeground(true);
        tryStartingOrStopSelf();
    }

    private void takeForeground(boolean take)
    {
        if (take)
            startForeground(NotificationHelper.ID_BG_SERVICE, getNotificationHelper().getForegroundNotification().build());
        else
            stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, final int startId)
    {
        super.onStartCommand(intent, flags, startId);

        if (intent != null)
            Log.d(TAG, "onStart() : action = " + intent.getAction());

        if (intent != null && AppUtils.checkRunningConditions(this)) {
            if (ACTION_FILE_TRANSFER.equals(intent.getAction())) {
                Device device = intent.getParcelableExtra(EXTRA_DEVICE);
                Transfer transfer = intent.getParcelableExtra(EXTRA_TRANSFER);
                final int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
                final boolean isAccepted = intent.getBooleanExtra(EXTRA_ACCEPTED, false);

                getNotificationHelper().getUtils().cancel(notificationId);

                try {
                    if (device == null || transfer == null)
                        throw new Exception("The device or group instance is broken");

                    FileTransferTask task = FileTransferTask.createFrom(getKuick(), transfer, device,
                            TransferItem.Type.INCOMING);

                    new Thread(() -> {
                        try (CommunicationBridge bridge = CommunicationBridge.connect(getKuick(), task.addressList,
                                task.device, 0)) {
                            bridge.requestNotifyTransferState(transfer.id, isAccepted);
                            bridge.receiveResult();
                        } catch (Exception ignored) {
                        }
                    }).start();

                    if (isAccepted)
                        mApp.run(task);
                    else
                        getKuick().removeAsynchronous(mApp, task.transfer, task.device);
                } catch (Exception e) {
                    e.printStackTrace();

                    if (isAccepted)
                        getNotificationHelper().showToast(R.string.mesg_somethingWentWrong);
                }
            } else if (ACTION_DEVICE_KEY_CHANGE_APPROVAL.equals(intent.getAction())) {
                Device device = intent.getParcelableExtra(EXTRA_DEVICE);
                boolean accepted = intent.getBooleanExtra(EXTRA_ACCEPTED, false);
                int notificationId = intent.getIntExtra(NotificationUtils.EXTRA_NOTIFICATION_ID, -1);
                int receiveKey = intent.getIntExtra(EXTRA_RECEIVE_KEY, -1);
                int sendKey = intent.getIntExtra(EXTRA_SEND_KEY, -1);

                getNotificationHelper().getUtils().cancel(notificationId);

                if (device != null) {
                    device.isBlocked = !accepted;

                    if (accepted) {
                        device.receiveKey = receiveKey;
                        device.sendKey = sendKey;
                    }

                    getKuick().update(device);
                    getKuick().broadcast();
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
                        ClipboardManager cbManager = ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE));

                        if (cbManager != null) {
                            cbManager.setPrimaryClip(ClipData.newPlainText("receivedText", textStreamObject.text));
                            Toast.makeText(this, R.string.mesg_textCopiedToClipboard, Toast.LENGTH_SHORT).show();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (ACTION_END_SESSION.equals(intent.getAction())) {
                stopSelf();
            } else if (ACTION_START_TRANSFER.equals(intent.getAction()) && intent.hasExtra(EXTRA_TRANSFER)
                    && intent.hasExtra(EXTRA_DEVICE) && intent.hasExtra(EXTRA_TRANSFER_TYPE)) {
                Device device = intent.getParcelableExtra(EXTRA_DEVICE);
                Transfer transfer = intent.getParcelableExtra(EXTRA_TRANSFER);
                TransferItem.Type type = (TransferItem.Type) intent.getSerializableExtra(EXTRA_TRANSFER_TYPE);

                try {
                    if (device == null || transfer == null || type == null)
                        throw new Exception();

                    FileTransferTask task = (FileTransferTask) mApp.findTaskBy(FileTransferTask.identifyWith(
                            transfer.id, device.uid, type));

                    if (task == null)
                        mApp.run(FileTransferTask.createFrom(getKuick(), transfer, device, type));
                    else
                        Toast.makeText(this, getString(R.string.mesg_groupOngoingNotice, task.item.name),
                                Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (ACTION_STOP_ALL_TASKS.equals(intent.getAction())) {
                mApp.interruptAllTasks();
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        takeForeground(false);

        try {
            mCommunicationServer.stop();
        } catch (Exception ignored) {
        }

        mApp.getNsdDaemon().unregisterService();
        mApp.getNsdDaemon().stopDiscovering();

        if (mApp.getWebShareServer() != null)
            mApp.getWebShareServer().stop();

        ContentValues values = new ContentValues();
        values.put(Kuick.FIELD_TRANSFER_ISSHAREDONWEB, 0);
        getKuick().update(new SQLQuery.Select(Kuick.TABLE_TRANSFER)
                .setWhere(String.format("%s = ?", Kuick.FIELD_TRANSFER_ISSHAREDONWEB),
                        String.valueOf(1)), values);

        if (mApp != null) {
            HotspotManager manager = mApp.getHotspotManager();
            if (manager.unloadPreviousConfig())
                Log.d(TAG, "onDestroy: Stopping hotspot (previously started)=" + manager.disable());

            mApp.interruptAllTasks();
        }

        if (getWifiLock() != null && getWifiLock().isHeld()) {
            getWifiLock().release();
            Log.d(TAG, "onDestroy: Releasing Wi-Fi lock");
        }

        AppUtils.generateNetworkPin(this);
        getKuick().broadcast();
    }

    private NotificationHelper getNotificationHelper()
    {
        return mApp.getNotificationHelper();
    }

    private WifiManager.WifiLock getWifiLock()
    {
        return mWifiLock;
    }

    private boolean isProcessRunning(long transferId, String deviceId, TransferItem.Type type)
    {
        return mApp.findTaskBy(FileTransferTask.identifyWith(transferId, deviceId, type)) != null;
    }

    /**
     * Some services like file transfer server, web share portal server involve writing and reading data.
     * So, it is best to avoid starting them when the app doesn't have the right permissions.
     */
    public void tryStartingOrStopSelf()
    {
        boolean webServerRunning = mApp.getWebShareServer().isAlive();
        boolean commServerRunning = mCommunicationServer.isListening();

        if (webServerRunning && commServerRunning)
            return;

        try {
            if (!AppUtils.checkRunningConditions(this))
                throw new Exception("The app doesn't have the satisfactory permissions to start the services.");

            if (!commServerRunning)
                mCommunicationServer.start();

            if (!webServerRunning) {
                mApp.getWebShareServer().setAsyncRunner(new WebShareServer.BoundRunner(
                        Executors.newFixedThreadPool(AppConfig.WEB_SHARE_CONNECTION_MAX)));
                mApp.getWebShareServer().start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    class CommunicationServer extends CoolSocket
    {
        CommunicationServer()
        {
            super(AppConfig.SERVER_PORT_COMMUNICATION);
            getConfigFactory().setReadTimeout(AppConfig.DEFAULT_TIMEOUT_SOCKET);
        }

        @Override
        public void onConnected(final ActiveConnection activeConnection)
        {
            // check if the same address has other connections and limit that to 5
            try {
                activeConnection.reply(AppUtils.getDeviceId(BackgroundService.this));

                JSONObject response = activeConnection.receive().getAsJson();
                final int activePin = getDefaultPreferences().getInt(Keyword.NETWORK_PIN, -1);
                final boolean hasPin = activePin != -1 && response.has(Keyword.DEVICE_PIN)
                        && activePin == response.getInt(Keyword.DEVICE_PIN);
                final Device device = new Device();
                final DeviceAddress deviceAddress = new DeviceAddress(activeConnection.getAddress());
                boolean sendInfo = true;

                try {
                    DeviceLoader.loadAsServer(getKuick(), response, device, hasPin);
                } catch (DeviceVerificationException e) {
                    getNotificationHelper().notifyKeyChanged(device, e.receiveKey, AppUtils.generateKey());
                    throw e;
                } catch (Exception e) {
                    sendInfo = false;
                    throw e;
                } finally {
                    DeviceLoader.processConnection(getKuick(), device, deviceAddress);
                    getKuick().broadcast();

                    if (sendInfo)
                        CommunicationBridge.sendSecure(activeConnection, true, AppUtils.getLocalDeviceAsJson(
                                BackgroundService.this, device.sendKey, 0));
                }

                CommunicationBridge.sendResult(activeConnection, true);

                if (hasPin) // pin is known, should be changed. Warn the listeners.
                    sendBroadcast(new Intent(ACTION_PIN_USED));

                getKuick().broadcast();
                activeConnection.setInternalCacheLimit(1073741824); // 1MB
                response = activeConnection.receive().getAsJson();

                handleRequest(activeConnection, device, deviceAddress, hasPin, response);
            } catch (Exception e) {
                try {
                    CommunicationBridge.sendError(activeConnection, e);
                } catch (Exception ignored) {
                }
            }
        }

        private void handleRequest(ActiveConnection activeConnection, Device device, DeviceAddress deviceAddress,
                                   boolean hasPin, JSONObject response) throws JSONException, IOException,
                ReconstructionFailedException, CommunicationException
        {
            switch (response.getString(Keyword.REQUEST)) {
                case (Keyword.REQUEST_TRANSFER):
                    if (mApp.hasTaskOf(IndexTransferTask.class))
                        throw new NotAllowedException(device);
                    else {
                        long transferId = response.getLong(Keyword.TRANSFER_ID);
                        String jsonIndex = response.getString(Keyword.INDEX);

                        try {
                            getKuick().reconstruct(new Transfer(transferId));
                            throw new ContentException(ContentException.Error.AlreadyExists);
                        } catch (ReconstructionFailedException e) {
                            CommunicationBridge.sendResult(activeConnection, true);
                            mApp.run(new IndexTransferTask(transferId, jsonIndex, device, hasPin));
                        }
                    }
                    return;
                case (Keyword.REQUEST_NOTIFY_TRANSFER_STATE): {
                    int transferId = response.getInt(Keyword.TRANSFER_ID);
                    boolean isAccepted = response.getBoolean(Keyword.TRANSFER_IS_ACCEPTED);
                    Transfer transfer = new Transfer(transferId);
                    TransferMember member = new TransferMember(transfer, device, TransferItem.Type.OUTGOING);

                    getKuick().reconstruct(transfer);
                    getKuick().reconstruct(member);

                    if (!isAccepted) {
                        getKuick().remove(member);
                        getKuick().broadcast();
                    }

                    CommunicationBridge.sendResult(activeConnection, true);
                    return;
                }
                case (Keyword.REQUEST_CLIPBOARD):
                    TextStreamObject textStreamObject = new TextStreamObject(AppUtils.getUniqueNumber(),
                            response.getString(Keyword.TRANSFER_TEXT));

                    getKuick().publish(textStreamObject);
                    getKuick().broadcast();
                    getNotificationHelper().notifyClipboardRequest(device, textStreamObject);

                    CommunicationBridge.sendResult(activeConnection, true);
                    return;
                case (Keyword.REQUEST_ACQUAINTANCE):
                    sendBroadcast(new Intent(ACTION_DEVICE_ACQUAINTANCE)
                            .putExtra(EXTRA_DEVICE, device)
                            .putExtra(EXTRA_DEVICE_ADDRESS, deviceAddress));
                    CommunicationBridge.sendResult(activeConnection, true);
                    return;
                case (Keyword.REQUEST_TRANSFER_JOB):
                    int transferId = response.getInt(Keyword.TRANSFER_ID);
                    String typeValue = response.getString(Keyword.TRANSFER_TYPE);
                    TransferItem.Type type = TransferItem.Type.valueOf(typeValue);

                    // The type is reversed to match our side
                    if (TransferItem.Type.INCOMING.equals(type))
                        type = TransferItem.Type.OUTGOING;
                    else if (TransferItem.Type.OUTGOING.equals(type))
                        type = TransferItem.Type.INCOMING;

                    Transfer transfer = new Transfer(transferId);
                    getKuick().reconstruct(transfer);

                    Log.d(BackgroundService.TAG, "CommunicationServer.onConnected(): "
                            + "transferId=" + transferId + " typeValue=" + typeValue);

                    if (TransferItem.Type.INCOMING.equals(type) && !device.isTrusted)
                        CommunicationBridge.sendError(activeConnection, Keyword.ERROR_NOT_TRUSTED);
                    else if (isProcessRunning(transferId, device.uid, type))
                        throw new ContentException(ContentException.Error.NotAccessible);
                    else {
                        FileTransferTask task = new FileTransferTask();
                        task.activeConnection = activeConnection;
                        task.transfer = transfer;
                        task.device = device;
                        task.type = type;
                        task.member = new TransferMember(transfer, device, type);
                        task.index = new TransferIndex(transfer);

                        getKuick().reconstruct(task.member);
                        CommunicationBridge.sendResult(activeConnection, true);

                        mApp.attach(task);
                        return;
                    }
                default:
                    CommunicationBridge.sendResult(activeConnection, false);
            }
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
