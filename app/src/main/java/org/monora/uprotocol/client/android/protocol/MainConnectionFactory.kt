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

package org.monora.uprotocol.client.android.protocol

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import org.monora.coolsocket.core.session.ActiveConnection
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.core.CommunicationBridge
import org.monora.uprotocol.core.CommunicationBridge.openConnection
import org.monora.uprotocol.core.protocol.ConnectionFactory
import java.io.IOException
import java.net.InetAddress
import javax.inject.Inject


class MainConnectionFactory @Inject constructor(
    @ApplicationContext private val context: Context,
) : ConnectionFactory {
    override fun enableCipherSuites(
        supportedCipherSuites: Array<out String>,
        enabledCipherSuiteList: MutableList<String>,
    ) {
        if (Build.VERSION.SDK_INT >= 20) {
            enabledCipherSuiteList.add("TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384")
            enabledCipherSuiteList.add("TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256")
        }
    }

    override fun openConnection(address: InetAddress): ActiveConnection {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = NetworkBinderCallback(manager, address)
            val builder = NetworkRequest.Builder()

            builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            builder.addTransportType(NetworkCapabilities.TRANSPORT_ETHERNET)
            manager.requestNetwork(builder.build(), callback)

            return callback.waitForConnection()
        }
        return CommunicationBridge.openConnection(address)
    }
}

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
private class NetworkBinderCallback(
    val connectivityManager: ConnectivityManager, val inetAddress: InetAddress,
) : ConnectivityManager.NetworkCallback() {
    private val lock = Object()

    private var bound = false

    private var exception: IOException? = null

    private var resultConnection: ActiveConnection? = null

    override fun onAvailable(network: Network) {
        super.onAvailable(network)
        if (!bindNetwork(network)) {
            Log.d(TAG, "onAvailable: Failed to bind network $network.")
            return
        }

        bound = true

        try {
            resultConnection = openConnection(inetAddress)
        } catch (e: Exception) {
            exception = IOException(e)
        } finally {
            synchronized(lock) { lock.notifyAll() }
            connectivityManager.unregisterNetworkCallback(this)
            bindNetwork(null)
        }
    }

    fun bindNetwork(network: Network?): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            connectivityManager.bindProcessToNetwork(network)
        } else {
            ConnectivityManager.setProcessDefaultNetwork(network)
        }
    }

    @Throws(IOException::class)
    fun waitForConnection(): ActiveConnection {
        try {
            synchronized(lock) { lock.wait(AppConfig.DEFAULT_TIMEOUT_SOCKET.toLong()) }
        } catch (e: InterruptedException) {
            exception = IOException(e)
        }

        resultConnection?.let { return it }
        exception?.let { throw it }

        if (!bound) {
            Log.d(TAG, "waitForConnection: The network was not available, trying without binding.")
            return openConnection(inetAddress)
        }

        throw IOException("No connection is handed over after waiting.")
    }

    companion object {
        private const val TAG = "MainConnectionFactory"
    }
}
