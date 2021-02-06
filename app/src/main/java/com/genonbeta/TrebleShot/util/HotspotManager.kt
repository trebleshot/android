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
package com.genonbeta.TrebleShot.util

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiManager.LocalOnlyHotspotCallback
import android.net.wifi.WifiManager.LocalOnlyHotspotReservation
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresApi
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

abstract class HotspotManager internal constructor(context: Context) {
    abstract val configuration: WifiConfiguration?

    abstract fun disable(): Boolean

    abstract fun enable(): Boolean

    abstract fun enableConfigured(apName: String, passKeyWPA2: String?): Boolean

    abstract val isEnabled: Boolean

    abstract val isStarted: Boolean

    abstract val previousConfig: WifiConfiguration?

    var secondaryCallback: LocalOnlyHotspotCallback? = null

    abstract fun unloadPreviousConfig(): Boolean

    val wifiManager: WifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    @RequiresApi(26)
    class OreoHotspotManager(context: Context) : HotspotManager(context) {
        override val configuration: WifiConfiguration?
            get() = hotspotReservation?.getWifiConfiguration()

        override fun disable(): Boolean {
            if (hotspotReservation == null)
                return false

            hotspotReservation?.close()
            hotspotReservation = null

            return true
        }

        override fun enable(): Boolean {
            try {
                wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: LocalOnlyHotspotReservation) {
                        super.onStarted(reservation)
                        hotspotReservation = reservation
                        secondaryCallback?.onStarted(reservation)
                    }

                    override fun onStopped() {
                        super.onStopped()
                        hotspotReservation = null
                        secondaryCallback?.onStopped()
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        hotspotReservation = null
                        secondaryCallback?.onFailed(reason)
                    }
                }, Handler(Looper.getMainLooper()))
                return true
            } catch (ignored: Throwable) {
            }
            return false
        }

        private var hotspotReservation: LocalOnlyHotspotReservation? = null

        override fun enableConfigured(apName: String, passKeyWPA2: String?): Boolean {
            return enable()
        }

        override val isEnabled: Boolean
            get() = OldHotspotManager.enabled(wifiManager)

        override val isStarted: Boolean
            get() = hotspotReservation != null

        override val previousConfig: WifiConfiguration?
            get() = configuration

        override fun unloadPreviousConfig(): Boolean {
            return hotspotReservation != null
        }
    }

    private class OldHotspotManager(context: Context) : HotspotManager(context) {
        companion object {
            private var getWifiApConfiguration: Method? = null
            private var getWifiApState: Method? = null
            private var isWifiApEnabled: Method? = null
            private var setWifiApEnabled: Method? = null
            private var setWifiApConfiguration: Method? = null
            fun enabled(wifiManager: WifiManager): Boolean {
                val result = invokeSilently(isWifiApEnabled, wifiManager) ?: return false
                return result as Boolean
            }

            private fun invokeSilently(method: Method?, receiver: Any, vararg args: Any): Any? {
                try {
                    return method?.invoke(receiver, *args)
                } catch (e: IllegalAccessException) {
                    Log.e(TAG, "exception in invoking methods: " + method?.name + "(): " + e.message)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "exception in invoking methods: " + method?.name + "(): " + e.message)
                } catch (e: InvocationTargetException) {
                    Log.e(TAG, "exception in invoking methods: " + method?.name + "(): " + e.message)
                }
                return null
            }

            fun supported(): Boolean {
                return getWifiApState != null && isWifiApEnabled != null && setWifiApEnabled != null
                        && getWifiApConfiguration != null
            }

            init {
                for (method in WifiManager::class.java.declaredMethods) {
                    when (method.name) {
                        "getWifiApConfiguration" -> getWifiApConfiguration = method
                        "getWifiApState" -> getWifiApState = method
                        "isWifiApEnabled" -> isWifiApEnabled = method
                        "setWifiApEnabled" -> setWifiApEnabled = method
                        "setWifiApConfiguration" -> setWifiApConfiguration = method
                    }
                }
            }
        }

        override fun disable(): Boolean {
            unloadPreviousConfig()
            return setHotspotEnabled(previousConfigPrivate, false)
        }

        override fun enable(): Boolean {
            wifiManager.setWifiEnabled(false)
            return setHotspotEnabled(configuration, true)
        }

        override fun enableConfigured(apName: String, passKeyWPA2: String?): Boolean {
            wifiManager.setWifiEnabled(false)

            if (previousConfigPrivate == null)
                previousConfigPrivate = configuration

            val wifiConfiguration = WifiConfiguration().apply {
                SSID = apName

                if (passKeyWPA2 != null && passKeyWPA2.length >= 8) {
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                    allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                    preSharedKey = passKeyWPA2
                } else
                    allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            }

            return setHotspotEnabled(wifiConfiguration, true)
        }

        override val isEnabled: Boolean
            get() = enabled(wifiManager)

        override val isStarted: Boolean
            get() = previousConfig != null

        override val configuration: WifiConfiguration?
            get() = invokeSilently(getWifiApConfiguration, wifiManager) as WifiConfiguration?

        override val previousConfig: WifiConfiguration?
            get() = previousConfigPrivate

        private var previousConfigPrivate: WifiConfiguration? = null

        private fun setHotspotConfig(config: WifiConfiguration): Boolean {
            val result = invokeSilently(setWifiApConfiguration, wifiManager, config) ?: return false
            return result as Boolean
        }

        private fun setHotspotEnabled(config: WifiConfiguration?, enabled: Boolean): Boolean {
            val result = invokeSilently(setWifiApEnabled, wifiManager, config, enabled) ?: return false
            return result as Boolean
        }

        override fun unloadPreviousConfig(): Boolean = previousConfig?.let {
            previousConfigPrivate = null
            return@let setHotspotConfig(it)
        } ?: false
    }

    companion object {
        private const val TAG = "HotspotUtils"
        val isSupported: Boolean
            get() = Build.VERSION.SDK_INT >= 26 || OldHotspotManager.supported()

        fun newInstance(context: Context): HotspotManager {
            return if (Build.VERSION.SDK_INT >= 26) OreoHotspotManager(context) else OldHotspotManager(context)
        }
    }

}