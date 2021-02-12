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
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.*
import android.util.Log
import androidx.annotation.RequiresApi
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.DeviceAddress
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.protocol.DeviceBlockedException
import com.genonbeta.TrebleShot.protocol.DeviceVerificationException
import com.genonbeta.TrebleShot.protocol.communication.*
import com.genonbeta.android.database.exception.ReconstructionFailedException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.monora.coolsocket.core.session.ActiveConnection
import java.io.Closeable
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException

/**
 * created by: Veli
 * date: 11.02.2018 15:07
 */
class CommunicationBridge(
    val kuick: Kuick,
    val activeConnection: ActiveConnection,
    var device: Device,
    var deviceAddress: DeviceAddress,
) : Closeable {
    override fun close() {
        try {
            activeConnection.closeSafely()
        } catch (ignored: Exception) {
        }
    }

    val context: Context
        get() = kuick.context

    @Throws(JSONException::class, IOException::class)
    fun requestAcquaintance() {
        activeConnection.reply(JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE))
    }

    @Throws(JSONException::class, IOException::class)
    fun requestFileTransfer(transferId: Long, files: JSONArray?) {
        activeConnection.reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.INDEX, files)
        )
    }

    @Throws(JSONException::class, IOException::class)
    fun requestFileTransferStart(transferId: Long, type: TransferItem.Type?) {
        activeConnection.reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_TYPE, type)
        )
    }

    @Throws(JSONException::class, IOException::class)
    fun requestNotifyTransferState(transferId: Long, accepted: Boolean) {
        activeConnection.reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_NOTIFY_TRANSFER_STATE)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_IS_ACCEPTED, accepted)
        )
    }

    @Throws(JSONException::class, IOException::class)
    fun requestTextTransfer(text: String) {
        activeConnection.reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD)
                .put(Keyword.TRANSFER_TEXT, text)
        )
    }

    @Throws(IOException::class, JSONException::class)
    fun sendError(errorCode: String) {
        sendError(activeConnection, errorCode)
    }

    @Throws(IOException::class, JSONException::class)
    fun sendResult(result: Boolean) {
        sendResult(activeConnection, result)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class NetworkBinderCallback(val connectivityManager: ConnectivityManager, val inetAddress: InetAddress) :
        ConnectivityManager.NetworkCallback() {
        private val lock = Object()
        private var exception: IOException? = null
        private var resultConnection: ActiveConnection? = null

        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            if (!bindNetwork(network)) {
                Log.d(TAG, "onAvailable: Failed to bind network $network")
                return
            }
            try {
                resultConnection = openConnection(inetAddress)
            } catch (e: IOException) {
                exception = e
            } catch (e: Exception) {
                e.printStackTrace()
                exception = IOException(e)
            } finally {
                synchronized(lock) { lock.notifyAll() }
                connectivityManager.unregisterNetworkCallback(this)
                bindNetwork(null)
            }
        }

        override fun onUnavailable() {
            super.onUnavailable()
            Log.d(
                TAG, "onUnavailable: No network was available for the requested network type. Opening by the " +
                        "default network"
            )
            try {
                resultConnection = openConnection(inetAddress)
            } catch (e: IOException) {
                exception = e
            } finally {
                synchronized(lock) { lock.notifyAll() }
            }
        }

        fun bindNetwork(network: Network?): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) connectivityManager.bindProcessToNetwork(
                network
            ) else ConnectivityManager.setProcessDefaultNetwork(
                network
            )
        }

        @Throws(IOException::class)
        fun waitForConnection(): ActiveConnection {
            try {
                synchronized(lock) { lock.wait(AppConfig.DEFAULT_TIMEOUT_SOCKET.toLong()) }
            } catch (e: InterruptedException) {
                e.printStackTrace()
                exception = IOException(e)
            }

            resultConnection?.let { return@let it }
            exception?.let { throw it }

            throw IOException("No connection is handed over after waiting.")
        }
    }

    companion object {
        val TAG = CommunicationBridge::class.java.simpleName

        @Throws(IOException::class, CommunicationException::class, JSONException::class)
        fun connect(kuick: Kuick, addressList: List<DeviceAddress>, device: Device?, pin: Int): CommunicationBridge {
            for (address in addressList) {
                try {
                    return connect(kuick, address, device, pin)
                } catch (ignored: IOException) {
                }
            }
            throw SocketException("Failed to connect to the socket address.")
        }

        @Throws(IOException::class, JSONException::class, CommunicationException::class)
        fun connect(kuick: Kuick, deviceAddress: DeviceAddress, device: Device?, pin: Int): CommunicationBridge {
            var device = device
            val activeConnection: ActiveConnection = openConnection(kuick.context, deviceAddress.inetAddress)
            val remoteDeviceId: String = activeConnection.receive().getAsString()
            if (device?.uid != null && device.uid != remoteDeviceId) {
                activeConnection.closeSafely()
                throw DifferentClientException(device, remoteDeviceId)
            }
            if (device == null) device = Device(remoteDeviceId)
            try {
                kuick.reconstruct(device)
            } catch (e: ReconstructionFailedException) {
                device.sendKey = AppUtils.generateKey()
            }
            activeConnection.reply(AppUtils.getLocalDeviceAsJson(kuick.context, device.sendKey, pin))
            DeviceLoader.processConnection(kuick, device, deviceAddress)
            DeviceLoader.loadAsClient(kuick, receiveSecure(activeConnection, device), device)
            receiveResult(activeConnection, device)
            return CommunicationBridge(kuick, activeConnection, device, deviceAddress)
        }

        @Throws(IOException::class)
        private fun openConnection(context: Context, inetAddress: InetAddress): ActiveConnection {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val manager: ConnectivityManager = context
                    .getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                return run {
                    val callback = NetworkBinderCallback(manager, inetAddress)
                    val builder: NetworkRequest.Builder = NetworkRequest.Builder()
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    manager.requestNetwork(builder.build(), callback)
                    callback.waitForConnection()
                }
            }
            return openConnection(inetAddress)
        }

        @Throws(IOException::class)
        private fun openConnection(inetAddress: InetAddress): ActiveConnection {
            return ActiveConnection.connect(
                InetSocketAddress(inetAddress, AppConfig.SERVER_PORT_COMMUNICATION),
                AppConfig.DEFAULT_TIMEOUT_SOCKET
            )
        }

        @Throws(IOException::class, JSONException::class, CommunicationException::class)
        fun receiveSecure(connection: ActiveConnection, targetDevice: Device): JSONObject {
            val jsonObject: JSONObject = connection.receive().asJson
            if (jsonObject.has(Keyword.ERROR)) {
                val errorCode = jsonObject.getString(Keyword.ERROR)
                when (errorCode) {
                    Keyword.ERROR_NOT_ALLOWED -> throw NotAllowedException(targetDevice)
                    Keyword.ERROR_NOT_TRUSTED -> throw NotTrustedException(targetDevice)
                    Keyword.ERROR_NOT_ACCESSIBLE -> throw ContentException(ContentException.Error.NotAccessible)
                    Keyword.ERROR_ALREADY_EXISTS -> throw ContentException(ContentException.Error.AlreadyExists)
                    Keyword.ERROR_NOT_FOUND -> throw ContentException(ContentException.Error.NotFound)
                    Keyword.ERROR_UNKNOWN -> throw CommunicationException()
                    else -> throw UnknownCommunicationErrorException(errorCode)
                }
            }
            return jsonObject
        }

        fun CommunicationBridge.receiveResult(): Boolean {
            return receiveResult(activeConnection, device)
        }

        @Throws(IOException::class, JSONException::class, CommunicationException::class)
        fun receiveResult(connection: ActiveConnection, targetDevice: Device): Boolean {
            return resultOf(receiveSecure(connection, targetDevice))
        }

        @Throws(JSONException::class)
        fun resultOf(jsonObject: JSONObject): Boolean {
            return jsonObject.getBoolean(Keyword.RESULT)
        }

        @Throws(IOException::class, JSONException::class, UnhandledCommunicationException::class)
        fun sendError(connection: ActiveConnection, exception: Exception) {
            try {
                throw exception
            } catch (e: NotTrustedException) {
                sendError(connection, Keyword.ERROR_NOT_TRUSTED)
            } catch (e: DeviceBlockedException) {
                sendError(connection, Keyword.ERROR_NOT_ALLOWED)
            } catch (e: DeviceVerificationException) {
                sendError(connection, Keyword.ERROR_NOT_ALLOWED)
            } catch (e: ReconstructionFailedException) {
                sendError(connection, Keyword.ERROR_NOT_FOUND)
            } catch (e: ContentException) {
                sendError(connection, e)
            } catch (e: Exception) {
                throw UnhandledCommunicationException("An unknown error was thrown during the communication", e)
            }
        }

        @Throws(IOException::class, JSONException::class)
        fun sendError(connection: ActiveConnection, e: ContentException) {
            when (e.error) {
                ContentException.Error.NotFound -> sendError(connection, Keyword.ERROR_NOT_FOUND)
                ContentException.Error.NotAccessible -> sendError(connection, Keyword.ERROR_NOT_ACCESSIBLE)
                ContentException.Error.AlreadyExists -> sendError(connection, Keyword.ERROR_ALREADY_EXISTS)
                else -> sendError(connection, Keyword.ERROR_UNKNOWN)
            }
        }

        @Throws(IOException::class, JSONException::class)
        fun sendError(connection: ActiveConnection, errorCode: String) {
            connection.reply(JSONObject().put(Keyword.ERROR, errorCode))
        }

        @Throws(IOException::class, JSONException::class)
        fun sendResult(connection: ActiveConnection, result: Boolean) {
            sendSecure(connection, result, JSONObject())
        }

        @Throws(JSONException::class, IOException::class)
        fun sendSecure(connection: ActiveConnection, result: Boolean, jsonObject: JSONObject) {
            connection.reply(jsonObject.put(Keyword.RESULT, result))
        }
    }
}