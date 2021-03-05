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
package org.monora.uprotocol.client.android.util

import android.Manifest.permission.*
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.*
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.annotation.UiThread
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.genonbeta.android.framework.util.Stoppable
import kotlinx.coroutines.delay
import org.json.JSONException
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.model.NetworkDescription
import org.monora.uprotocol.client.android.service.backgroundservice.TaskStoppedException
import org.monora.uprotocol.client.android.task.DeviceIntroductionTask.SuggestNetworkException
import org.monora.uprotocol.core.protocol.communication.CommunicationException
import java.io.IOException
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeoutException

/**
 * created by: veli
 * date: 15/04/18 18:37
 */
class Connections(contextLocal: Context) {
    val context: Context = contextLocal.applicationContext

    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val isLocationServiceEnabled: Boolean
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            this.locationManager.isLocationEnabled
        } else {
            this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

    val p2pManager
        get() = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

    private var wirelessEnableRequested = false

    fun canAccessLocation(): Boolean {
        return hasLocationPermission() && isLocationServiceEnabled
    }

    private fun canReadScanResults(): Boolean {
        return wifiManager.isWifiEnabled && (Build.VERSION.SDK_INT < 23 || canAccessLocation())
    }

    fun canReadWifiInfo(): Boolean {
        return Build.VERSION.SDK_INT < 26 || hasLocationPermission() && isLocationServiceEnabled
    }

    @Throws(
        SuggestNetworkException::class,
        WifiInaccessibleException::class,
        TimeoutException::class,
        InterruptedException::class,
        IOException::class,
        CommunicationException::class,
        TaskStoppedException::class,
        JSONException::class
    )
    fun connectToNetwork(stoppable: Stoppable, description: NetworkDescription, pin: Int): ClientRoute {
        if (Build.VERSION.SDK_INT >= 29) {
            val suggestionList: MutableList<WifiNetworkSuggestion> = ArrayList<WifiNetworkSuggestion>()
            suggestionList.add(description.toNetworkSuggestion())
            var status: Int = wifiManager.removeNetworkSuggestions(suggestionList)
            if (status == WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS
                || status == WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_REMOVE_INVALID
            ) status = wifiManager.addNetworkSuggestions(suggestionList)
            when (status) {
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_EXCEEDS_MAX_PER_APP -> throw SuggestNetworkException(
                    description, SuggestNetworkException.Type.ExceededLimit
                )
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_APP_DISALLOWED -> throw SuggestNetworkException(
                    description, SuggestNetworkException.Type.AppDisallowed
                )
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_INTERNAL -> throw SuggestNetworkException(
                    description, SuggestNetworkException.Type.ErrorInternal
                )
                WifiManager.STATUS_NETWORK_SUGGESTIONS_ERROR_ADD_DUPLICATE -> throw SuggestNetworkException(
                    description, SuggestNetworkException.Type.Duplicate
                )
                WifiManager.STATUS_NETWORK_SUGGESTIONS_SUCCESS -> Log.d(TAG, "Network suggestion successful!")
                else -> Log.d(TAG, "Network suggestion successful!")
            }
        }
        return establishHotspotConnection(stoppable, description, pin)
    }

    /**
     * @return True if disabling the network was successful
     */
    @Deprecated("Do not use this method with 10 and above.")
    fun disableCurrentNetwork(): Boolean {
        // WONTFIX: Android 10 makes this obsolete.
        // NOTTODO: Networks added by other applications will possibly reconnect even if we disconnect them.
        // This is because we are only allowed to manipulate the networks that we added.
        // And if it is the case, then the return value of disableNetwork will be false.
        return (isConnectedToAnyNetwork() && wifiManager.disconnect()
                && wifiManager.disableNetwork(wifiManager.connectionInfo.networkId))
    }

    private fun enableNetwork(networkId: Int): Boolean {
        Log.d(TAG, "enableNetwork: Enabling network: $networkId")
        if (wifiManager.enableNetwork(networkId, true)) return true
        Log.d(TAG, "toggleConnection: Could not enable the network")
        return false
    }

    @WorkerThread
    @Throws(
        WifiInaccessibleException::class,
        TimeoutException::class,
        IOException::class,
        CommunicationException::class,
        TaskStoppedException::class,
        InterruptedException::class,
        JSONException::class
    )
    suspend fun establishHotspotConnection(
        stoppable: Stoppable,
        description: NetworkDescription,
        pin: Int,
    ): ClientRoute {
        val timeout = (System.nanoTime() + AppConfig.DEFAULT_TIMEOUT_HOTSPOT * 1e6).toLong()
        var toggled = Build.VERSION.SDK_INT >= 29
        var connectionReset = false

        while (true) {
            val timedOut = System.nanoTime() > timeout
            val wifiDhcpInfo: DhcpInfo = wifiManager.dhcpInfo

            Log.d(TAG, "establishHotspotConnection(): Waiting for the network. DhcpInfo: $wifiDhcpInfo")

            if (!wifiManager.isWifiEnabled) {
                Log.d(TAG, "establishHotspotConnection(): Wifi is off. Making a request to turn it on.")

                if (Build.VERSION.SDK_INT >= 29 || !wifiManager.setWifiEnabled(true)) {
                    Log.d(TAG, "establishHotspotConnection(): Wifi was off. The request has failed. Exiting.")
                    throw WifiInaccessibleException("The device is running Android 10 or Wi-Fi is inaccessible.")
                }
            } else if (!toggled && !isConnectedToNetwork(description)) {
                Log.d(TAG, "establishHotspotConnection(): The network is not ready yet.")
                wifiManager.startScan()

                val result = findFromScanResults(description)

                if (!connectionReset && isConnectedToAnyNetwork()) {
                    disableCurrentNetwork()
                    wifiManager.setWifiEnabled(false)
                    connectionReset = true
                } else if (result == null) {
                    Log.e(TAG, "establishHotspotConnection(): No network found? " + description.ssid)
                } else if (startConnection(createWifiConfig(result, description.password)).also { toggled = it }) {
                    Log.d(TAG, "establishHotspotConnection(): Toggled network using results " + description.ssid)
                } else {
                    Log.d(TAG, "establishHotspotConnection(): Failed to toggle network? " + description.ssid)
                }
            } else if (wifiDhcpInfo.gateway != 0) {
                try {
                    val address = InetAddress.getByAddress(InetAddresses.toByteArray(wifiDhcpInfo.gateway))
                    return setUpConnection(context, address, pin)
                } catch (e: Exception) {
                    Log.d(TAG, "establishHotspotConnection(): Connection failed.", e)

                    if (timedOut) {
                        throw e
                    }
                }
            } else {
                Log.d(TAG, "establishHotspotConnection(): No DHCP provided or connection not ready. Looping...")
            }

            delay(1000)

            if (stoppable.interrupted()) {
                throw TaskStoppedException("Task has been stopped.", stoppable.interruptedByUser())
            } else if (timedOut) {
                throw TimeoutException("The process took longer than expected.")
            }
        }
    }

    /**
     * @param configuration The configuration that contains network SSID, BSSID, other fields required to filter the
     * network
     * @see findFromConfigurations
     */
    @Deprecated(
        "The use of this method is limited to Android version 9 and below due to the deprecation of the "
                + "APIs it makes use of."
    )
    @RequiresPermission(allOf = [ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE])
    fun findFromConfigurations(configuration: WifiConfiguration): WifiConfiguration? {
        return findFromConfigurations(configuration.SSID, configuration.BSSID)
    }

    /**
     * @param ssid  The SSID that will be used to filter.
     * @param bssid The MAC address of the network. Its use is prioritized when not null since it is unique.
     * @return The matching configuration or null if no configuration matched with the given parameters.
     */
    @Deprecated(
        """The use of this method is limited to Android version 9 and below due to the deprecation of the
      APIs it makes use of."""
    )
    @RequiresPermission(allOf = [ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE])
    fun findFromConfigurations(ssid: String, bssid: String?): WifiConfiguration? {
        val list: List<WifiConfiguration> = wifiManager.configuredNetworks
        for (config in list) {
            if (bssid == null) {
                if (ssid.equals(config.SSID, ignoreCase = true)) return config
            } else {
                if (bssid.equals(config.BSSID, ignoreCase = true)) return config
            }
        }
        return null
    }

    @Throws(SecurityException::class)
    fun findFromScanResults(description: NetworkDescription): ScanResult? {
        return findFromScanResults(description.ssid, description.bssid)
    }

    @Throws(SecurityException::class)
    fun findFromScanResults(ssid: String, bssid: String?): ScanResult? {
        if (canReadScanResults()) {
            for (result in wifiManager.scanResults) {
                if (result.SSID.equals(ssid, ignoreCase = true)
                    && (bssid == null || result.BSSID.equals(bssid, ignoreCase = true))
                ) {
                    Log.d(TAG, "findFromScanResults: Found the network with capabilities: " + result.capabilities)
                    return result
                }
            }
        } else {
            Log.e(TAG, "findFromScanResults: Cannot read scan results")
            throw SecurityException("You do not have permission to read the scan results")
        }
        Log.d(TAG, "findFromScanResults: Could not find the related Wi-Fi network with SSID $ssid")
        return null
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, ACCESS_COARSE_LOCATION) == PERMISSION_GRANTED
    }

    fun isConnectionToHotspotNetwork(): Boolean {
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        return getCleanSsid(wifiInfo.ssid).startsWith(AppConfig.PREFIX_ACCESS_POINT)
    }

    /**
     * @return True if connected to a Wi-Fi network.
     */
    fun isConnectedToAnyNetwork(): Boolean {
        val info: NetworkInfo? = this.connectivityManager.activeNetworkInfo
        return info != null && info.type == ConnectivityManager.TYPE_WIFI && info.isConnected
    }

    private fun isConnectedToNetwork(description: NetworkDescription): Boolean {
        return isConnectedToNetwork(description.ssid, description.bssid)
    }

    private fun isConnectedToNetwork(configuration: WifiConfiguration): Boolean {
        return isConnectedToNetwork(configuration.SSID, configuration.BSSID)
    }

    private fun isConnectedToNetwork(ssid: String, bssid: String?): Boolean {
        if (!isConnectedToAnyNetwork()) return false
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val tgSsid = getCleanSsid(wifiInfo.ssid)
        Log.d(TAG, "isConnectedToNetwork: " + ssid + "=" + tgSsid + ":" + bssid + "=" + wifiInfo.bssid)
        return bssid?.equals(wifiInfo.bssid, ignoreCase = true) ?: (ssid == tgSsid)
    }

    /**
     * @return True if the mobile data connection is active.
     */
    @Deprecated("Do not use this method above 9, there is a better method in-place.")
    fun isMobileDataActive(): Boolean {
        return connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
    }

    fun notifyWirelessRequestHandled(): Boolean {
        val returnedState = wirelessEnableRequested
        wirelessEnableRequested = false
        return returnedState
    }

    @UiThread
    fun showConnectionOptions(activity: Activity, provider: SnackbarPlacementProvider, locationPermRequestId: Int) {
        if (!wifiManager.isWifiEnabled)
            provider.createSnackbar(R.string.mesg_suggestSelfHotspot)
                ?.setAction(R.string.butn_enable) {
                    wirelessEnableRequested = true
                    turnOnWiFi(activity, provider)
                }?.show()
        else if (validateLocationPermission(activity, locationPermRequestId)) {
            provider.createSnackbar(R.string.mesg_scanningSelfHotspot)
                ?.setAction(R.string.butn_wifiSettings) {
                    activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }?.show()
        }
    }

    /**
     * Enable and connect to the given network specification.
     *
     * @param config The network specifier that will be connected to.
     * @return True when the request is successful and false when it fails.
     */
    @Deprecated(
        """The use of this method is limited to Android version 9 and below due to the deprecation of the
      APIs it makes use of."""
    )
    fun startConnection(config: WifiConfiguration): Boolean {
        if (isConnectedToNetwork(config)) {
            Log.d(TAG, "startConnection: Already connected to the network.")
            return true
        }
        if (isConnectedToAnyNetwork()) {
            Log.d(TAG, "startConnection: Connected to some other network, will try to disable it.")
            disableCurrentNetwork()
        }
        try {
            val existingConfig: WifiConfiguration? = findFromConfigurations(config)

            if (existingConfig != null && !wifiManager.removeNetwork(existingConfig.networkId)) {
                Log.d(TAG, "startConnection: The config exits but could not remove it.")
            } else if (!enableNetwork(wifiManager.addNetwork(config))) {
                Log.d(TAG, "startConnection: Could not enable the network.")
            } else if (!wifiManager.reconnect()) {
                Log.d(TAG, "startConnection: Could not reconnect the networks.")
            } else {
                return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    /**
     * This method activates or deactivates a given network depending on its state.
     *
     * @param config The network specifier that you want to toggle the connection to.
     * @return True when the request is successful, false if otherwise.
     */
    @Deprecated("The use of this method is limited to Android version 9 and below.")
    fun toggleConnection(config: WifiConfiguration): Boolean {
        return if (isConnectedToNetwork(config)) wifiManager.disconnect() else startConnection(config)
    }

    fun toggleHotspot(
        backend: BackgroundBackend,
        activity: FragmentActivity,
        provider: SnackbarPlacementProvider,
        manager: HotspotManager,
        suggestActions: Boolean,
        locationPermRequestId: Int,
    ) {
        if (!HotspotManager.supported || Build.VERSION.SDK_INT >= 26
            && !validateLocationPermission(activity, locationPermRequestId)
        ) return

        // Android introduced permissions in 23 and this permission is not needed for local only hotspot introduced
        // in 26
        if (Build.VERSION.SDK_INT in 23..25 && !Settings.System.canWrite(context)) {
            AlertDialog.Builder(activity)
                .setMessage(R.string.mesg_errorHotspotPermission)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_settings) { _: DialogInterface?, _: Int ->
                    activity.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            .setData(Uri.parse("package:" + activity.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                .show()
        } else if (Build.VERSION.SDK_INT < 26 && !manager.enabled && isMobileDataActive() && suggestActions) {
            AlertDialog.Builder(activity)
                .setMessage(R.string.mesg_warningHotspotMobileActive)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_skip) { _: DialogInterface?, _: Int ->
                    // no need to call watcher due to recycle
                    toggleHotspot(backend, activity, provider, manager, false, locationPermRequestId)
                }
                .show()
        } else {
            val config: WifiConfiguration? = manager.configuration
            if (!manager.enabled || config != null) {
                provider.createSnackbar(
                    if (manager.enabled) R.string.mesg_stoppingSelfHotspot else R.string.mesg_startingSelfHotspot
                )?.show()
            }

            backend.toggleHotspot()
        }
    }

    fun turnOnWiFi(activity: Activity, provider: SnackbarPlacementProvider) {
        when {
            Build.VERSION.SDK_INT >= 29 -> activity.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            wifiManager.setWifiEnabled(true) -> provider.createSnackbar(R.string.mesg_turningWiFiOn)?.show()
            else -> {
                AlertDialog.Builder(activity)
                    .setMessage(R.string.mesg_wifiEnableFailed)
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_settings) { _: DialogInterface?, _: Int ->
                        activity.startActivity(
                            Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    .show()
            }
        }
    }

    fun validateLocationPermission(activity: Activity, permRequestId: Int): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true
        if (!hasLocationPermission()) {
            AlertDialog.Builder(activity)
                .setMessage(R.string.mesg_locationPermissionRequiredSelfHotspot)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_allow) { _: DialogInterface?, _: Int ->
                    activity.requestPermissions(
                        arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION), permRequestId
                    )
                }
                .show()
        } else if (!isLocationServiceEnabled) {
            AlertDialog.Builder(activity)
                .setMessage(R.string.mesg_locationDisabledSelfHotspot)
                .setNegativeButton(R.string.butn_cancel, null)
                .setPositiveButton(R.string.butn_locationSettings) { _: DialogInterface?, _: Int ->
                    activity.startActivity(
                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                .show()
        } else return true
        return false
    }

    class WifiInaccessibleException(message: String?) : Exception(message)

    companion object {
        private val TAG = Connections::class.simpleName

        fun getCleanSsid(ssid: String?): String {
            return ssid?.replace("\"", "") ?: ""
        }

        fun createWifiConfig(result: ScanResult, password: String?): WifiConfiguration {
            val config = WifiConfiguration()
            config.hiddenSSID = false
            config.BSSID = result.BSSID
            config.status = WifiConfiguration.Status.ENABLED
            when {
                result.capabilities.contains("WEP") -> {
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                    config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104)
                    config.SSID = "\"" + result.SSID + "\""
                    config.wepTxKeyIndex = 0
                    config.wepKeys[0] = password
                }
                result.capabilities.contains("PSK") -> {
                    config.SSID = "\"" + result.SSID + "\""
                    config.preSharedKey = "\"" + password + "\""
                }
                result.capabilities.contains("EAP") -> {
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_EAP)
                    config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                    config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP)
                    config.allowedProtocols.set(WifiConfiguration.Protocol.WPA)
                    config.SSID = "\"" + result.SSID + "\""
                    config.preSharedKey = "\"" + password + "\""
                }
                else -> {
                    config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
                    config.SSID = "\"" + result.SSID + "\""
                    config.preSharedKey = null
                }
            }
            return config
        }

        @WorkerThread
        @Throws(CommunicationException::class, IOException::class, JSONException::class)
        fun setUpConnection(context: Context, inetAddress: InetAddress, pin: Int): ClientRoute {
            // TODO: 2/26/21 Fix set up connection
            /*val bridge = CommunicationBridge.connect(kuick, deviceAddress, null, pin)
            val device = bridge.device
            bridge.requestAcquaintance()
            if (bridge.receiveResult()) {
                Log.d(TAG, "setupConnection(): AP has been reached. Returning OK state.")
                DeviceLoader.processConnection(kuick, device, deviceAddress)
                return ClientRoute(device, deviceAddress)
            }*/
            throw IOException("Didn't have the result and the errors were unknown")
        }
    }
}