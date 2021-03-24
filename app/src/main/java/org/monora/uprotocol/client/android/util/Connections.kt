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
import androidx.annotation.RequiresApi
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

    fun isConnectedToAnyNetwork(): Boolean {
        val info: NetworkInfo? = this.connectivityManager.activeNetworkInfo
        return info != null && info.type == ConnectivityManager.TYPE_WIFI && info.isConnected
    }

    fun isConnectedToNetwork(description: NetworkDescription): Boolean {
        return isConnectedToNetwork(description.ssid, description.bssid)
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
        @RequiresApi(Build.VERSION_CODES.M)
        if (Build.VERSION.SDK_INT in 23..25 && !Settings.System.canWrite(context)) {
            provider.createSnackbar(R.string.mesg_needsSettingsWritePermission)?.apply {
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

    private fun validateLocationAccess(
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