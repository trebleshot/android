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

import android.content.Context
import android.content.Intent
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import androidx.preference.PreferenceManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.core.ClientLoader
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * created by: Veli
 * date: 22.01.2018 15:35
 */
@Singleton
class NsdDaemon @Inject constructor(
    @ApplicationContext val context: Context,
    val appDatabase: AppDatabase,
    val persistenceProvider: PersistenceProvider,
    val connectionFactory: ConnectionFactory
) {
    private val nsdManager: NsdManager by lazy {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
            throw UnsupportedOperationException("This field shouldn't have been invoked on this version of OS");
        }

        context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    // TODO: 2/25/21 Can this become a live data
    private val onlineClientList: MutableMap<String, ClientRoute> = ArrayMap()

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var registrationListener: NsdManager.RegistrationListener? = null

    val discovering: Boolean
        get() = discoveryListener != null

    val enabled: Boolean
        get() = preferences.getBoolean("nsd_enabled", false)

    fun isDeviceOnline(client: UClient): Boolean {
        synchronized(onlineClientList) {
            for (deviceRoute in onlineClientList.values) if (deviceRoute.device == client) return true
        }
        return false
    }

    fun registerService() {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || registrationListener != null) {
            return
        }

        val registrationListener = RegistrationListener().also { registrationListener = it }
        val localServiceInfo = NsdServiceInfo()
        localServiceInfo.serviceName = persistenceProvider.clientNickname
        localServiceInfo.serviceType = AppConfig.NSD_SERVICE_TYPE
        localServiceInfo.port = AppConfig.SERVER_PORT_COMMUNICATION
        try {
            nsdManager.registerService(localServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDiscovering() {
        if (!enabled || Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || discoveryListener != null) {
            return
        }

        try {
            val discoveryListener = DiscoveryListener().also { discoveryListener = it }
            nsdManager.discoverServices(AppConfig.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (ignored: Exception) {
        }
    }

    fun stopDiscovering() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || discoveryListener == null) {
            return
        }

        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (ignored: Exception) {
        } finally {
            discoveryListener = null
        }
    }

    fun unregisterService() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN || registrationListener == null) {
            return
        }

        try {
            nsdManager.unregisterService(registrationListener)
        } catch (ignored: Exception) {
        } finally {
            registrationListener = null
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
            registrationListener = null
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
            synchronized(onlineClientList) {
                onlineClientList.remove(serviceInfo.serviceName)
            }
        }

        fun clear() {
            discoveryListener = null
            synchronized(onlineClientList) { onlineClientList.clear() }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private inner class ResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Could not resolve " + serviceInfo.serviceName)
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            with (serviceInfo) {
                Log.v(TAG, "Resolved '$serviceName' on '${host.hostAddress}'")
            }

            CoroutineScope(Dispatchers.Main).launch(Dispatchers.IO) {
                try {
                    val client = ClientLoader.load(
                        connectionFactory, persistenceProvider, serviceInfo.host
                    )

                    if (client is UClient) {
                        val address = UClientAddress(serviceInfo.host, client.clientUid)

                        synchronized(onlineClientList) {
                            onlineClientList.put(serviceInfo.serviceName, ClientRoute(client, address))
                        }
                    } else {
                        Log.d(TAG, "onServiceResolved: Not a " + UClient::class.simpleName + " derivative.")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        val TAG = NsdDaemon::class.simpleName
    }
}