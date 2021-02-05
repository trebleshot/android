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

package com.genonbeta.TrebleShot.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.dataobject.Device;
import com.genonbeta.TrebleShot.dataobject.DeviceAddress;
import com.genonbeta.TrebleShot.dataobject.TransferItem;
import com.genonbeta.TrebleShot.protocol.DeviceBlockedException;
import com.genonbeta.TrebleShot.protocol.DeviceVerificationException;
import com.genonbeta.TrebleShot.protocol.communication.*;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.List;

/**
 * created by: Veli
 * date: 11.02.2018 15:07
 */

public class CommunicationBridge implements Closeable
{
    public static final String TAG = CommunicationBridge.class.getSimpleName();

    private final Kuick kuick;

    private final ActiveConnection activeConnection;

    private final Device device;

    private final DeviceAddress deviceAddress;

    public CommunicationBridge(Kuick kuick, ActiveConnection activeConnection, Device device, DeviceAddress deviceAddress)
    {
        this.kuick = kuick;
        this.activeConnection = activeConnection;
        this.device = device;
        this.deviceAddress = deviceAddress;
    }

    @Override
    public void close()
    {
        try {
            getActiveConnection().closeSafely();
        } catch (Exception ignored) {
        }
    }

    public static CommunicationBridge connect(Kuick kuick, List<DeviceAddress> addressList, @Nullable Device device,
                                              int pin)
            throws IOException, CommunicationException, JSONException
    {
        for (DeviceAddress address : addressList) {
            try {
                return connect(kuick, address, device, pin);
            } catch (IOException ignored) {
            }
        }

        throw new SocketException("Failed to connect to the socket address.");
    }

    public static CommunicationBridge connect(Kuick kuick, DeviceAddress deviceAddress, @Nullable Device device, int pin)
            throws IOException, JSONException, CommunicationException
    {
        ActiveConnection activeConnection = openConnection(kuick.getContext(), deviceAddress.inetAddress);
        String remoteDeviceId = activeConnection.receive().getAsString();

        if (device != null && device.uid != null && !device.uid.equals(remoteDeviceId)) {
            activeConnection.closeSafely();
            throw new DifferentClientException(device, remoteDeviceId);
        }

        if (device == null)
            device = new Device(remoteDeviceId);

        try {
            kuick.reconstruct(device);
        } catch (ReconstructionFailedException e) {
            device.sendKey = AppUtils.generateKey();
        }

        activeConnection.reply(AppUtils.getLocalDeviceAsJson(kuick.getContext(), device.sendKey, pin));

        DeviceLoader.processConnection(kuick, device, deviceAddress);
        DeviceLoader.loadAsClient(kuick, receiveSecure(activeConnection, device), device);
        CommunicationBridge.receiveResult(activeConnection, device);

        return new CommunicationBridge(kuick, activeConnection, device, deviceAddress);
    }

    public ActiveConnection getActiveConnection()
    {
        return activeConnection;
    }

    public Context getContext()
    {
        return getKuick().getContext();
    }

    public Device getDevice()
    {
        return device;
    }

    public DeviceAddress getDeviceAddress()
    {
        return deviceAddress;
    }

    public Kuick getKuick()
    {
        return kuick;
    }

    private static ActiveConnection openConnection(Context context, InetAddress inetAddress) throws IOException
    {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            final ConnectivityManager manager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);

            if (manager != null) {
                NetworkBinderCallback callback = new NetworkBinderCallback(manager, inetAddress);

                NetworkRequest.Builder builder = new NetworkRequest.Builder();
                builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI);
                manager.requestNetwork(builder.build(), callback);

