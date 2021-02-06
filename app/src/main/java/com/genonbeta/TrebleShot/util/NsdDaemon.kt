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
package com.genonbeta.TrebleShot.utilimport

import android.content.*
import android.os.*
import android.util.Log
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.util.AppUtils

com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
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
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

/**
 * created by: Veli
 * date: 22.01.2018 15:35
 */
class NsdDaemon(val context: Context, val database: Kuick, private val mPreferences: SharedPreferences) {
    private var mNsdManager: NsdManager? = null
    private var mNsdDiscoveryListener: NsdManager.DiscoveryListener? = null
    private var mNsdRegistrationListener: NsdManager.RegistrationListener? = null
    private val mOnlineDeviceList: MutableMap<String, DeviceRoute?> = ArrayMap<String, DeviceRoute?>()

    @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private val discoveryListener: NsdManager.DiscoveryListener?
        private get() {
            if (mNsdDiscoveryListener == null) mNsdDiscoveryListener =
                com.genonbeta.TrebleShot.util.NsdDaemon.DiscoveryListener()
            return mNsdDiscoveryListener
        }

    @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private val nsdManager: NsdManager
        private get() {
            if (mNsdManager == null) mNsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
            return mNsdManager
        }

    @get:RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private val registrationListener: NsdManager.RegistrationListener?
        private get() {
            if (mNsdRegistrationListener == null) mNsdRegistrationListener =
                com.genonbeta.TrebleShot.util.NsdDaemon.RegistrationListener()
            return mNsdRegistrationListener
        }

    fun isDeviceOnline(device: Device?): Boolean {
        synchronized(mOnlineDeviceList) {
            for (deviceRoute in mOnlineDeviceList.values) if (deviceRoute.device.equals(
                    device
                )
            ) return true
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
            localServiceInfo.setServiceName(AppUtils.getLocalDeviceName(context))
            localServiceInfo.setServiceType(AppConfig.NSD_SERVICE_TYPE)
            localServiceInfo.setPort(AppConfig.SERVER_PORT_COMMUNICATION)
            try {
                nsdManager.registerService(localServiceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun startDiscovering() {
        try {
            if (isServiceEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) nsdManager.discoverServices(
                AppConfig.NSD_SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
        } catch (ignored: Exception) {
        }
    }

    fun stopDiscovering() {
        try {
            if (mNsdDiscoveryListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) nsdManager.stopServiceDiscovery(
                mNsdDiscoveryListener
            )
        } catch (ignored: Exception) {
        }
    }

    fun unregisterService() {
        try {
            if (mNsdRegistrationListener != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) nsdManager.unregisterService(
                mNsdRegistrationListener
            )
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
            if (serviceInfo.getServiceType() == AppConfig.NSD_SERVICE_TYPE) nsdManager.resolveService(
                serviceInfo,
                com.genonbeta.TrebleShot.util.NsdDaemon.ResolveListener()
            )
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.i(TAG, "'" + serviceInfo.getServiceName() + "' service is now lost.")
            synchronized(mOnlineDeviceList) {
                if (mOnlineDeviceList.remove(serviceInfo.getServiceName()) != null) context.sendBroadcast(
                    Intent(
                        ACTION_DEVICE_STATUS
                    )
                )
            }
        }

        fun clear() {
            mNsdDiscoveryListener = null
            synchronized(mOnlineDeviceList) { mOnlineDeviceList.clear() }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private inner class ResolveListener : NsdManager.ResolveListener {
        override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
            Log.e(TAG, "Could not resolve " + serviceInfo.getServiceName())
        }

        override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
            Log.v(
                TAG, "Resolved " + serviceInfo.getServiceName() + " with success has the IP address of "
                        + serviceInfo.getHost().getHostAddress()
            )
            DeviceLoader.load(
                database,
                serviceInfo.getHost(),
                OnDeviceResolvedListener { device: Device?, address: DeviceAddress? ->
                    synchronized(mOnlineDeviceList) {
                        mOnlineDeviceList.put(
                            serviceInfo.getServiceName(),
                            DeviceRoute(device, address)
                        )
                    }
                    context.sendBroadcast(Intent(ACTION_DEVICE_STATUS))
                })
        }
    }

    companion object {
        val TAG = NsdDaemon::class.java.simpleName
        const val ACTION_DEVICE_STATUS = "org.monora.trebleshot.android.intent.action.DEVICE_STATUS"
    }
}