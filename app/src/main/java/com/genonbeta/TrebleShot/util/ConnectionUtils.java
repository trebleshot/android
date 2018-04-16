package com.genonbeta.TrebleShot.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.support.v4.content.ContextCompat;

import com.genonbeta.TrebleShot.adapter.NetworkDeviceListAdapter;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.object.NetworkDevice;

import java.net.ConnectException;

import okio.Timeout;

import static junit.framework.Assert.fail;

/**
 * created by: veli
 * date: 15/04/18 18:37
 */
public class ConnectionUtils
{
	private Context mContext;
	private WifiManager mWifiManager;
	private HotspotUtils mHotspotUtils;
	private LocationManager mLocationManager;
	private ConnectivityManager mConnectivityManager;

	public ConnectionUtils(Context context)
	{
		mContext = context;

		mWifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
		mLocationManager = (LocationManager) getContext().getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		mHotspotUtils = HotspotUtils.getInstance(getContext());
		mConnectivityManager = (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);
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
		if (!isConnectedToAnyNetwork())
			return false;

		return getWifiManager().disableNetwork(getWifiManager().getConnectionInfo().getNetworkId());
	}

	@WorkerThread
	public String establishHotspotConnection(Interrupter interrupter, final NetworkDeviceListAdapter.HotspotNetwork hotspotNetwork, TimeoutListener timeoutListener)
	{
		final long startTime = System.currentTimeMillis();
		boolean connectionToggled = false;
		String remoteAddress = null;

		while (remoteAddress == null) {
			int passedTime = (int) (System.currentTimeMillis() - startTime);

			if (!getWifiManager().isWifiEnabled()) {
				if (!getWifiManager().setWifiEnabled(true))
					break; // failed to start Wireless
			} else if (!isConnectedToNetwork(hotspotNetwork) && !connectionToggled) {
				connectionToggled = toggleConnection(hotspotNetwork);
			} else {
				for (AddressedInterface addressedInterface : NetworkUtils.getInterfaces(true, null)) {
					if (addressedInterface.getNetworkInterface().getDisplayName().startsWith(AppConfig.NETWORK_INTERFACE_WIFI)) {
						String testedRemoteAddress = NetworkUtils.getAddressPrefix(addressedInterface.getAssociatedAddress()) + "1";

						if (NetworkUtils.ping(testedRemoteAddress, 1000)) {
							remoteAddress = testedRemoteAddress;
							break;
						}
					}
				}
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			} finally {
				if (timeoutListener.onTimePassed(1000, passedTime) || interrupter.interrupted())
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
						fail("Please type hex pair for the password");
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
						fail("Please type hex pair for the password");
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
