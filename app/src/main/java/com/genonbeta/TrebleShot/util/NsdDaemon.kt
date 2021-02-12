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

import android.content.*
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.dataobject.DeviceRoute

/**
 * created by: Veli
 * date: 22.01.2018 15:35
 */
class NsdDaemon(val context: Context, val database: Kuick, private val mPreferences: SharedPreferences) {
    private var mNsdManager: NsdManager? = null
    private var mNsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var mNsdRegistrationListener: NsdManager.RegistrationListener? = null
    private val onlineDeviceList: MutableMap<String, DeviceRoute> = ArrayMap()

    @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private val discoveryListener: NsdManager.DiscoveryListener?
        get() {
            if (mNsdDiscoveryListener == null) mNsdDiscoveryListener = DiscoveryListener()
            return mNsdDiscoveryListener
        }

    @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private val nsdManager: NsdManager
        get() {
            return mNsdManager ?: (context.getSystemService(Context.NSD_SERVICE) as NsdManager).also {
                mNsdManager = it
            }
        }

    @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private val registrationListener: NsdManager.RegistrationListener?
        get() {
            if (mNsdRegistrationListener == null) mNsdRegistrationListener = RegistrationListener()
            return mNsdRegistrationListener
        }

    fun isDeviceOnline(device: Device): Boolean {
        synchronized(onlineDeviceList) {
            for (deviceRoute in onlineDeviceList.values) if (deviceRoute.device.equals(device)) return true
        }
        return false
    }

    val isDiscovering: Boolean
        get() = mNsdDiscoveryListener != null

    val isServiceEnabled: Boolean
        get() = mPreferences.getBoolean("nsd_enabled", false)

    fun registerService() {
        if (isServiceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            val localServiceInfo = NsdServiceInfo()
            localServiceInfo.serviceName = AppUtils.getLocalDeviceName(context)
            localServiceInfo.serviceType = AppConfig.NSD_SERVICE_TYPE
            localServiceInfo.port = AppConfig.SERVER_PORT_COMMUNICATION
            try {
                nsdManager.registerService(localServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startDiscovering() {
        try {
            if (isServiceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                nsdManager.discoverServices(AppConfig.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
            }
        } catch (ignored: Exception) {
        }
    }

    fun stopDiscovering() {
        try {
            if (mNsdDiscoveryListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                nsdManager.stopServiceDiscovery(mNsdDiscoveryListener)
            }
        } catch (ignored: Exception) {
        }
    }

    fun unregisterService() {
        try {
            if (mNsdRegistrationListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                nsdManager.unregisterService(mNsdRegistrationListener)
            }
        } catch (ignored: Exception) {
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private inner class RegistrationListener : NsdManager.RegistrationListener {
        override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Failed to register self service with error code $errorCode")
            clear()
        }

        override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Failed to unregister self service with error code $errorCode")
        }

        override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "Self service is now registered: " + serviceInfo.getServiceName())
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "Self service is unregistered: " + serviceInfo.getServiceName())
            clear()
        }

        fun clear() {
            mNsdRegistrationListener = null
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private inner class DiscoveryListener : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "NSD discovery failed to start with error code $errorCode")
            clear()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "NSD discovery failed to stop with error code $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) {
            Log.v(TAG, "NSD discovery started")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.v(TAG, "NSD discovery stopped")
            clear()
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "'" + serviceInfo.getServiceName() + "' service has been discovered.")
            if (serviceInfo.serviceType == AppConfig.NSD_SERVICE_TYPE) nsdManager.resolveService(
                serviceInfo, ResolveListener()
            )
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "'" + serviceInfo.serviceName + "' service is now lost.")
            synchronized(onlineDeviceList) {
                if (onlineDeviceList.remove(serviceInfo.serviceName) != null) {
                    context.sendBroadcast(Intent(ACTION_DEVICE_STATUS))
                }
            }
        }

        fun clear() {
            mNsdDiscoveryListener = null
            synchronized(onlineDeviceList) { onlineDeviceList.clear() }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private inner class ResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Could not resolve " + serviceInfo.getServiceName())
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.v(
                TAG, "Resolved " + serviceInfo.serviceName + " with success has the IP address of "
                        + serviceInfo.host.hostAddress
            )
            DeviceLoader.load(
                database,
                serviceInfo.host,
                object : DeviceLoader.OnDeviceResolvedListener {
                    override fun onDeviceResolved(device: Device, address: DeviceAddress) {
                        synchronized(onlineDeviceList) {
                            onlineDeviceList.put(
                                serviceInfo.getServiceName(),
                                DeviceRoute(device, address)
                            )
                        }
                        context.sendBroadcast(Intent(ACTION_DEVICE_STATUS))
                    }
                }
            )
        }
    }

    companion object {
        val TAG = NsdDaemon::class.java.simpleName
        const val ACTION_DEVICE_STATUS = "org.monora.trebleshot.android.intent.action.DEVICE_STATUS"
    }
}