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
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import android.view.LayoutInflater
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.os.*
import android.util.Log
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.TrebleShot.config.Keyword
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.protocol.communication.CommunicationException
import com.genonbeta.TrebleShot.protocol.communication.ContentException
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.Closeable
import java.io.IOException
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketException

/**
 * created by: Veli
 * date: 11.02.2018 15:07
 */
class CommunicationBridge(
    val kuick: Kuick,
    activeConnection: ActiveConnection,
    device: Device,
    deviceAddress: DeviceAddress
) : Closeable {
    private val activeConnection: ActiveConnection
    val device: Device
    private val deviceAddress: DeviceAddress
    override fun close() {
        try {
            getActiveConnection().closeSafely()
        } catch (ignored: Exception) {
        }
    }

    fun getActiveConnection(): ActiveConnection {
        return activeConnection
    }

    val context: Context
        get() = kuick.context

    fun getDeviceAddress(): DeviceAddress {
        return deviceAddress
    }

    @Throws(JSONException::class, IOException::class)
    fun requestAcquaintance() {
        getActiveConnection().reply(JSONObject().put(Keyword.REQUEST, Keyword.REQUEST_ACQUAINTANCE))
    }

    @Throws(JSONException::class, IOException::class)
    fun requestFileTransfer(transferId: Long, files: JSONArray?) {
        getActiveConnection().reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.INDEX, files)
        )
    }

    @Throws(JSONException::class, IOException::class)
    fun requestFileTransferStart(transferId: Long, type: TransferItem.Type?) {
        getActiveConnection().reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_TRANSFER_JOB)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_TYPE, type)
        )
    }

    @Throws(JSONException::class, IOException::class)
    fun requestNotifyTransferState(transferId: Long, accepted: Boolean) {
        getActiveConnection().reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_NOTIFY_TRANSFER_STATE)
                .put(Keyword.TRANSFER_ID, transferId)
                .put(Keyword.TRANSFER_IS_ACCEPTED, accepted)
        )
    }

    @Throws(JSONException::class, IOException::class)
    fun requestTextTransfer(text: String?) {
        getActiveConnection().reply(
            JSONObject()
                .put(Keyword.REQUEST, Keyword.REQUEST_CLIPBOARD)
                .put(Keyword.TRANSFER_TEXT, text)
        )
    }

    @Throws(IOException::class, JSONException::class)
    fun sendError(errorCode: String?) {
        sendError(getActiveConnection(), errorCode)
    }

    @Throws(IOException::class, JSONException::class)
    fun sendResult(result: Boolean) {
        sendResult(getActiveConnection(), result)
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class NetworkBinderCallback(connectivityManager: ConnectivityManager?, inetAddress: InetAddress) :
        NetworkCallback() {
        private val connectivityManager: ConnectivityManager?
        private val inetAddress: InetAddress
        private val lock = Any()
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
            if (resultConnection == null) throw IOException("No connection is handed over after waiting.") else if (exception != null) throw exception
            return resultConnection
        }

        init {
            this.connectivityManager = connectivityManager
            this.inetAddress = inetAddress
        }
    }

    companion object {
        val TAG = CommunicationBridge::class.java.simpleName
        @Throws(IOException::class, CommunicationException::class, JSONException::class)
        fun connect(
            kuick: Kuick?, addressList: List<DeviceAddress?>, device: Device?,
            pin: Int
        ): CommunicationBridge {
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
            if (device != null && device.uid != null && device.uid != remoteDeviceId) {
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
                return if (manager != null) {
                    val callback = NetworkBinderCallback(manager, inetAddress)
                    val builder: NetworkRequest.Builder = NetworkRequest.Builder()
                    builder.addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    manager.requestNetwork(builder.build(), callback)
                    callback.waitForConnection()
                } else throw IOException("Connectivity manager is empty.")
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
        fun receiveSecure(connection: ActiveConnection?, targetDevice: Device?): JSONObject {
            val jsonObject: JSONObject = connection.receive().getAsJson()
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

        @JvmOverloads
        @Throws(IOException::class, JSONException::class, CommunicationException::class)
        fun receiveResult(
            connection: ActiveConnection? = getActiveConnection(),
            targetDevice: Device? = getDevice()
        ): Boolean {
            return resultOf(receiveSecure(connection, targetDevice))
        }

        @Throws(JSONException::class)
        fun resultOf(jsonObject: JSONObject): Boolean {
            return jsonObject.getBoolean(Keyword.RESULT)
        }

        @Throws(IOException::class, JSONException::class, UnhandledCommunicationException::class)
        fun sendError(connection: ActiveConnection?, exception: Exception?) {
            try {
                throw exception!!
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
        fun sendError(connection: ActiveConnection?, e: ContentException) {
            when (e.error) {
                ContentException.Error.NotFound -> sendError(connection, Keyword.ERROR_NOT_FOUND)
                ContentException.Error.NotAccessible -> sendError(connection, Keyword.ERROR_NOT_ACCESSIBLE)
                ContentException.Error.AlreadyExists -> sendError(connection, Keyword.ERROR_ALREADY_EXISTS)
                else -> sendError(connection, Keyword.ERROR_UNKNOWN)
            }
        }

        @Throws(IOException::class, JSONException::class)
        fun sendError(connection: ActiveConnection?, errorCode: String?) {
            connection.reply(JSONObject().put(Keyword.ERROR, errorCode))
        }

        @Throws(IOException::class, JSONException::class)
        fun sendResult(connection: ActiveConnection?, result: Boolean) {
            sendSecure(connection, result, JSONObject())
        }

        @Throws(JSONException::class, IOException::class)
        fun sendSecure(connection: ActiveConnection?, result: Boolean, jsonObject: JSONObject) {
            connection.reply(jsonObject.put(Keyword.RESULT, result))
        }
    }

    init {
        this.activeConnection = activeConnection
        this.device = device
        this.deviceAddress = deviceAddress
    }
}