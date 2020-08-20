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
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.*;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import com.genonbeta.TrebleShot.App;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.Kuick;
import com.genonbeta.TrebleShot.object.Device;
import com.genonbeta.TrebleShot.object.DeviceAddress;
import com.genonbeta.TrebleShot.object.DeviceRoute;
import com.genonbeta.TrebleShot.protocol.communication.CommunicationException;
import com.genonbeta.TrebleShot.task.DeviceIntroductionTask;
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider;
import com.genonbeta.android.framework.util.Stoppable;
import org.json.JSONException;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static com.genonbeta.TrebleShot.adapter.DeviceListAdapter.NetworkDescription;

/**
 * created by: veli
 * date: 15/04/18 18:37
 */
public class ConnectionUtils
{
    public static final String TAG = ConnectionUtils.class.getSimpleName();


    private final Context mContext;
    private final WifiManager mWifiManager;
    private final LocationManager mLocationManager;
    private final ConnectivityManager mConnectivityManager;
    private boolean mWirelessEnableRequested = false;

    public ConnectionUtils(Context context)
    {
        mContext = context;
        mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mLocationManager = (LocationManager) getContext().getApplicationContext().getSystemService(
                Context.LOCATION_SERVICE);
        mConnectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static String getCleanSsid(String ssid)
    {
        if (ssid == null)
            return "";

        return ssid.replace("\"", "");
    }

    public boolean canAccessLocation()
    {
        return hasLocationPermission() && isLocationServiceEnabled();
    }

    public boolean canReadScanResults()
    {
        return getWifiManager().isWifiEnabled() && (Build.VERSION.SDK_INT < 23 || canAccessLocation());
    }

    public boolean canReadWifiInfo()
    {
        return Build.VERSION.SDK_INT < 26 || (hasLocationPermission() && isLocationServiceEnabled());
    }

    public InetAddress connectToNetwork(Stoppable stoppable, NetworkDescription description)
            throws DeviceIntroductionTask.SuggestNetworkException, ConnectionUtils.WifiInaccessibleException,
            TimeoutException, InterruptedException
    {
        if (Build.VERSION.SDK_INT >= 29) {
            final List<WifiNetworkSuggestion> suggestionList = new ArrayList<>();
            suggestionList.add(description.toNetworkSuggestion());

            int status = getWifiManager().removeNetworkSuggestions(suggestionList);
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
                    || status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID)
                status = getWifiManager().addNetworkSuggestions(suggestionList);
            switch (status) {
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP:
                    throw new DeviceIntroductionTask.SuggestNetworkException(description,
                            DeviceIntroductionTask.SuggestNetworkException.Type.ExceededLimit);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED:
                    throw new DeviceIntroductionTask.SuggestNetworkException(description,
                            DeviceIntroductionTask.SuggestNetworkException.Type.AppDisallowed);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL:
                    throw new DeviceIntroductionTask.SuggestNetworkException(description,
                            DeviceIntroductionTask.SuggestNetworkException.Type.ErrorInternal);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE:
                    throw new DeviceIntroductionTask.SuggestNetworkException(description,
                            DeviceIntroductionTask.SuggestNetworkException.Type.Duplicate);
                case WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS:
                default:
                    Log.d(TAG, "Network suggestion successful!");
            }
        }

        return establishHotspotConnection(stoppable, description);
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

    @WorkerThread
    public InetAddress establishHotspotConnection(Stoppable stoppable, NetworkDescription description)
            throws WifiInaccessibleException, InterruptedException, TimeoutException
    {
        long startTime = System.nanoTime();
        boolean connectionToggled = Build.VERSION.SDK_INT >= 29;
        boolean connectionReset = false;

        while (System.nanoTime() - startTime < AppConfig.DEFAULT_TIMEOUT_HOTSPOT * 1e6) {
            DhcpInfo wifiDhcpInfo = getWifiManager().getDhcpInfo();

            Log.d(TAG, "establishHotspotConnection(): Waiting to reach to the network. DhcpInfo: "
                    + wifiDhcpInfo.toString());

            if (!getWifiManager().isWifiEnabled()) {
                Log.d(TAG, "establishHotspotConnection(): Wifi is off. Making a request to turn it on");

                if (Build.VERSION.SDK_INT >= 29 || !getWifiManager().setWifiEnabled(true)) {
                    Log.d(TAG, "establishHotspotConnection(): Wifi was off. The request has failed. Exiting.");
                    throw new WifiInaccessibleException("Wifi won't turn on. It is either because the device is " +
                            "running Android 10 or later, or the Wi-Fi access is blocked by some other reasons.");
                }
            } else if (!connectionToggled && !isConnectedToNetwork(description)) {
                Log.d(TAG, "establishHotspotConnection: The network is not ready to be used yet.");

                getWifiManager().startScan();
                ScanResult result = findFromScanResults(description);

                if (!connectionReset && isConnectedToAnyNetwork()) {
                    disableCurrentNetwork();
                    getWifiManager().setWifiEnabled(false);
                    connectionReset = true;
                } else if (result == null)
                    Log.e(TAG, "establishHotspotConnection: No network found with the name " + description.ssid);
                else if (connectionToggled = startConnection(createWifiConfig(result, description.password)))
                    Log.d(TAG, "establishHotspotConnection: Successfully toggled network from scan results "
                            + description.ssid);
                else
                    Log.d(TAG, "establishHotspotConnection: Toggling network failed " + description.ssid);
            } else if (wifiDhcpInfo.gateway != 0) {
                Kuick kuick = AppUtils.getKuick(getContext());

                try {
                    InetAddress address = InetAddress.getByAddress(InetAddresses.toByteArray(wifiDhcpInfo.gateway));
                    DeviceAddress deviceAddress = new DeviceAddress(address);

                    CommunicationBridge.connect(kuick, deviceAddress, null, 0);
                    Log.d(TAG, "establishHotspotConnection(): AP has been reached. Returning OK state.");
                    return address;
                } catch (UnknownHostException e) {
                    Log.d(TAG, "establishHotspotConnection(): Host is unknown.", e);
                } catch (Exception e) {
                    Log.d(TAG, "establishHotspotConnection(): Connection failed.", e);
                }
            } else
                Log.d(TAG, "establishHotspotConnection(): No DHCP provided or connection not ready. Looping...");

            Thread.sleep(1000);

            if (stoppable.isInterrupted())
                throw new InterruptedException("Task is interrupted.");
        }

        throw new TimeoutException("The process took longer than expected.");
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

    public ScanResult findFromScanResults(NetworkDescription description) throws SecurityException
    {
        return findFromScanResults(description.ssid, description.bssid);
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

    public boolean hasLocationPermission()
    {
        return ContextCompat.checkSelfPermission(getContext(),
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

    public LocationManager getLocationManager()
    {
        return mLocationManager;
    }

    public WifiManager getWifiManager()
    {
        return mWifiManager;
    }

    public static boolean isClientNetwork(String ssid)
    {
        String prefix = AppConfig.PREFIX_ACCESS_POINT;
        return ssid != null && (ssid.startsWith(prefix) || ssid.startsWith("\"" + prefix));
    }

    public boolean isConnectionToHotspotNetwork()
    {
        WifiInfo wifiInfo = getWifiManager().getConnectionInfo();
        return wifiInfo != null && getCleanSsid(wifiInfo.getSSID()).startsWith(AppConfig.PREFIX_ACCESS_POINT);
    }

    /**
     * @return True if connected to a Wi-Fi network.
     */
    public boolean isConnectedToAnyNetwork()
    {
        NetworkInfo info = getConnectivityManager().getActiveNetworkInfo();
        return info != null && info.getType() == ConnectivityManager.TYPE_WIFI && info.isConnected();
    }

    public boolean isConnectedToNetwork(NetworkDescription description)
    {
        return isConnectedToNetwork(description.ssid, description.bssid);
    }

    public boolean isConnectedToNetwork(WifiConfiguration configuration)
    {
        return isConnectedToNetwork(configuration.SSID, configuration.BSSID);
    }

    public boolean isConnectedToNetwork(String ssid, String bssid)
    {
        if (!isConnectedToAnyNetwork())
            return false;
        WifiInfo wifiInfo = getWifiManager().getConnectionInfo();
        String tgSsid = getCleanSsid(wifiInfo.getSSID());

        Log.d(TAG, "isConnectedToNetwork: " + ssid + "=" + tgSsid + ":" + bssid + "=" + wifiInfo.getBSSID());
        return bssid == null ? ssid.equals(tgSsid) : bssid.equalsIgnoreCase(wifiInfo.getBSSID());
    }

    public boolean isLocationServiceEnabled()
    {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? mLocationManager.isLocationEnabled()
                : mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
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

    public boolean notifyWirelessRequestHandled()
    {
        boolean returnedState = mWirelessEnableRequested;
        mWirelessEnableRequested = false;
        return returnedState;
    }

    @WorkerThread
    public static DeviceRoute setupConnection(Context context, InetAddress inetAddress, int pin)
            throws CommunicationException, IOException, JSONException
    {
        Kuick kuick = AppUtils.getKuick(context);
        DeviceAddress deviceAddress = new DeviceAddress(inetAddress);
        CommunicationBridge bridge = CommunicationBridge.connect(kuick, deviceAddress, null, pin);
        Device device = bridge.getDevice();

        bridge.requestAcquaintance();

        if (bridge.receiveResult() && device.uid != null) {
            DeviceLoader.processConnection(kuick, device, deviceAddress);
            return new DeviceRoute(device, deviceAddress);
        }

        throw new CommunicationException("Didn't have the result and the errors were unknown");
    }

    @UiThread
    public void showConnectionOptions(Activity activity, SnackbarPlacementProvider provider,
                                      int locationPermRequestId)
    {
        if (!getWifiManager().isWifiEnabled())
            provider.createSnackbar(R.string.mesg_suggestSelfHotspot)
                    .setAction(R.string.butn_enable, view -> {
                        mWirelessEnableRequested = true;
                        turnOnWiFi(activity, provider);
                    })
                    .show();
        else if (validateLocationPermission(activity, locationPermRequestId)) {
            provider.createSnackbar(R.string.mesg_scanningSelfHotspot)
                    .setAction(R.string.butn_wifiSettings, view -> activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                    .show();
        }
    }

    /**
     * Enable and connect to the given network specification.
     *
     * @param config The network specifier that will be connected to.
     * @return True when the request is successful and false when it fails.
     * @deprecated The use of this method is limited to Android version 9 and below due to the deprecation of the
     * APIs it makes use of.
     */
    public boolean startConnection(WifiConfiguration config)
    {
        if (isConnectedToNetwork(config)) {
            Log.d(TAG, "startConnection: Already connected to the network");
            return true;
        }

        if (isConnectedToAnyNetwork()) {
            Log.d(TAG, "startConnection: Connected to some other network, will try to disable it.");
            disableCurrentNetwork();
        }

        try {
            WifiConfiguration existingConfig = findFromConfigurations(config);
            if (existingConfig != null && !getWifiManager().removeNetwork(existingConfig.networkId)) {
                Log.d(TAG, "startConnection: The config exits but could not remove it.");
            } else if (!enableNetwork(getWifiManager().addNetwork(config))) {
                Log.d(TAG, "startConnection: Could not enable the network.");
            } else if (!getWifiManager().reconnect()) {
                Log.d(TAG, "startConnection: Could not reconnect the networks.");
                ;
            } else
                return true;
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * This method activates or deactivates a given network depending on its state.
     *
     * @param config The network specifier that you want to toggle the connection to.
     * @return True when the request is successful, false if otherwise.
     * @deprecated The use of this method is limited to Android version 9 and below due to the deprecation of the
     * APIs it makes use of.
     */
    @Deprecated
    public boolean toggleConnection(WifiConfiguration config)
    {
        return isConnectedToNetwork(config) ? getWifiManager().disconnect() : startConnection(config);
    }

    public void toggleHotspot(FragmentActivity activity, SnackbarPlacementProvider provider, HotspotManager manager,
                              boolean suggestActions, int locationPermRequestId)
    {
        if (!HotspotManager.isSupported() || (Build.VERSION.SDK_INT >= 26 && !validateLocationPermission(activity,
                locationPermRequestId)))
            return;

        if (Build.VERSION.SDK_INT >= 23 && !Settings.System.canWrite(activity)) {
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_errorHotspotPermission)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_settings, (dialog, which) ->
                            activity.startActivity(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                                    .setData(Uri.parse("package:" + activity.getPackageName()))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                    .show();
        } else if (Build.VERSION.SDK_INT < 26 && !manager.isEnabled() && isMobileDataActive() && suggestActions) {
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_warningHotspotMobileActive)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_skip, (dialog, which) -> {
                        // no need to call watcher due to recycle
                        toggleHotspot(activity, provider, manager, false, locationPermRequestId);
                    })
                    .show();
        } else {
            WifiConfiguration config = manager.getConfiguration();

            if (!manager.isEnabled() || (config != null && AppUtils.getHotspotName(activity).equals(config.SSID)))
                provider.createSnackbar(manager.isEnabled() ? R.string.mesg_stoppingSelfHotspot
                        : R.string.mesg_startingSelfHotspot).show();

            toggleHotspot(activity);
        }
    }

    private void toggleHotspot(Activity activity)
    {
        try {
            App.from(activity).toggleHotspot();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }

    public void turnOnWiFi(Activity activity, SnackbarPlacementProvider provider)
    {
        if (Build.VERSION.SDK_INT >= 29)
            activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
        else if (getWifiManager().setWifiEnabled(true)) {
            provider.createSnackbar(R.string.mesg_turningWiFiOn).show();
        } else
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_wifiEnableFailed)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_settings, (dialog, which) -> activity.startActivity(
                            new Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                    .show();
    }

    public boolean validateLocationPermission(Activity activity, int permRequestId)
    {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        if (!hasLocationPermission()) {
            new AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_locationPermissionRequiredSelfHotspot)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_allow, (dialog, which) -> {
                        activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION}, permRequestId);
                    })
                    .show();
        } else if (!isLocationServiceEnabled()) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.mesg_locationDisabledSelfHotspot)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_locationSettings, (dialog, which) -> activity.startActivity(new Intent(
                            Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)))
                    .show();
        } else
            return true;

        return false;
    }

    public static class WifiInaccessibleException extends Exception
    {
        public WifiInaccessibleException(String message)
        {
            super(message);
        }
    }
}
