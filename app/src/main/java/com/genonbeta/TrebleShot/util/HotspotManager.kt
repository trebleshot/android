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
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

abstract class HotspotManager internal constructor(context: Context) {
    private val mWifiManager: WifiManager
    protected var mSecondaryCallback: LocalOnlyHotspotCallback? = null
    val wifiManager: WifiManager
        get() = mWifiManager

    abstract fun disable(): Boolean
    abstract fun enable(): Boolean
    abstract fun enableConfigured(apName: String, passKeyWPA2: String?): Boolean
    abstract val configuration: WifiConfiguration?
    abstract val previousConfig: WifiConfiguration?
    abstract val isEnabled: Boolean
    abstract val isStarted: Boolean
    @RequiresApi
    fun setSecondaryCallback(callback: LocalOnlyHotspotCallback?) {
        mSecondaryCallback = callback
    }

    abstract fun unloadPreviousConfig(): Boolean

    @RequiresApi(26)
    class OreoHotspotManager(context: Context) : HotspotManager(context) {
        private var mHotspotReservation: LocalOnlyHotspotReservation? = null
        override fun disable(): Boolean {
            if (mHotspotReservation == null) return false
            mHotspotReservation.close()
            mHotspotReservation = null
            return true
        }

        override fun enable(): Boolean {
            try {
                wifiManager.startLocalOnlyHotspot(object : LocalOnlyHotspotCallback() {
                    override fun onStarted(reservation: LocalOnlyHotspotReservation) {
                        super.onStarted(reservation)
                        mHotspotReservation = reservation
                        if (mSecondaryCallback != null) mSecondaryCallback.onStarted(reservation)
                    }

                    override fun onStopped() {
                        super.onStopped()
                        mHotspotReservation = null
                        if (mSecondaryCallback != null) mSecondaryCallback.onStopped()
                    }

                    override fun onFailed(reason: Int) {
                        super.onFailed(reason)
                        mHotspotReservation = null
                        if (mSecondaryCallback != null) mSecondaryCallback.onFailed(reason)
                    }
                }, Handler(Looper.getMainLooper()))
                return true
            } catch (ignored: Throwable) {
            }
            return false
        }

        override fun getConfiguration(): WifiConfiguration? {
            return if (mHotspotReservation == null) null else mHotspotReservation.getWifiConfiguration()
        }

        override fun getPreviousConfig(): WifiConfiguration? {
            return getConfiguration()
        }

        override fun enableConfigured(apName: String, passKeyWPA2: String?): Boolean {
            return enable()
        }

        override fun isEnabled(): Boolean {
            return OldHotspotManager.enabled(wifiManager)
        }

        override fun isStarted(): Boolean {
            return mHotspotReservation != null
        }

        override fun unloadPreviousConfig(): Boolean {
            return mHotspotReservation != null
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
                val result = invokeSilently(isWifiApEnabled, wifiManager)
                    ?: return false
                return result as Boolean
            }

            private fun invokeSilently(method: Method?, receiver: Any, vararg args: Any): Any? {
                try {
                    return method!!.invoke(receiver, *args)
                } catch (e: IllegalAccessException) {
                    Log.e(TAG, "exception in invoking methods: " + method!!.name + "(): " + e.message)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "exception in invoking methods: " + method!!.name + "(): " + e.message)
                } catch (e: InvocationTargetException) {
                    Log.e(TAG, "exception in invoking methods: " + method!!.name + "(): " + e.message)
                }
                return null
            }

            fun supported(): Boolean {
                return getWifiApState != null && isWifiApEnabled != null && setWifiApEnabled != null && getWifiApConfiguration != null
            }

            init {
                for (method in WifiManager::class.java.getDeclaredMethods()) {
                    when (com.genonbeta.TrebleShot.util.method.getName()) {
                        "getWifiApConfiguration" -> getWifiApConfiguration = com.genonbeta.TrebleShot.util.method
                        "getWifiApState" -> getWifiApState = com.genonbeta.TrebleShot.util.method
                        "isWifiApEnabled" -> isWifiApEnabled = com.genonbeta.TrebleShot.util.method
                        "setWifiApEnabled" -> setWifiApEnabled = com.genonbeta.TrebleShot.util.method
                        "setWifiApConfiguration" -> setWifiApConfiguration = com.genonbeta.TrebleShot.util.method
                    }
                }
            }
        }

        private var mPreviousConfig: WifiConfiguration? = null
        override fun disable(): Boolean {
            unloadPreviousConfig()
            return setHotspotEnabled(mPreviousConfig, false)
        }

        override fun enable(): Boolean {
            wifiManager.setWifiEnabled(false)
            return setHotspotEnabled(getConfiguration(), true)
        }

        override fun enableConfigured(apName: String, passKeyWPA2: String?): Boolean {
            wifiManager.setWifiEnabled(false)
            if (mPreviousConfig == null) mPreviousConfig = getConfiguration()
            val wifiConfiguration = WifiConfiguration()
            wifiConfiguration.SSID = apName
            if (passKeyWPA2 != null && passKeyWPA2.length >= 8) {
                wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK)
                wifiConfiguration.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN)
                wifiConfiguration.preSharedKey = passKeyWPA2
            } else wifiConfiguration.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE)
            return setHotspotEnabled(wifiConfiguration, true)
        }

        override fun isEnabled(): Boolean {
            return enabled(wifiManager)
        }

        override fun isStarted(): Boolean {
            return getPreviousConfig() != null
        }

        override fun getConfiguration(): WifiConfiguration? {
            return invokeSilently(getWifiApConfiguration, wifiManager) as WifiConfiguration?
        }

        override fun getPreviousConfig(): WifiConfiguration? {
            return mPreviousConfig
        }

        private fun setHotspotConfig(config: WifiConfiguration): Boolean {
            val result = invokeSilently(setWifiApConfiguration, wifiManager, config)
                ?: return false
            return result as Boolean
        }

        private fun setHotspotEnabled(config: WifiConfiguration?, enabled: Boolean): Boolean {
            val result = invokeSilently(
                setWifiApEnabled,
                wifiManager,
                config,
                enabled
            )
                ?: return false
            return result as Boolean
        }

        override fun unloadPreviousConfig(): Boolean {
            if (mPreviousConfig == null) return false
            val result = setHotspotConfig(mPreviousConfig)
            mPreviousConfig = null
            return result
        }
    }

    companion object {
        private const val TAG = "HotspotUtils"
        val isSupported: Boolean
            get() = Build.VERSION.SDK_INT >= 26 || OldHotspotManager.supported()

        fun newInstance(context: Context): HotspotManager {
            return if (Build.VERSION.SDK_INT >= 26) OreoHotspotManager(context) else OldHotspotManager(context)
        }
    }

    init {
        mWifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }
}