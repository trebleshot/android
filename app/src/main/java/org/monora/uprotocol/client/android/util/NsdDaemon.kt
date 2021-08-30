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
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.collection.ArrayMap
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.backend.Backend
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.database.model.UClientAddress
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.core.ClientLoader
import org.monora.uprotocol.core.persistence.PersistenceProvider
import org.monora.uprotocol.core.protocol.ConnectionFactory
import org.monora.uprotocol.core.protocol.communication.CredentialsException
import org.monora.uprotocol.core.spec.v1.Config
import java.net.NetworkInterface
import javax.inject.Inject
import javax.inject.Singleton

/**
 * created by: Veli
 * date: 22.01.2018 15:35
 */
@Singleton
class NsdDaemon @Inject constructor(
    @ApplicationContext context: Context,
    val persistenceProvider: PersistenceProvider,
    val connectionFactory: ConnectionFactory,
    val backend: Backend,
) {
    private val onlineClientMap = ArrayMap<String, ClientRoute>()

    private val _onlineClients: MutableLiveData<List<ClientRoute>> = MutableLiveData()

    val onlineClients: LiveData<List<ClientRoute>> = _onlineClients

    private val nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private var registrationListener: NsdManager.RegistrationListener? = null

    fun registerService() {
        if (registrationListener != null) {
            return
        }

        val registrationListener = RegistrationListener().also { registrationListener = it }
        val localServiceInfo = NsdServiceInfo().apply {
            serviceName = persistenceProvider.clientNickname
            serviceType = Config.SERVICE_UPROTOCOL_DNS_SD
            port = Config.PORT_UPROTOCOL
        }

        try {
            nsdManager.registerService(localServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDiscovering() {
        if (discoveryListener != null) {
            return
        }

        try {
            val discoveryListener = DiscoveryListener().also { discoveryListener = it }
            nsdManager.discoverServices(Config.SERVICE_UPROTOCOL_DNS_SD, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (ignored: Exception) {
        }
    }

    fun stopDiscovering() {
        if (discoveryListener == null) {
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
        if (registrationListener == null) {
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
            Log.v(TAG, "Self service is now registered: " + serviceInfo.serviceName)
        }

        override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "Self service is unregistered: " + serviceInfo.serviceName)
            clear()
        }

        fun clear() {
            registrationListener = null
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private inner class DiscoveryListener : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "NSD failed to start with error code $errorCode")
            clear()
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "NSD failed to stop with error code $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) {
            Log.v(TAG, "NSD started")
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.v(TAG, "NSD stopped")
            clear()
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "'" + serviceInfo.serviceName + "' service has been discovered.")
            if (serviceInfo.serviceType == Config.SERVICE_UPROTOCOL_DNS_SD) {
                nsdManager.resolveService(serviceInfo, ResolveListener())
            }
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "'" + serviceInfo.serviceName + "' service is now lost.")

            synchronized(onlineClientMap) {
                onlineClientMap.remove(serviceInfo.serviceName)
                _onlineClients.postValue(onlineClientMap.values.toList())
            }
        }

        fun clear() {
            discoveryListener = null
            synchronized(onlineClientMap) { onlineClientMap.clear() }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private inner class ResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Could not resolve '${serviceInfo.serviceName}' with error code $errorCode")
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            with(serviceInfo) {
                if (host.isLoopbackAddress || host.isAnyLocalAddress
                    || NetworkInterface.getByInetAddress(host) != null
                ) {
                    Log.d(TAG, "onServiceResolved: Resolved LOCAL '$serviceName' on '$host' (skipping)")
                    return
                }

                Log.v(TAG, "Resolved '$serviceName' on '$host'")
            }

            backend.applicationScope.launch(Dispatchers.IO) {
                try {
                    val client = ClientLoader.load(connectionFactory, persistenceProvider, serviceInfo.host)

                    if (client !is UClient) {
                        Log.d(TAG, "onServiceResolved: Not a " + UClient::class.simpleName + " derivative.")
                        return@launch
                    }

                    val address = UClientAddress(serviceInfo.host, client.clientUid)

                    synchronized(onlineClientMap) {
                        onlineClientMap[serviceInfo.serviceName] = ClientRoute(client, address)
                        onlineClientMap.values.toMutableList().also { clients ->
                            clients.sortByDescending { it.client.lastUsageTime }
                            _onlineClients.postValue(clients)
                        }
                    }
                } catch (e: CredentialsException) {
                    val client = e.client
                    if (e.firstTime && client is UClient) {
                        backend.services.notifications.notifyClientCredentialsChanged(client)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    companion object {
        private const val TAG = "NsdDaemon"
    }
}
