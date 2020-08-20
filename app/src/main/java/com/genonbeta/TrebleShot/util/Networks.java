/*
 * Copyright (C) 2020 Veli Tasalı
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.genonbeta.TrebleShot.adapter.ActiveConnectionListAdapter;
import com.genonbeta.TrebleShot.adapter.DeviceListAdapter;
import com.genonbeta.TrebleShot.config.AppConfig;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Networks
{
    public static Inet4Address getFirstInet4Address(
            @NonNull ActiveConnectionListAdapter.EditableNetworkInterface networkInterface)
    {
        return getFirstInet4Address(networkInterface.getInterface());
    }

    public static Inet4Address getFirstInet4Address(@NonNull NetworkInterface networkInterface)
    {
        Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();

        while (addresses.hasMoreElements()) {
            InetAddress address = addresses.nextElement();

            if (address instanceof Inet4Address)
                return (Inet4Address) address;
        }

        return null;
    }

    public static List<NetworkInterface> getInterfaces(boolean ipV4only, String[] avoidedInterfaces)
    {
        List<NetworkInterface> filteredInterfaceList = new ArrayList<>();

        try {
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();

            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();
                boolean avoidedInterface = false;

                if (avoidedInterfaces != null && avoidedInterfaces.length > 0)
                    for (String match : avoidedInterfaces)
                        if (networkInterface.getDisplayName().startsWith(match))
                            avoidedInterface = true;

                if (avoidedInterface)
                    continue;

                Enumeration<InetAddress> addressList = networkInterface.getInetAddresses();

                while (addressList.hasMoreElements()) {
                    InetAddress address = addressList.nextElement();
                    if (!address.isLoopbackAddress() && (address instanceof Inet4Address || !ipV4only)) {
                        filteredInterfaceList.add(networkInterface);
                        break;
                    }
                }
            }
        } catch (Exception ignored) {
        }

        return filteredInterfaceList;
    }
}
