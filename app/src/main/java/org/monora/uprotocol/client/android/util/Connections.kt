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
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.DhcpInfo
import android.net.NetworkInfo
import android.net.Uri
import android.net.wifi.ScanResult
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSuggestion
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.core.content.ContextCompat
import com.genonbeta.android.framework.ui.callback.SnackbarPlacementProvider
import com.genonbeta.android.framework.util.Stoppable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.backend.BackgroundBackend
import org.monora.uprotocol.client.android.config.AppConfig
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

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    val p2pManager
        get() = context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager

    val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager

    private var wirelessEnableRequested = false

    fun canAccessLocation(): Boolean {
        return hasLocationPermission() && isLocationServiceEnabled()
    }

    private fun canReadScanResults(): Boolean {
        return wifiManager.isWifiEnabled && (Build.VERSION.SDK_INT < 23 || canAccessLocation())
    }

    fun canReadWifiInfo(): Boolean {
        return Build.VERSION.SDK_INT < 26 || hasLocationPermission() && isLocationServiceEnabled()
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
    suspend fun connectToNetwork(stoppable: Stoppable, description: NetworkDescription, pin: Int) {
        if (Build.VERSION.SDK_INT >= 29) {
            val suggestionList: MutableList<WifiNetworkSuggestion> = ArrayList()
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

        // TODO: 3/20/21 This should establish an actual connection.
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
    suspend fun establishHotspotConnection(description: NetworkDescription): InetAddress {
        val timeout = (System.nanoTime() + AppConfig.DEFAULT_TIMEOUT_HOTSPOT * 1e6).toLong()
        var toggled = Build.VERSION.SDK_INT >= 29
        var connectionReset = false

        while (true) {
            val timedOut = System.nanoTime() > timeout
            val wifiDhcpInfo: DhcpInfo = wifiManager.dhcpInfo

            Log.d(TAG, "establishHotspotConnection: Waiting for the network. DhcpInfo: $wifiDhcpInfo")

            if (!wifiManager.isWifiEnabled) {
                Log.d(TAG, "establishHotspotConnection: Wifi is off. Making a request to turn it on.")

                if (Build.VERSION.SDK_INT >= 29 || !wifiManager.setWifiEnabled(true)) {
                    Log.d(TAG, "establishHotspotConnection: Wifi was off. The request has failed. Exiting.")
                    throw WifiInaccessibleException("The device is running Android 10 or Wi-Fi is inaccessible.")
                }
            } else if (!toggled && !isConnectedToNetwork(description)) {
                Log.d(TAG, "establishHotspotConnection: The network is not ready yet.")
                wifiManager.startScan()

                val result = findFromScanResults(description)

                if (!connectionReset && isConnectedToAnyNetwork()) {
                    disableCurrentNetwork()
                    wifiManager.isWifiEnabled = false
                    connectionReset = true
                } else if (result == null) {
                    Log.e(TAG, "establishHotspotConnection: No network found? " + description.ssid)
                } else if (startConnection(createWifiConfig(result, description.password)).also { toggled = it }) {
                    Log.d(TAG, "establishHotspotConnection: Toggled network using results " + description.ssid)
                } else {
                    Log.d(TAG, "establishHotspotConnection: Failed to toggle network? " + description.ssid)
                }
            } else if (wifiDhcpInfo.gateway != 0) {
                try {
                    return withContext(Dispatchers.IO) {
                        InetAddresses.from(wifiDhcpInfo.gateway)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "establishHotspotConnection: Connection failed.", e)

                    if (timedOut) {
                        throw e
                    }
                }
            } else {
                Log.d(TAG, "establishHotspotConnection: No DHCP provided or connection not ready. Looping...")
            }

            delay(1000)

            if (timedOut) {
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

    fun hasLocationPermission(): Boolean {
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

    fun isConnectedToNetwork(description: NetworkDescription): Boolean {
        return isConnectedToNetwork(description.ssid, description.bssid)
    }

    private fun isConnectedToNetwork(configuration: WifiConfiguration): Boolean {
        return isConnectedToNetwork(configuration.SSID, configuration.BSSID)
    }

    private fun isConnectedToNetwork(ssid: String, bssid: String?): Boolean {
        if (!isConnectedToAnyNetwork()) return false
        val wifiInfo: WifiInfo = wifiManager.connectionInfo
        val tgSsid = getCleanSsid(wifiInfo.ssid)
        Log.d(TAG, "isConnectedToNetwork: " + ssid + "=" + tgSsid + "," + bssid + "=" + wifiInfo.bssid)
        return bssid?.equals(wifiInfo.bssid, ignoreCase = true) ?: (ssid == tgSsid)
    }

    fun isLocationServiceEnabled(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        this.locationManager.isLocationEnabled
    } else {
        this.locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @Deprecated("Do not use this method above 9, there is a better method in-place.")
    fun isMobileDataActive(): Boolean {
        return connectivityManager.activeNetworkInfo?.type == ConnectivityManager.TYPE_MOBILE
    }

    fun notifyWirelessRequestHandled(): Boolean {
        val returnedState = wirelessEnableRequested
        wirelessEnableRequested = false
        return returnedState
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
    @RequiresPermission(allOf = [ACCESS_FINE_LOCATION, ACCESS_WIFI_STATE])
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

    fun toggleHotspot(
        backend: BackgroundBackend,
        provider: SnackbarPlacementProvider,
        manager: HotspotManager,
        suggestActions: Boolean,
        permissionsResultLauncher: ActivityResultLauncher<Array<String>>,
    ) {
        if (!HotspotManager.supported || Build.VERSION.SDK_INT >= 26
            && !validateLocationAccess(provider, permissionsResultLauncher)
        ) return

        // Android introduced permissions in 23 and this permission is not needed for local only hotspot introduced
        // in 26
        if (Build.VERSION.SDK_INT in 23..25 && !Settings.System.canWrite(context)) {
            provider.createSnackbar(R.string.mesg_errorHotspotPermission)?.apply {
                setAction(R.string.butn_settings) {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                            .setData(Uri.parse("package:" + context.packageName))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                show()
            }
        } else if (Build.VERSION.SDK_INT < 26 && !manager.enabled && isMobileDataActive() && suggestActions) {
            provider.createSnackbar(R.string.mesg_warningHotspotMobileActive)?.apply {
                setAction(R.string.butn_skip) {
                    toggleHotspot(backend, provider, manager, false, permissionsResultLauncher)
                }
                show()
            }
        } else {
            val config: WifiConfiguration? = manager.configuration
            val state = if (manager.enabled) R.string.mesg_stoppingSelfHotspot else R.string.mesg_startingSelfHotspot

            if (!manager.enabled || config != null) {
                provider.createSnackbar(state)?.show()
            }

            backend.toggleHotspot()
        }
    }

    fun turnOnWiFi(provider: SnackbarPlacementProvider) {
        val startSettings = {
            context.startActivity(
                Intent(Settings.ACTION_WIFI_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }

        when {
            Build.VERSION.SDK_INT >= 29 -> startSettings()
            wifiManager.setWifiEnabled(true) -> provider.createSnackbar(R.string.mesg_turningWiFiOn)?.show()
            else -> provider.createSnackbar(R.string.mesg_wifiEnableFailed)?.apply {
                setAction(R.string.butn_settings) {
                    startSettings()
                }
                show()
            }
        }
    }

    fun validateLocationAccess(
        provider: SnackbarPlacementProvider,
        permissionsResultLauncher: ActivityResultLauncher<Array<String>>,
    ): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true

        if (!hasLocationPermission()) {
            provider.createSnackbar(R.string.mesg_locationPermissionRequiredAny)?.apply {
                setAction(R.string.butn_allow) {
                    permissionsResultLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
                }
                show()
            }
        } else if (!isLocationServiceEnabled()) {
            provider.createSnackbar(R.string.mesg_locationServiceDisabled)?.apply {
                setAction(R.string.butn_locationSettings) {
                    Activities.startLocationServiceSettings(context)
                }
                show()
            }
        } else {
            return true
        }

        return false
    }

    fun validateLocationAccessNoPrompt(permissionsResultLauncher: ActivityResultLauncher<Array<String>>): Boolean {
        if (Build.VERSION.SDK_INT < 23) return true

        if (!hasLocationPermission()) {
            permissionsResultLauncher.launch(arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION))
        } else if (!isLocationServiceEnabled()) {
            context.startActivity(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } else {
            return true
        }

        return false
    }

    class WifiInaccessibleException(message: String?) : Exception(message)

    companion object {
        private val TAG = Connections::class.simpleName

        fun getCleanSsid(ssid: String?): String {
            return ssid?.trim()?.replace("\"", "") ?: ""
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
    }
}