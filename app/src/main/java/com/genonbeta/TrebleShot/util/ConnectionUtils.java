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

import com.genonbeta.CoolSocket.CoolSocket;
import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.android.framework.util.Interrupter;

import org.json.JSONObject;

import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;

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

    public boolean canReadScanResults()
    {
        return getWifiManager().isWifiEnabled()
                && (Build.VERSION.SDK_INT < 23 || (hasLocationPermission(getContext()) && isLocationServiceEnabled()));
    }

    public boolean disableCurrentNetwork()
    {
        // TODO: Networks added by other applications will possibly reconnect even if we disconnect them
        // And it is the case that then the return value of disableNetwork will be false.
        return isConnectedToAnyNetwork()
                && getWifiManager().disconnect()
                && getWifiManager().disableNetwork(getWifiManager().getConnectionInfo().getNetworkId());
    }

    @WorkerThread
    public String establishHotspotConnection(Interrupter interrupter, final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork, TimeoutListener timeoutListener)
    {
        long startTime = System.currentTimeMillis();
        boolean connectionToggled = false;
        String remoteAddress = null;

        while (remoteAddress == null) {
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

                for (AddressedInterface addressedInterface : NetworkUtils.getInterfaces(true, null)) {
                    if (addressedInterface.getNetworkInterface().getDisplayName().startsWith(AppConfig.NETWORK_INTERFACE_WIFI)) {
                        Log.d(TAG, "establishHotspotConnection(): A network is connected. Waiting to get a response from the server");

                        DhcpInfo routeInfo = getWifiManager().getDhcpInfo();

                        if (routeInfo != null) {
                            String testedRemoteAddress = NetworkUtils.convertInet4Address(routeInfo.gateway);
                            Log.d(TAG, "establishHotspotConnection(): There is DHCP info provided waiting to reach the address " + testedRemoteAddress);

                            if (NetworkUtils.ping(testedRemoteAddress, 1000)) {
                                Log.d(TAG, "establishHotspotConnection(): AP has been reached. Returning OK state.");

                                remoteAddress = testedRemoteAddress;
                                break;
                            } else
                                Log.d(TAG, "establishHotspotConnection(): Connection check ping failed");
                        } else
                            Log.d(TAG, "establishHotspotConnection(): No DHCP provided. Looping...");
                    }
                }
            }

            if (timeoutListener.onTimePassed(1000, passedTime) || interrupter.interrupted())
                break;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                break;
            }
        }

        return remoteAddress;
    }

    @WorkerThread
    public NetworkDevice setupConnection(final AccessDatabase database, final String ipAddress, final int accessPin, final NetworkDeviceLoader.OnDeviceRegisteredListener listener)
    {
        return CommunicationBridge.connect(database, NetworkDevice.class, new CommunicationBridge.Client.ConnectionHandler()
        {
            @Override
            public void onConnect(CommunicationBridge.Client client)
            {
                try {
                    client.setSecureKey(accessPin);

                    CoolSocket.ActiveConnection activeConnection = client.connectWithHandshake(ipAddress, false);
                    NetworkDevice device = client.loadDevice(activeConnection);

                    activeConnection.reply(new JSONObject()
                            .put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE)
                            .toString());

                    JSONObject receivedReply = new JSONObject(activeConnection.receive().response);

                    if (receivedReply.has(Keyword.RESULT)
                            && receivedReply.getBoolean(Keyword.RESULT)
                            && device.deviceId != null) {
                        final NetworkDevice.Connection connection = NetworkDeviceLoader.processConnection(database, device, ipAddress);

                        device.lastUsageTime = System.currentTimeMillis();
                        device.tmpSecureKey = accessPin;
                        device.isRestricted = false;
                        device.isTrusted = true;

                        database.publish(device);

                        if (listener != null)
                            listener.onDeviceRegistered(database, device, connection);
                    }

                    client.setReturn(device);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
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
                    } else {
                        //fail("Please type hex pair for the password");
                    }
                    break;
                case 2: // WEP128
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                    if (hotspotNetwork.password != null
                            && hotspotNetwork.password.matches("[0-9A-Fa-f]*")) {
                        config.wepKeys[0] = hotspotNetwork.password;
                    } else {
                        //fail("Please type hex pair for the password");
                    }
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


    public interface TimeoutListener
    {
        boolean onTimePassed(int delimiter, long timePassed);
    }
}
