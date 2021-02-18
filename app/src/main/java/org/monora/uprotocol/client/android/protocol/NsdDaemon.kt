package org.monora.uprotocol.client.android.protocol

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import org.monora.uprotocol.core.ClientLoader
import org.monora.uprotocol.core.spec.v1.Config

@RequiresApi(Build.VERSION_CODES.JELLY_BEAN)
class NsdDaemon(
    val context: Context,
    val connectionFactory: DefaultConnectionFactory,
    val persistenceProvider: DefaultPersistenceProvider,
) {
    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private var discoveryListener: DiscoveryListener? = DiscoveryListener()

    private var registrationListener: RegistrationListener? = RegistrationListener()

    fun registerService() {
        val localServiceInfo = NsdServiceInfo()
        localServiceInfo.serviceName = persistenceProvider.clientNickname
        localServiceInfo.serviceType = SERVICE
        localServiceInfo.port = Config.PORT_UPROTOCOL
        try {
            nsdManager.registerService(localServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun startDiscovering() {
        try {
            nsdManager.discoverServices(SERVICE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (ignored: Exception) {
        }
    }

    fun stopDiscovering() {
        try {
            if (discoveryListener != null)
                nsdManager.stopServiceDiscovery(discoveryListener)
        } catch (ignored: Exception) {
        }
    }

    fun unregisterService() {
        try {
            if (registrationListener != null)
                nsdManager.unregisterService(registrationListener)
        } catch (ignored: Exception) {
        }
    }

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
            Log.v(TAG, "'" + serviceInfo.serviceName + "' service has been discovered.")
            nsdManager.resolveService(serviceInfo, ResolveListener())
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "'" + serviceInfo.serviceName + "' service is now lost.")
        }

        fun clear() {
            discoveryListener = null
        }
    }

    private inner class ResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Could not resolve " + serviceInfo.serviceName)
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.v(TAG, "Resolved service " + serviceInfo.serviceName + " on " + serviceInfo.host.hostAddress)

            val thread = Thread {
                try {
                    val client = ClientLoader.load(connectionFactory, persistenceProvider, serviceInfo.host)
                    Log.v(TAG, "Resolved client " + client.clientNickname + " (" + client.clientUid + ")")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            thread.start()
        }
    }

    companion object {
        val TAG = NsdDaemon::class.simpleName
        const val SERVICE = "_tscomm._tcp."
    }
}
