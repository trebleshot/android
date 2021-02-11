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
import android.net.MacAddress
import android.net.wifi.ScanResult
import android.net.wifi.WifiNetworkSuggestion
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.util.ObjectsCompat
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.fragment.DeviceListFragment
import com.genonbeta.TrebleShot.graphics.drawable.TextDrawable.*
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Connections
import com.genonbeta.TrebleShot.util.DeviceLoader
import com.genonbeta.TrebleShot.utilimport.NsdDaemon
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import java.util.*

class DeviceListAdapter(
    fragment: IEditableListFragment<VirtualDevice, ViewHolder>, private val connections: Connections,
    private val nsdDaemon: NsdDaemon, hiddenDeviceTypes: Array<Device.Type>,
) : EditableListAdapter<DeviceListAdapter.VirtualDevice, RecyclerViewAdapter.ViewHolder>(fragment) {
    private val iconBuilder: IShapeBuilder = AppUtils.getDefaultIconBuilder(context)

    private val hiddenDeviceTypes: List<Device.Type>

    override fun onLoad(): MutableList<VirtualDevice> {
        val devMode = AppUtils.getDefaultPreferences(context)
            .getBoolean("developer_mode", false)
        val list: MutableList<VirtualDevice> = ArrayList()
        if (connections.canReadScanResults()) {
            for (result in connections.wifiManager.scanResults) {
                if ((result.capabilities == null || result.capabilities == "[ESS]")
                    && Connections.isClientNetwork(result.SSID)
                ) list.add(DescriptionVirtualDevice(NetworkDescription(result)))
            }
        }
        for (device in AppUtils.getKuick(context).castQuery(
            SQLQuery.Select(Kuick.TABLE_DEVICES)
                .setOrderBy(Kuick.FIELD_DEVICES_LASTUSAGETIME + " DESC"), Device::class.java
        )) {
            if (nsdDaemon.isDeviceOnline(device))
                device.type = Device.Type.NormalOnline
            else if (Device.Type.NormalOnline == device.type)
                device.type = Device.Type.Normal
            if (!hiddenDeviceTypes.contains(device.type) && (!device.isLocal || devMode))
                list.add(DbVirtualDevice(device))
        }
        val filteredList: MutableList<VirtualDevice> = ArrayList()
        for (virtualDevice in list) if (filterItem(virtualDevice)) filteredList.add(virtualDevice)
        return filteredList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(
            layoutInflater.inflate(
                if (horizontalOrientation || isGridLayoutRequested()) R.layout.list_network_device_grid else R.layout.list_network_device,
                parent,
                false
            )
        )
        fragment.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.menu)
            .setOnClickListener { v: View? ->
                DeviceListFragment.openInfo(
                    fragment.getActivity(), connections,
                    getItem(holder.adapterPosition)
                )
            }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val virtualDevice: VirtualDevice = getItem(position)
        val parentView = holder.itemView
        val text1: TextView = parentView.findViewById(R.id.text1)
        val text2: TextView = parentView.findViewById(R.id.text2)
        val image = parentView.findViewById<ImageView>(R.id.image)
        val statusImage = parentView.findViewById<ImageView>(R.id.imageStatus)
        val layoutOnline = parentView.findViewById<View>(R.id.layout_online)
        text1.text = virtualDevice.name()
        text2.text = virtualDevice.description(context)
        layoutOnline.visibility = if (virtualDevice.isOnline()) View.VISIBLE else View.GONE
        var isRestricted = false
        var isTrusted = false
        if (virtualDevice is DbVirtualDevice) {
            val device = virtualDevice.device
            isRestricted = device.isBlocked
            isTrusted = device.isTrusted
            DeviceLoader.showPictureIntoView(device, image, iconBuilder)
        } else image.setImageDrawable(iconBuilder.buildRound(virtualDevice.name()))
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
        protected var isSelected = false

        override fun applyFilter(filteringKeywords: Array<String>): Boolean {
            for (keyword in filteringKeywords) if (keyword == name()) return true
            return false
        }

        abstract fun description(context: Context): String

        override fun getComparableName(): String = name()

        override fun getSelectableTitle(): String = name()

        abstract fun isOnline(): Boolean

        override fun isSelectableSelected(): Boolean {
            return isSelected
        }

        abstract fun name(): String
    }

    class DbVirtualDevice(val device: Device) : VirtualDevice() {
        override var id: Long = device.hashCode().toLong()

        override fun comparisonSupported(): Boolean {
            return true
        }

        override fun description(context: Context): String {
            return device.model
        }

        override fun getComparableDate(): Long {
            return device.lastUsageTime
        }

        override fun getComparableSize(): Long {
            return 0
        }

        override fun isOnline(): Boolean {
            return Device.Type.NormalOnline == device.type
        }

        override fun name(): String {
            return device.username
        }

        override fun setSelectableSelected(selected: Boolean): Boolean {
            isSelected = selected
            return true
        }
    }

    class DescriptionVirtualDevice(val description: NetworkDescription) : VirtualDevice() {
        override var id: Long = hashCode().toLong()

        override fun comparisonSupported(): Boolean {
            return true
        }

        override fun description(context: Context): String {
            return context.getString(R.string.text_trebleshotHotspot)
        }

        override fun getComparableDate(): Long {
            return System.currentTimeMillis()
        }

        override fun getComparableSize(): Long {
            return 0
        }

        override fun isOnline(): Boolean {
            return true
        }

        override fun name(): String {
            return description.ssid
        }

        override fun setSelectableSelected(selected: Boolean): Boolean {
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
            password?.let { builder.setWpa2Passphrase(it) }
            bssid?.let { builder.setBssid(MacAddress.fromString(it)) }

            return builder.build()
        }
    }

    init {
        this.hiddenDeviceTypes = listOf(*hiddenDeviceTypes)
    }
}