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
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.protocol.DeviceInsecureException;
import com.genonbeta.TrebleShot.util.communicationbridge.DifferentClientException;
import com.genonbeta.android.database.exception.ReconstructionFailedException;
import org.json.JSONException;
import org.json.JSONObject;
import org.monora.coolsocket.core.session.ActiveConnection;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;

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
    public void close() throws IOException
    {
        activeConnection.close();
    }

    public static CommunicationBridge connect(Kuick kuick, DeviceAddress deviceAddress, @Nullable Device device, int pin)
            throws IOException, DifferentClientException, JSONException
    {
        ActiveConnection activeConnection = openConnection(deviceAddress.inetAddress);
        String remoteDeviceId = activeConnection.receive().getAsString();

        if (device != null && device.uid != null && !device.uid.equals(remoteDeviceId))
            throw new DifferentClientException(device, remoteDeviceId);

        if (device == null)
            device = new Device(remoteDeviceId);

        try {
            kuick.reconstruct(device);
        } catch (ReconstructionFailedException e) {
            device.sendKey = AppUtils.generateKey();
        }

        activeConnection.reply(AppUtils.getLocalDeviceAsJson(kuick.getContext(), device, pin));

        try {
            DeviceLoader.loadFrom(kuick, activeConnection.receive().getAsJson(), device, false, true);
        } catch (DeviceInsecureException ignored) {
            device.isBlocked = false;
            kuick.publish(device);
        }

        DeviceLoader.processConnection(kuick, device, deviceAddress);

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

    public void notifyStateOfTransferRequest(long groupId, boolean accepted) throws JSONException, IOException
    {
        getActiveConnection().reply(new JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_STATE)
                .put(Keyword.TRANSFER_GROUP_ID, groupId)
                .put(Keyword.TRANSFER_IS_ACCEPTED, accepted));
    }

    public static ActiveConnection openConnection(InetAddress inetAddress) throws IOException
    {
        return ActiveConnection.connect(new InetSocketAddress(inetAddress, AppConfig.SERVER_PORT_COMMUNICATION),
                AppConfig.DEFAULT_SOCKET_TIMEOUT);
    }
}