                return callback.waitForConnection();
            } else
                throw new IOException("Connectivity manager is empty.");
        }

        return openConnection(inetAddress);
    }

    private static ActiveConnection openConnection(InetAddress inetAddress) throws IOException
    {
        return ActiveConnection.connect(new InetSocketAddress(inetAddress, AppConfig.SERVER_PORT_COMMUNICATION),
                AppConfig.DEFAULT_TIMEOUT_SOCKET);
    }

    public void requestAcquaintance() throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE));
    }

    public void requestFileTransfer(long transferId, JSONArray files) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.INDEX, files));
    }

    public void requestFileTransferStart(long transferId, TransferItem.Type type) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_TYPE, type));
    }

    public void requestNotifyTransferState(long transferId, boolean accepted) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_NOTIFY_TRANSFER_STATE)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_IS_ACCEPTED, accepted));
    }

    public void requestTextTransfer(String text) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD)
                .put(Keyword.TRANSFER_TEXT, text));
    }

    public boolean receiveResult() throws JSONException, IOException, CommunicationException
    {
        return receiveResult(getActiveConnection(), getDevice());
    }

    public static JSONObject receiveSecure(ActiveConnection connection, Device targetDevice) throws IOException,
            JSONException, CommunicationException
    {
        JSONObject jsonObject = connection.receive().getAsJson();
        if (jsonObject.has(Keyword.ERROR)) {
            final String errorCode = jsonObject.getString(Keyword.ERROR);
            switch (errorCode) {
                case Keyword.ERROR_NOT_ALLOWED:
                    throw new NotAllowedException(targetDevice);
                case Keyword.ERROR_NOT_TRUSTED:
                    throw new NotTrustedException(targetDevice);
                case Keyword.ERROR_NOT_ACCESSIBLE:
                    throw new ContentException(ContentException.Error.NotAccessible);
                case Keyword.ERROR_ALREADY_EXISTS:
                    throw new ContentException(ContentException.Error.AlreadyExists);
                case Keyword.ERROR_NOT_FOUND:
                    throw new ContentException(ContentException.Error.NotFound);
                case Keyword.ERROR_UNKNOWN:
                    throw new CommunicationException();
                default:
                    throw new UnknownCommunicationErrorException(errorCode);
            }
        }
        return jsonObject;
    }

    public static boolean receiveResult(ActiveConnection connection, Device targetDevice) throws IOException,
            JSONException, CommunicationException
    {
        return resultOf(receiveSecure(connection, targetDevice));
    }

    public static boolean resultOf(JSONObject jsonObject) throws JSONException
    {
        return jsonObject.getBoolean(Keyword.RESULT);
    }

    public static void sendError(ActiveConnection connection, Exception exception) throws IOException, JSONException,
            UnhandledCommunicationException
    {
        try {
            throw exception;
        } catch (NotTrustedException e) {
            CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_TRUSTED);
        } catch (DeviceBlockedException | DeviceVerificationException e) {
            CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_ALLOWED);
        } catch (ReconstructionFailedException e) {
            CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_FOUND);
        } catch (ContentException e) {
            CommunicationBridge.sendError(connection, e);
        } catch (Exception e) {
            throw new UnhandledCommunicationException("An unknown error was thrown during the communication", e);
        }
    }

    public static void sendError(ActiveConnection connection, ContentException e) throws IOException, JSONException
    {
        switch (e.error) {
            case NotFound:
                CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_FOUND);
                break;
            case NotAccessible:
                CommunicationBridge.sendError(connection, Keyword.ERROR_NOT_ACCESSIBLE);
                break;
            case AlreadyExists:
                CommunicationBridge.sendError(connection, Keyword.ERROR_ALREADY_EXISTS);
                break;
            default:
                CommunicationBridge.sendError(connection, Keyword.ERROR_UNKNOWN);
        }
    }

    public void sendError(String errorCode) throws IOException, JSONException
    {
        sendError(getActiveConnection(), errorCode);
    }

    public static void sendError(ActiveConnection connection, String errorCode) throws IOException, JSONException
    {
        connection.reply(new JSONObject().put(Keyword.ERROR, errorCode));
    }

    public void sendResult(boolean result) throws IOException, JSONException
    {
        sendResult(getActiveConnection(), result);
    }

    public static void sendResult(ActiveConnection connection, boolean result) throws IOException, JSONException
    {
        sendSecure(connection, result, new JSONObject());
    }

    public static void sendSecure(ActiveConnection connection, boolean result, JSONObject jsonObject)
            throws JSONException, IOException
    {
        connection.reply(jsonObject.put(Keyword.RESULT, result));
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static class NetworkBinderCallback extends ConnectivityManager.NetworkCallback
    {
        private final ConnectivityManager connectivityManager;

        private final InetAddress inetAddress;

        private final Object lock = new Object();

        private IOException exception;

        private ActiveConnection resultConnection;

        public NetworkBinderCallback(ConnectivityManager connectivityManager, InetAddress inetAddress)
        {
            this.connectivityManager = connectivityManager;
            this.inetAddress = inetAddress;
        }

        @Override
        public void onAvailable(@NonNull Network network)
        {
            super.onAvailable(network);

            if (!bindNetwork(network)) {
                Log.d(TAG, "onAvailable: Failed to bind network " + network);
                return;
            }

            try {
                resultConnection = openConnection(inetAddress);
            } catch (IOException e) {
                exception = e;
            } catch (Exception e) {
                e.printStackTrace();
                exception = new IOException(e);
            } finally {
                synchronized (lock) {
                    lock.notifyAll();
                }
                connectivityManager.unregisterNetworkCallback(this);
                bindNetwork(null);
            }
        }

        @Override
        public void onUnavailable()
        {
            super.onUnavailable();
            Log.d(TAG, "onUnavailable: No network was available for the requested network type. Opening by the " +
                    "default network");

            try {
                resultConnection = openConnection(inetAddress);
            } catch (IOException e) {
                exception = e;
            } finally {
                synchronized (lock) {
                    lock.notifyAll();
                }
            }
        }

        public boolean bindNetwork(@Nullable Network network)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                return connectivityManager.bindProcessToNetwork(network);

            return ConnectivityManager.setProcessDefaultNetwork(network);
        }

        public ActiveConnection waitForConnection() throws IOException
        {
            try {
                synchronized (lock) {
                    lock.wait(AppConfig.DEFAULT_TIMEOUT_SOCKET);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                exception = new IOException(e);
            }

            if (resultConnection == null)
                throw new IOException("No connection is handed over after waiting.");
            else if (exception != null)
                throw exception;

            return resultConnection;
        }
    }
}