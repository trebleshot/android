package com.genonbeta.TrebleShot.util;

import android.content.Context;

import androidx.annotation.Nullable;

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeoutException;

/**
 * created by: Veli
 * date: 11.02.2018 15:07
 */

abstract public class CommunicationBridge implements CoolSocket.Client.ConnectionHandler
{
    public static Client connect(AccessDatabase database, final Client.ConnectionHandler handler)
    {
        return connect(database, false, handler);
    }

    public static <T> T connect(AccessDatabase database, Class<T> clazz, final Client.ConnectionHandler handler)
    {
        Client clientInstance = connect(database, true, handler);

        return clientInstance.getReturn() != null && clazz != null
                ? clazz.cast(clientInstance.getReturn())
                : null;
    }

    public static Client connect(AccessDatabase database, boolean currentThread, final Client.ConnectionHandler handler)
    {
        final Client clientInstance = new Client(database);

        if (currentThread)
            handler.onConnect(clientInstance);
        else
            new Thread()
            {
                @Override
                public void run()
                {
                    super.run();
                    handler.onConnect(clientInstance);
                }
            }.start();

        return clientInstance;
    }

    public static class Client extends CoolSocket.Client
    {
        private AccessDatabase mDatabase;
        private NetworkDevice mDevice;
        private int mSecureKey = -1;

        public Client(AccessDatabase database)
        {
            mDatabase = database;
        }

        public CoolSocket.ActiveConnection communicate(NetworkDevice targetDevice, NetworkDevice.Connection targetConnection) throws IOException, TimeoutException, DifferentClientException, CommunicationException
        {
            CoolSocket.ActiveConnection activeConnection = connectWithHandshake(targetConnection.ipAddress, false);

            communicate(activeConnection, targetDevice);

            return activeConnection;
        }

        public CoolSocket.ActiveConnection communicate(CoolSocket.ActiveConnection activeConnection, NetworkDevice targetDevice) throws IOException, TimeoutException, DifferentClientException, CommunicationException
        {
            updateDeviceIfOkay(activeConnection, targetDevice);
            return activeConnection;
        }

        public CoolSocket.ActiveConnection connect(String ipAddress) throws IOException
        {
            InetAddress inetAddress = InetAddress.getByName(ipAddress);

            if (!inetAddress.isReachable(1000))
                throw new IOException("Ping test before connection to the address has failed");

            return connect(new InetSocketAddress(ipAddress, AppConfig.SERVER_PORT_COMMUNICATION), AppConfig.DEFAULT_SOCKET_TIMEOUT);
        }

        public CoolSocket.ActiveConnection connect(NetworkDevice.Connection connection) throws IOException
        {
            return connect(connection.ipAddress);
        }

        public CoolSocket.ActiveConnection connectWithHandshake(String ipAddress, boolean handshakeOnly) throws IOException, TimeoutException, CommunicationException
        {
            return handshake(connect(ipAddress), handshakeOnly);
        }

        public Context getContext()
        {
            return getDatabase().getContext();
        }

        public AccessDatabase getDatabase()
        {
            return mDatabase;
        }

        @Nullable
        public NetworkDevice getDevice()
        {
            return mDevice;
        }

        public void setDevice(NetworkDevice device)
        {
            mDevice = device;
        }

        public CoolSocket.ActiveConnection handshake(CoolSocket.ActiveConnection activeConnection, boolean handshakeOnly) throws IOException, TimeoutException, CommunicationException
        {
            try {
                activeConnection.reply(new JSONObject()
                        .put(Keyword.HANDSHAKE_REQUIRED, true)
                        .put(Keyword.HANDSHAKE_ONLY, handshakeOnly)
                        .put(Keyword.DEVICE_INFO_SERIAL, AppUtils.getDeviceSerial(getContext()))
                        .put(Keyword.DEVICE_SECURE_KEY, mDevice == null ? mSecureKey : mDevice.tmpSecureKey)
                        .toString());
            } catch (JSONException e) {
                throw new CommunicationException("Failed to open connection between devices");
            }

            return activeConnection;
        }

        public NetworkDevice loadDevice(String ipAddress) throws TimeoutException, IOException, CommunicationException
        {
            return loadDevice(connectWithHandshake(ipAddress, true));
        }

        public NetworkDevice loadDevice(CoolSocket.ActiveConnection activeConnection) throws TimeoutException, IOException, CommunicationException
        {
            try {
                CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                JSONObject responseJSON = new JSONObject(response.response);

                return NetworkDeviceLoader.loadFrom(getDatabase(), responseJSON);
            } catch (JSONException e) {
                throw new CommunicationException("Cannot read the device from JSON");
            }
        }

        public void setSecureKey(int secureKey)
        {
            mSecureKey = secureKey;
        }

        public NetworkDevice updateDeviceIfOkay(CoolSocket.ActiveConnection activeConnection, NetworkDevice targetDevice) throws IOException, TimeoutException, CommunicationException, DifferentClientException
        {
            NetworkDevice loadedDevice = loadDevice(activeConnection);
            NetworkDevice.Connection connection = NetworkDeviceLoader.processConnection(getDatabase(), loadedDevice, activeConnection.getClientAddress());

            if (!targetDevice.deviceId.equals(loadedDevice.deviceId))
                throw new DifferentClientException("The target device did not match with the connected one");
            else {
                loadedDevice.lastUsageTime = System.currentTimeMillis();

                mDatabase.publish(loadedDevice);
                setDevice(loadedDevice);
            }

            return loadedDevice;
        }

        public interface ConnectionHandler
        {
            void onConnect(Client client);
        }
    }

    public static class DifferentClientException extends Exception
    {
        public DifferentClientException(String desc)
        {
            super(desc);
        }
    }

    public static class CommunicationException extends Exception
    {
        public CommunicationException(String desc)
        {
            super(desc);
        }
    }
}
