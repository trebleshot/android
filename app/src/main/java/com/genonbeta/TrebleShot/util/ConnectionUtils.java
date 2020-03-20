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
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.wifi.*;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.android.framework.util.Stoppable;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.List;

import static com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter.*;

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

    private ConnectionUtils(Context context)
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

    public static WifiConfiguration createWifiConfig(ScanResult result, String password)
    {
        WifiConfiguration config = new WifiConfiguration();
        config.hiddenSSID = false;
        config.BSSID = result.BSSID;
        config.status = WifiConfiguration.Status.ENABLED;

        if (result.capabilities.contains("WEP")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.SSID = "\"" + result.SSID + "\"";
            config.wepTxKeyIndex = 0;
            config.wepKeys[0] = password;
        } else if (result.capabilities.contains("PSK")) {
            config.SSID = "\"" + result.SSID + "\"";
            config.preSharedKey = "\"" + password + "\"";
        } else if (result.capabilities.contains("EAP")) {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP);
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
            config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
            config.SSID = "\"" + result.SSID + "\"";
            config.preSharedKey = "\"" + password + "\"";
        } else {
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.SSID = "\"" + result.SSID + "\"";
            config.preSharedKey = null;
        }

        return config;
    }

    /**
     * @return True if disabling the network was successful
     * @deprecated Do not use this method with 10 and above.
     */
    @Deprecated
    public boolean disableCurrentNetwork()
    {
        // WONTFIX: Android 10 makes this obsolete.
        // NOTTODO: Networks added by other applications will possibly reconnect even if we disconnect them
        // This is because we are only allowed to manipulate the connections that we added.
        // And if it is the case, then the return value of disableNetwork will be false.
        return isConnectedToAnyNetwork() && getWifiManager().disconnect()
                && getWifiManager().disableNetwork(getWifiManager().getConnectionInfo().getNetworkId());
    }

    public boolean enableNetwork(int networkId)
    {
        Log.d(TAG, "enableNetwork: Enabling network: " + networkId);
        if (getWifiManager().enableNetwork(networkId, true))
            return true;

        Log.d(TAG, "toggleConnection: Could not enable the network");
        return false;
    }

    public InetAddress establishHotspotConnection(final Stoppable stoppable, final NetworkSpecifier<?> hotspotNetwork,
                                                  final ConnectionCallback connectionCallback)
    {
        return establishHotspotConnection(stoppable, hotspotNetwork, connectionCallback, (short) 4);
    }

    @WorkerThread
    private InetAddress establishHotspotConnection(final Stoppable stoppable, NetworkSpecifier<?> specifier,
                                                   final ConnectionCallback connectionCallback, short leftAttempts)
    {
        leftAttempts--;

        final int pingTimeout = 1000; // ms
        final long startTime = System.currentTimeMillis();

        InetAddress address = null;
        boolean connectionToggled = specifier instanceof NetworkSuggestion; // suggestions comes pretested and initiated

        while (true) {
            int passedTime = (int) (System.currentTimeMillis() - startTime);
            @NonNull final DhcpInfo wifiDhcpInfo = getWifiManager().getDhcpInfo();

            Log.d(TAG, "establishHotspotConnection(): Waiting to reach to the network. DhcpInfo: "
                    + wifiDhcpInfo.toString());

            if (Build.VERSION.SDK_INT < 29 && !getWifiManager().isWifiEnabled()) {
                Log.d(TAG, "establishHotspotConnection(): Wifi is off. Making a request to turn it on");

                if (!getWifiManager().setWifiEnabled(true)) {
                    Log.d(TAG, "establishHotspotConnection(): Wifi was off. The request has failed. Exiting.");
                    break;
                }
            } else if (specifier instanceof UnfamiliarNetwork) {
                Log.d(TAG, "establishHotspotConnection: The network is not ready to be used yet.");

                NetworkDescription description = ((UnfamiliarNetwork) specifier).object;
                String ssid = description.ssid;
                String bssid = description.bssid;
                String password = description.password;
                ScanResult result = findFromScanResults(ssid, bssid);

                getWifiManager().startScan();

                if (result == null)
                    Log.e(TAG, "establishHotspotConnection: No network found with the name " + ssid);
                else {
                    specifier = new HotspotNetwork(createWifiConfig(result, password));
                    Log.d(TAG, "establishHotspotConnection: Created HotspotNetwork object from scan results");
                }
            } else if (specifier instanceof HotspotNetwork && !isConnectedToNetwork((HotspotNetwork) specifier)
                    && !connectionToggled) {
                Log.d(TAG, "establishHotspotConnection(): Requested network toggle");
                connectionToggled = toggleConnection((HotspotNetwork) specifier);
            } else if (wifiDhcpInfo.gateway != 0) {
                try {
                    Inet4Address testAddress = NetworkUtils.convertInet4Address(wifiDhcpInfo.gateway);
                    NetworkInterface networkInterface = NetworkUtils.findNetworkInterface(testAddress);

                    address = testAddress;

                    if (testAddress.isReachable(pingTimeout) || testAddress.isReachable(networkInterface,
                            3600, pingTimeout)) {
                        Log.d(TAG, "establishHotspotConnection(): AP has been reached. Returning OK state.");
                        address = testAddress;
                        break;
                    } else
                        Log.d(TAG, "establishHotspotConnection(): Connection check ping failed");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else
                Log.d(TAG, "establishHotspotConnection(): No DHCP provided or connection not ready. Looping...");

            if (connectionCallback.onTimePassed(1000, passedTime) || stoppable.isInterrupted()) {
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

        return address != null || leftAttempts <= 0 ? address : establishHotspotConnection(stoppable, specifier,
                connectionCallback, leftAttempts);
    }

    /**
     * @param configuration The configuration that contains network SSID, BSSID, other fields required to filter the
     *                      network
     * @see #findFromConfigurations(String, String)
     */
    @Deprecated
    public WifiConfiguration findFromConfigurations(WifiConfiguration configuration)
    {
        return findFromConfigurations(configuration.SSID, configuration.BSSID);
    }

    /**
     * @param ssid  The SSID that will be used to filter.
     * @param bssid The MAC address of the network. Its use is prioritized when not null since it is unique.
     * @return The matching configuration or null if no configuration matched with the given parameters.
     * @deprecated The use of this method is limited to Android version 9 and below due to the deprecation of the
     * APIs it makes use of.
     */
    @Deprecated
    public WifiConfiguration findFromConfigurations(String ssid, @Nullable String bssid)
    {
        List<WifiConfiguration> list = getWifiManager().getConfiguredNetworks();
        for (WifiConfiguration config : list)
            if (bssid == null) {
                if (ssid.equalsIgnoreCase(config.SSID))
                    return config;
            } else {
                if (bssid.equalsIgnoreCase(config.BSSID))
                    return config;
            }

        return null;
    }

    public ScanResult findFromScanResults(String ssid, @Nullable String bssid) throws SecurityException
    {
        if (canReadScanResults()) {
            for (ScanResult result : getWifiManager().getScanResults())
                if (result.SSID.equalsIgnoreCase(ssid) && (bssid == null || result.BSSID.equalsIgnoreCase(bssid))) {
                    Log.d(TAG, "findFromScanResults: Found the network with capabilities: " + result.capabilities);
                    return result;
                }
        } else {
            Log.e(TAG, "findFromScanResults: Cannot read scan results");
            throw new SecurityException("You do not have permission to read the scan results");
        }

        Log.d(TAG, "findFromScanResults: Could not find the related Wi-Fi network with SSID " + ssid);
        return null;
    }

    public boolean hasLocationPermission(Context context)
    {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
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

    public boolean isConnectionToHotspotNetwork()
    {
        WifiInfo wifiInfo = getWifiManager().getConnectionInfo();
        return wifiInfo != null && getCleanNetworkName(wifiInfo.getSSID()).startsWith(AppConfig.PREFIX_ACCESS_POINT);
    }

    /**
     * @return True if connected to a Wi-Fi network.
     * @deprecated Do not use this method with 10 and above.
     */
    @Deprecated
    public boolean isConnectedToAnyNetwork()
    {
        NetworkInfo info = getConnectivityManager().getActiveNetworkInfo();
        return info != null && info.getType() == ConnectivityManager.TYPE_WIFI && info.isConnected();
    }

    public boolean isConnectedToNetwork(HotspotNetwork hotspotNetwork)
    {
        if (!isConnectedToAnyNetwork())
            return false;

        String bssid = hotspotNetwork.object.BSSID;
        Log.d(TAG, "isConnectedToNetwork: " + bssid + " othr: " + getWifiManager().getConnectionInfo().getBSSID());
        return bssid != null && bssid.equalsIgnoreCase(getWifiManager().getConnectionInfo().getBSSID());
    }

    public boolean isLocationServiceEnabled()
    {
        return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    /**
     * @return True if the mobile data connection is active.
     * @deprecated Do not use this method above 9, there is a better method in-place.
     */
    @Deprecated
    public boolean isMobileDataActive()
    {
        return mConnectivityManager.getActiveNetworkInfo() != null
                && mConnectivityManager.getActiveNetworkInfo().getType() == ConnectivityManager.TYPE_MOBILE;
    }

    /**
     * Enable and connect to the given network specification.
     *
     * @param hotspotNetwork The network specifier that will be connected to.
     * @return True when the request is successful and false when it fails.
     * @deprecated The use of this method is limited to Android version 9 and below due to the deprecation of the
     * APIs it makes use of.
     */
    public boolean startConnection(HotspotNetwork hotspotNetwork)
    {
        if (isConnectedToNetwork(hotspotNetwork)) {
            Log.d(TAG, "toggleConnection: Already connected to the network");
            return true;
        }

        if (isConnectedToAnyNetwork()) {
            Log.d(TAG, "toggleConnection: Connected to some other network, will try to disable it.");
            disableCurrentNetwork();
        }

        try {
            WifiConfiguration config = hotspotNetwork.object;
            WifiConfiguration existingConfig = findFromConfigurations(config);

            getWifiManager().disconnect();

            if (existingConfig != null) {
                Log.d(TAG, "toggleConnection: Network already exists, will try to enable it.");
                return enableNetwork(existingConfig.networkId);
            } else {
                Log.d(TAG, "toggleConnection: Network did not exist before, adding it.");
                return enableNetwork(getWifiManager().addNetwork(config));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        disableCurrentNetwork();

        return false;
    }

    @TargetApi(29)
    public int suggestNetwork(NetworkSuggestion suggestion)
    {
        final List<WifiNetworkSuggestion> suggestions = new ArrayList<>();
        suggestions.add(suggestion.object);

        return getWifiManager().addNetworkSuggestions(suggestions);
    }

    /**
     * This method activates or deactivates a given network depending on its state.
     *
     * @param hotspotNetwork The network specifier that you want to toggle the connection to.
     * @return True when the request is successful, false if otherwise.
     * @deprecated The use of this method is limited to Android version 9 and below due to the deprecation of the
     * APIs it makes use of.
     */
    @Deprecated
    public boolean toggleConnection(HotspotNetwork hotspotNetwork)
    {
        return isConnectedToNetwork(hotspotNetwork) ? getWifiManager().disconnect() : startConnection(hotspotNetwork);
    }

    public interface ConnectionCallback
    {
        boolean onTimePassed(int delimiter, long timePassed);
    }
}
