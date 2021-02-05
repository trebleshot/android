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
package com.genonbeta.TrebleShot.adapter

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
import android.os.Parcelable
import android.os.Parcel
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
import android.os.Bundle
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
import android.os.PowerManager
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
import android.net.wifi.ScanResult
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import android.widget.ImageView
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.util.Connections
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.IllegalStateException
import java.util.*

class DeviceListAdapter(
    fragment: IEditableListFragment<VirtualDevice, ViewHolder>, private val mConnections: Connections,
    nsdDaemon: NsdDaemon, hiddenDeviceTypes: Array<Device.Type?>?
) : EditableListAdapter<VirtualDevice?, RecyclerViewAdapter.ViewHolder?>(fragment) {
    private val mIconBuilder: IShapeBuilder?
    private val mHiddenDeviceTypes: List<Device.Type>
    private val mNsdDaemon: NsdDaemon
    override fun onLoad(): List<VirtualDevice> {
        val devMode = AppUtils.getDefaultPreferences(context)!!
            .getBoolean("developer_mode", false)
        val list: MutableList<VirtualDevice> = ArrayList()
        if (mConnections.canReadScanResults()) {
            for (result in mConnections.wifiManager.scanResults) {
                if ((result.capabilities == null || result.capabilities == "[ESS]")
                    && Connections.Companion.isClientNetwork(result.SSID)
                ) list.add(DescriptionVirtualDevice(NetworkDescription(result)))
            }
        }
        for (device in AppUtils.getKuick(context).castQuery(
            SQLQuery.Select(Kuick.Companion.TABLE_DEVICES)
                .setOrderBy(Kuick.Companion.FIELD_DEVICES_LASTUSAGETIME + " DESC"), Device::class.java
        )) {
            if (mNsdDaemon.isDeviceOnline(device)) device.type =
                Device.Type.NORMAL_ONLINE else if (Device.Type.NORMAL_ONLINE == device.type) device.type =
                Device.Type.NORMAL
            if (!mHiddenDeviceTypes.contains(device.type) && (!device.isLocal || devMode)) list.add(
                DbVirtualDevice(
                    device
                )
            )
        }
        val filteredList: MutableList<VirtualDevice> = ArrayList()
        for (virtualDevice in list) if (filterItem(virtualDevice)) filteredList.add(virtualDevice)
        return filteredList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(
            inflater.inflate(
                if (isHorizontalOrientation || isGridLayoutRequested) R.layout.list_network_device_grid else R.layout.list_network_device,
                parent,
                false
            )
        )
        fragment!!.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.menu)
            .setOnClickListener { v: View? ->
                DeviceListFragment.Companion.openInfo(
                    fragment!!.getActivity(), mConnections,
                    list[holder.adapterPosition]
                )
            }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val virtualDevice: VirtualDevice? = getItem(position)
        val parentView = holder.itemView
        val text1: TextView = parentView.findViewById<TextView>(R.id.text1)
        val text2: TextView = parentView.findViewById<TextView>(R.id.text2)
        val image = parentView.findViewById<ImageView>(R.id.image)
        val statusImage = parentView.findViewById<ImageView>(R.id.imageStatus)
        val layoutOnline = parentView.findViewById<View>(R.id.layout_online)
        text1.setText(virtualDevice!!.name())
        text2.setText(virtualDevice.description(context))
        layoutOnline.visibility = if (virtualDevice.isOnline()) View.VISIBLE else View.GONE
        var isRestricted = false
        var isTrusted = false
        if (virtualDevice is DbVirtualDevice) {
            val device = virtualDevice.device
            isRestricted = device.isBlocked
            isTrusted = device.isTrusted
            DeviceLoader.showPictureIntoView(device, image, mIconBuilder)
        } else image.setImageDrawable(mIconBuilder.buildRound(virtualDevice.name()))
        if (isRestricted) {
            statusImage.visibility = View.VISIBLE
            statusImage.setImageResource(R.drawable.ic_block_white_24dp)
        } else if (isTrusted) {
            statusImage.visibility = View.VISIBLE
            statusImage.setImageResource(R.drawable.ic_vpn_key_white_24dp)
        } else {
            statusImage.visibility = View.GONE
        }
    }

    abstract class VirtualDevice : Editable {
        protected var mIsSelected = false
        override fun applyFilter(filteringKeywords: Array<String>): Boolean {
            for (keyword in filteringKeywords) if (keyword == name()) return true
            return false
        }

        abstract fun description(context: Context): String?
        override fun getComparableName(): String? {
            return name()
        }

        fun getSelectableTitle(): String? {
            return name()
        }

        abstract fun isOnline(): Boolean
        fun isSelectableSelected(): Boolean {
            return mIsSelected
        }

        abstract fun name(): String?
        override fun setId(id: Long) {
            throw IllegalStateException("This object does not support ID attributing.")
        }
    }

    class DbVirtualDevice(val device: Device) : VirtualDevice() {
        override fun comparisonSupported(): Boolean {
            return true
        }

        override fun description(context: Context): String? {
            return device.model
        }

        override fun getComparableDate(): Long {
            return device.lastUsageTime
        }

        override fun getComparableSize(): Long {
            return 0
        }

        override fun getId(): Long {
            return device.hashCode().toLong()
        }

        override fun isOnline(): Boolean {
            return Device.Type.NORMAL_ONLINE == device.type
        }

        override fun name(): String? {
            return device.username
        }

        fun setSelectableSelected(selected: Boolean): Boolean {
            mIsSelected = selected
            return true
        }
    }

    class DescriptionVirtualDevice(val description: NetworkDescription) : VirtualDevice() {
        override fun comparisonSupported(): Boolean {
            return true
        }

        override fun description(context: Context): String? {
            return context.getString(R.string.text_trebleshotHotspot)
        }

        override fun getComparableDate(): Long {
            return System.currentTimeMillis()
        }

        override fun getComparableSize(): Long {
            return 0
        }

        override fun getId(): Long {
            return hashCode().toLong()
        }

        override fun isOnline(): Boolean {
            return true
        }

        override fun name(): String? {
            return description.ssid
        }

        fun setSelectableSelected(selected: Boolean): Boolean {
            return false
        }
    }

    class NetworkDescription(var ssid: String, var bssid: String?, var password: String?) {
        constructor(result: ScanResult) : this(result.SSID, result.BSSID, null) {}

        override fun hashCode(): Int {
            return ObjectsCompat.hash(ssid, bssid, password)
        }

        @RequiresApi(29)
        fun toNetworkSuggestion(): WifiNetworkSuggestion {
            val builder: WifiNetworkSuggestion.Builder = WifiNetworkSuggestion.Builder()
                .setSsid(ssid)
                .setIsAppInteractionRequired(true)
            if (password != null) builder.setWpa2Passphrase(password)
            if (bssid != null) builder.setBssid(MacAddress.fromString(bssid))
            return builder.build()
        }
    }

    init {
        mIconBuilder = AppUtils.getDefaultIconBuilder(context)
        mNsdDaemon = nsdDaemon
        mHiddenDeviceTypes = if (hiddenDeviceTypes != null) Arrays.asList(*hiddenDeviceTypes) else ArrayList()
    }
}