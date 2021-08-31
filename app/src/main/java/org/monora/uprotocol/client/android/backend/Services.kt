/*
 * Copyright (C) 2021 Veli Tasalı
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
package org.monora.uprotocol.client.android.backend

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import com.yanzhenjie.andserver.Server
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.WebDataRepository
import org.monora.uprotocol.client.android.di.WebShareServer
import org.monora.uprotocol.client.android.util.HotspotManager
import org.monora.uprotocol.client.android.util.NotificationBackend
import org.monora.uprotocol.client.android.util.Notifications
import org.monora.uprotocol.client.android.util.NsdDaemon
import org.monora.uprotocol.client.android.util.Permissions
import org.monora.uprotocol.core.TransportSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Services @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nsdDaemon: NsdDaemon,
    private val transportSession: TransportSession,
    private val webDataRepository: WebDataRepository,
    @WebShareServer private val webShareServer: Server,
) {
    val hotspotManager = HotspotManager.newInstance(context)

    val isServingAnything
        get() = webDataRepository.isServing

    private val mediaScannerConnectionClient = MediaScannerConnectionClient()

    val mediaScannerConnection = MediaScannerConnection(context, mediaScannerConnectionClient)

    val notifications = Notifications(NotificationBackend(context))

    private var wifiLock: WifiManager.WifiLock? = null

    private val wifiManager: WifiManager
        get() = hotspotManager.wifiManager

    fun start() {
        val webServerRunning = webShareServer.isRunning
        val commServerRunning = transportSession.isListening

        if (!mediaScannerConnection.isConnected) {
            mediaScannerConnection.connect()
        }

        if (webServerRunning && commServerRunning) {
            Log.d(TAG, "start: Services are already up")
            return
        }

        try {
            if (!Permissions.checkRunningConditions(context)) throw Exception(
                "The app doesn't have the satisfactory permissions to start the services."
            )

            wifiLock = wifiManager.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, TAG).also {
                it.acquire()
            }

            if (!commServerRunning) {
                transportSession.start()
            }

            if (!webServerRunning) {
                webShareServer.startup()
            }

            nsdDaemon.registerService()
            nsdDaemon.startDiscovering()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        transportSession.runCatching {
            stop()
        }

        if (mediaScannerConnection.isConnected) {
            mediaScannerConnection.disconnect()
        }

        wifiLock?.takeIf { it.isHeld }?.let {
            it.release()
            wifiLock = null

            Log.d(TAG, "onDestroy: Releasing Wi-Fi lock")
        }

        nsdDaemon.unregisterService()
        nsdDaemon.stopDiscovering()
        webShareServer.shutdown()

        if (hotspotManager.unloadPreviousConfig()) {
            Log.d(TAG, "onDestroy: Stopping hotspot (previously started)=" + hotspotManager.disable())
        }
    }

    fun toggleHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Build.VERSION.SDK_INT < Build.VERSION_CODES.O
            && !Settings.System.canWrite(context)
        ) return

        if (hotspotManager.enabled) {
            hotspotManager.disable()
        } else {
            val result = hotspotManager.enableConfigured(context.getString(R.string.app_name), null)
            Log.d(TAG, "toggleHotspot: Enabling=$result")
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private inner class SecondaryHotspotCallback : WifiManager.LocalOnlyHotspotCallback() {
        override fun onStarted(reservation: WifiManager.LocalOnlyHotspotReservation) {
            super.onStarted(reservation)
            context.sendBroadcast(
                Intent(ACTION_OREO_HOTSPOT_STARTED).putExtra(EXTRA_HOTSPOT_CONFIG, reservation.wifiConfiguration)
            )
        }
    }

    private class MediaScannerConnectionClient : MediaScannerConnection.MediaScannerConnectionClient {
        override fun onScanCompleted(path: String?, uri: Uri?) {
            Log.d(TAG, "onScanCompleted: $path")
        }

        override fun onMediaScannerConnected() {
            Log.d(TAG, "onMediaScannerConnected: Service connected")
        }
    }

    companion object {
        private const val TAG = "Services"

        const val ACTION_OREO_HOTSPOT_STARTED = "org.monora.trebleshot.intent.action.HOTSPOT_STARTED"

        const val EXTRA_HOTSPOT_CONFIG = "hotspotConfig"
    }

    init {
        if (Build.VERSION.SDK_INT >= 26) {
            hotspotManager.secondaryCallback = SecondaryHotspotCallback()
        }
    }
}
