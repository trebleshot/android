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
import android.net.wifi.ScanResult
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Connections
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
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