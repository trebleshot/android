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
import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.DeviceConnection;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.util.communicationbridge.CommunicationException;
import com.genonbeta.TrebleShot.util.communicationbridge.DifferentClientException;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
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
    public static final String TAG = CommunicationBridge.class.getSimpleName();

    public static Client connect(Kuick kuick, final Client.ConnectionHandler handler)
    {
        final Client client = new Client(kuick);
        new Thread(() -> handler.onConnect(client)).start();
        return client;
    }

    public static class Client extends CoolSocket.Client
    {
        private Kuick mKuick;
        private Device mDevice;
        private int mPin = -1;

        public Client(Kuick kuick)
        {
            mKuick = kuick;
        }

        public Client(Kuick kuick, int pin)
        {
            this(kuick);
            setPin(pin);
        }

        public CoolSocket.ActiveConnection communicate(Device targetDevice, DeviceConnection targetConnection)
                throws IOException, TimeoutException, CommunicationException, JSONException
        {
            return communicate(targetDevice, targetConnection, false);
        }

        public CoolSocket.ActiveConnection communicate(Device targetDevice, DeviceConnection targetConnection,
                                                       boolean handshakeOnly)
                throws IOException, TimeoutException, CommunicationException, JSONException
        {
            setDevice(targetDevice);
            return communicate(targetConnection.toInet4Address(), handshakeOnly);
        }

        public CoolSocket.ActiveConnection communicate(InetAddress address, boolean handshakeOnly)
                throws IOException, TimeoutException, CommunicationException, JSONException
        {
            CoolSocket.ActiveConnection activeConnection = connectWithHandshake(address, handshakeOnly);
            communicate(activeConnection, handshakeOnly);
            return activeConnection;
        }

        public void communicate(CoolSocket.ActiveConnection activeConnection, boolean handshakeOnly) throws IOException,
                TimeoutException, CommunicationException, JSONException
        {
            boolean keyNotSent = getDevice() == null;
            updateDeviceIfOkay(activeConnection);

            if (!handshakeOnly && keyNotSent) {
                activeConnection.reply(new JSONObject().put(Keyword.DEVICE_INFO_KEY, getDevice().secureKey)
                        .toString());
                activeConnection.receive(); // STUB
            }
        }

        public CoolSocket.ActiveConnection connect(InetAddress inetAddress) throws IOException
        {
            if (!inetAddress.isReachable(1000))
                throw new IOException("Ping test before connection to the address has failed");

            return connect(new InetSocketAddress(inetAddress, AppConfig.SERVER_PORT_COMMUNICATION),
                    AppConfig.DEFAULT_SOCKET_TIMEOUT);
        }

        public CoolSocket.ActiveConnection connect(DeviceConnection connection) throws IOException
        {
            return connect(connection.toInet4Address());
        }

        public CoolSocket.ActiveConnection connectWithHandshake(DeviceConnection connection, boolean handshakeOnly)
                throws IOException, TimeoutException, JSONException
        {
            return connectWithHandshake(connection.toInet4Address(), handshakeOnly);
        }

        public CoolSocket.ActiveConnection connectWithHandshake(InetAddress inetAddress, boolean handshakeOnly)
                throws IOException, TimeoutException, JSONException
        {
            return handshake(connect(inetAddress), handshakeOnly);
        }

        public Context getContext()
        {
            return getKuick().getContext();
        }

        public Kuick getKuick()
        {
            return mKuick;
        }

        public Device getDevice()
        {
            return mDevice;
        }

        public CoolSocket.ActiveConnection handshake(CoolSocket.ActiveConnection activeConnection,
                                                     boolean handshakeOnly) throws IOException, TimeoutException,
                JSONException
        {
            JSONObject reply = new JSONObject()
                    .put(Keyword.HANDSHAKE_REQUIRED, true)
                    .put(Keyword.HANDSHAKE_ONLY, handshakeOnly)
                    .put(Keyword.DEVICE_INFO_SERIAL, AppUtils.getDeviceId(getContext()))
                    .put(Keyword.DEVICE_PIN, mPin);

            AppUtils.applyDeviceToJSON(getContext(), reply, mDevice != null ? mDevice.secureKey : -1);
            activeConnection.reply(reply.toString());

            return activeConnection;
        }

        public Device loadDevice(CoolSocket.ActiveConnection activeConnection) throws TimeoutException,
                IOException, CommunicationException
        {
            try {
                CoolSocket.ActiveConnection.Response response = activeConnection.receive();
                JSONObject responseJSON = new JSONObject(response.response);

                return NetworkDeviceLoader.loadFrom(getKuick(), responseJSON);
            } catch (JSONException e) {
                throw new CommunicationException("Cannot read the device from JSON");
            }
        }

        public void setDevice(Device device)
        {
            mDevice = device;
        }

        public void setPin(int pin)
        {
            mPin = pin;
        }

        protected void updateDeviceIfOkay(CoolSocket.ActiveConnection activeConnection) throws IOException,
                TimeoutException, CommunicationException
        {
            Device loadedDevice = loadDevice(activeConnection);

            NetworkDeviceLoader.processConnection(getKuick(), loadedDevice, activeConnection.getClientAddress());

            if (getDevice() != null && !getDevice().id.equals(loadedDevice.id))
                throw new DifferentClientException(getDevice(), loadedDevice);

            if (loadedDevice.clientVersion >= 1) {
                if (getDevice() == null) {
                    try {
                        Device existingDevice = new Device(loadedDevice.id);

                        AppUtils.getKuick(getContext()).reconstruct(existingDevice);
                        setDevice(existingDevice);
                    } catch (ReconstructionFailedException ignored) {
                        loadedDevice.secureKey = AppUtils.generateKey();
                    }
                }

                if (getDevice() != null) {
                    loadedDevice.applyPreferences(getDevice());

                    loadedDevice.secureKey = getDevice().secureKey;
                    loadedDevice.isRestricted = false;
                } else
                    loadedDevice.isLocal = AppUtils.getDeviceId(getContext()).equals(loadedDevice.id);
            }

            loadedDevice.lastUsageTime = System.currentTimeMillis();

            getKuick().publish(loadedDevice);
            getKuick().broadcast();
            setDevice(loadedDevice);
        }

        public interface ConnectionHandler
        {
            void onConnect(Client client);
        }
    }
}
