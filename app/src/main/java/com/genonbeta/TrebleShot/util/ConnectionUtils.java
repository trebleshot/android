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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;

import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.android.framework.util.Interrupter;

/**
 * created by: veli
 * date: 15/04/18 18:37
 */
public class ConnectionUtils
{
    public static final String TAG = ConnectionUtils.class.getSimpleName();

    private Context mContext;
    private WifiManager mWifiManager;
    private HotspotUtils mHotspotUtils;
    private LocationManager mLocationManager;
    private ConnectivityManager mConnectivityManager;

    ConnectionUtils(Context context)
    {
        mContext = context;

        mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mLocationManager = (LocationManager) getContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        mHotspotUtils = HotspotUtils.getInstance(getContext());
        mConnectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static ConnectionUtils getInstance(Context context)
    {
        return new ConnectionUtils(context);
    }

    public static String getCleanNetworkName(String networkName)
    {
        if (networkName == null)
            return "";

        return networkName.replace("\"", "");
    }

    public boolean canAccessLocation()
    {
        return hasLocationPermission(getContext()) && isLocationServiceEnabled();
    }

    public boolean canReadScanResults()
    {
        return getWifiManager().isWifiEnabled() && (Build.VERSION.SDK_INT < 23 || canAccessLocation());
    }

    public boolean disableCurrentNetwork()
    {
        // This is because we are only allowed to manipulate the connections that we added.
        // And if it is the case, then the return value of disableNetwork will be false.
        return isConnectedToAnyNetwork()
                && getWifiManager().disconnect()
                && getWifiManager().disableNetwork(getWifiManager().getConnectionInfo().getNetworkId());
    }

    @WorkerThread
    public String establishHotspotConnection(final Interrupter interrupter,
                                             final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork,
                                             final ConnectionCallback connectionCallback)
    {
        final int pingTimeout = 1000; // ms
        final long startTime = System.currentTimeMillis();

        String remoteAddress = null;
        boolean connectionToggled = false;

        while (true) {
            int passedTime = (int) (System.currentTimeMillis() - startTime);

            if (!getWifiManager().isWifiEnabled()) {
                Log.d(TAG, "establishHotspotConnection(): Wifi is off. Making a request to turn it on");

                if (!getWifiManager().setWifiEnabled(true)) {
                    Log.d(TAG, "establishHotspotConnection(): Wifi was off. The request has failed. Exiting.");
                    break;
                }
            } else if (!isConnectedToNetwork(hotspotNetwork) && !connectionToggled) {
                Log.d(TAG, "establishHotspotConnection(): Requested network toggle");
                toggleConnection(hotspotNetwork);

                connectionToggled = true;
            } else {
                Log.d(TAG, "establishHotspotConnection(): Waiting to connect to the server");
                final DhcpInfo routeInfo = getWifiManager().getDhcpInfo();

                if (routeInfo != null && routeInfo.gateway > 0) {
                    final String testedRemoteAddress = NetworkUtils.convertInet4Address(routeInfo.gateway);

                    Log.d(TAG, String.format("establishHotspotConnection(): DhcpInfo: gateway: %s dns1: %s dns2: %s ipAddr: %s serverAddr: %s netMask: %s",
                            testedRemoteAddress,
                            NetworkUtils.convertInet4Address(routeInfo.dns1),
                            NetworkUtils.convertInet4Address(routeInfo.dns2),
                            NetworkUtils.convertInet4Address(routeInfo.ipAddress),
                            NetworkUtils.convertInet4Address(routeInfo.serverAddress),
                            NetworkUtils.convertInet4Address(routeInfo.netmask)));

                    Log.d(TAG, "establishHotspotConnection(): There is DHCP info provided waiting to reach the address " + testedRemoteAddress);

                    if (NetworkUtils.ping(testedRemoteAddress, pingTimeout)) {
                        Log.d(TAG, "establishHotspotConnection(): AP has been reached. Returning OK state.");
                        remoteAddress = testedRemoteAddress;
                        break;
                    } else
                        Log.d(TAG, "establishHotspotConnection(): Connection check ping failed");
                } else
                    Log.d(TAG, "establishHotspotConnection(): No DHCP provided. Looping...");
            }

            if (connectionCallback.onTimePassed(1000, passedTime) || interrupter.interrupted()) {
                Log.d(TAG, "establishHotspotConnection(): Timed out or onTimePassed returned true. Exiting...");
                break;
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        return remoteAddress;
    }

    public boolean hasLocationPermission(Context context)
    {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    public Context getContext()
    {
        return mContext;
    }

    public ConnectivityManager getConnectivityManager()
    {
        return mConnectivityManager;
    }

    public HotspotUtils getHotspotUtils()
    {
        return mHotspotUtils;
    }

    public LocationManager getLocationManager()
    {
        return mLocationManager;
    }

    public WifiManager getWifiManager()
    {
        return mWifiManager;
    }

    public boolean isConnectionSelfNetwork()
    {
        WifiInfo wifiInfo = getWifiManager().getConnectionInfo();

        return wifiInfo != null
                && getCleanNetworkName(wifiInfo.getSSID()).startsWith(AppConfig.PREFIX_ACCESS_POINT);
    }

    public boolean isConnectedToAnyNetwork()
    {
        NetworkInfo info = getConnectivityManager().getActiveNetworkInfo();

        return info != null
                && info.getType() == ConnectivityManager.TYPE_WIFI
                && info.isConnected();
    }

    public boolean isConnectedToNetwork(NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
    {
        if (!isConnectedToAnyNetwork())
            return false;

        if (hotspotNetwork.BSSID != null)
            return hotspotNetwork.BSSID.equals(getWifiManager().getConnectionInfo().getBSSID());

        return hotspotNetwork.SSID.equals(getCleanNetworkName(getWifiManager().getConnectionInfo().getSSID()));
    }

    public boolean isLocationServiceEnabled()
    {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    public boolean isMobileDataActive()
    {
        return mConnectivityManager.getActiveNetworkInfo() != null
                && mConnectivityManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;
    }

    public boolean toggleConnection(NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork)
    {
        if (!isConnectedToNetwork(hotspotNetwork)) {
            if (isConnectedToAnyNetwork())
                disableCurrentNetwork();

            WifiConfiguration config = new WifiConfiguration();

            config.SSID = String.format("\"%s\"", hotspotNetwork.SSID);

            switch (hotspotNetwork.keyManagement) {
                case 0: // OPEN
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    break;
                case 1: // WEP64
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);

                    if (hotspotNetwork.password != null
                            && hotspotNetwork.password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = hotspotNetwork.password;
                    } else
                        Log.d(TAG, "Please type hex pair for the password");
                    break;
                case 2: // WEP128
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                    if (hotspotNetwork.password != null
                            && hotspotNetwork.password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = hotspotNetwork.password;
                    } else
                        Log.d(TAG, "Please type hex pair for the password");
                    break;
                case 3: // WPA_TKIP
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                    if (hotspotNetwork.password != null
                            && hotspotNetwork.password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = hotspotNetwork.password;
                    } else {
                        config.preSharedKey = '"' + hotspotNetwork.password + '"';
                    }
                    break;
                case 4: // WPA2_AES
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                    config.allowedProtocols.set(WifiConfiguration.Protocol.RSN);

                    if (hotspotNetwork.password != null
                            && hotspotNetwork.password.matches("[0-9A-Fa-f]{64}")) {
                        config.preSharedKey = hotspotNetwork.password;
                    } else {
                        config.preSharedKey = '"' + hotspotNetwork.password + '"';
                    }
                    break;
            }

            int netId = getWifiManager().addNetwork(config);

            getWifiManager().disconnect();
            getWifiManager().enableNetwork(netId, true);

            return getWifiManager().reconnect();
        }

        disableCurrentNetwork();

        return false;
    }

    public interface ConnectionCallback
    {
        boolean onTimePassed(int delimiter, long timePassed);
    }
}
