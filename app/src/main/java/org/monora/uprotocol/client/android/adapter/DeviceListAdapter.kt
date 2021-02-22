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
package org.monora.uprotocol.client.android.adapter

import android.content.Context
import android.net.MacAddress
import android.net.wifi.ScanResult
import android.net.wifi.WifiNetworkSuggestion
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.util.ObjectsCompat
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.IListingFragment
import org.monora.uprotocol.client.android.database.Kuick
import org.monora.uprotocol.client.android.fragment.DeviceListFragment
import org.monora.uprotocol.client.android.drawable.TextDrawable.IShapeBuilder
import org.monora.uprotocol.client.android.model.Device
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.Connections
import org.monora.uprotocol.client.android.util.DeviceLoader
import org.monora.uprotocol.client.android.util.NsdDaemon
import org.monora.uprotocol.client.android.widget.ListingAdapter
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import org.monora.uprotocol.client.android.backend.Destination
import org.monora.uprotocol.client.android.backend.OperationBackend
import org.monora.uprotocol.client.android.backend.SharingBackend
import org.monora.uprotocol.client.android.model.ContentModel
import java.util.*

class DeviceListAdapter(
    fragment: IListingFragment<VirtualDevice, ViewHolder>,
    private val connections: Connections,
    private val nsdDaemon: NsdDaemon,
    hiddenDeviceTypes: Array<Device.Type>,
) : ListingAdapter<DeviceListAdapter.VirtualDevice, RecyclerViewAdapter.ViewHolder>(fragment) {
    private val iconBuilder: IShapeBuilder = AppUtils.getDefaultIconBuilder(context)

    private val hiddenDeviceTypes: List<Device.Type> = listOf(*hiddenDeviceTypes)

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
        val delegate = fragment.filteringDelegate
        for (virtualDevice in list) if (delegate.disabled() || delegate.filter(fragment, virtualDevice)) {
            filteredList.add(virtualDevice)
        }
        return filteredList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(layoutInflater.inflate(R.layout.list_network_device, parent, false))
        fragment.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.menu)
            .setOnClickListener { v: View? ->
                DeviceListFragment.openInfo(
                    fragment.requireActivity(), connections,
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

    abstract class VirtualDevice : ContentModel {
        protected var isSelected = false

        abstract fun description(context: Context): String

        abstract fun isOnline(): Boolean

        override fun canSelect(): Boolean = true

        override fun selected(): Boolean {
            return isSelected
        }

        override fun select(selected: Boolean) {
            isSelected = selected
        }
    }

    class DbVirtualDevice(val device: Device) : VirtualDevice() {
        override fun description(context: Context): String {
            return device.model
        }

        override fun isOnline(): Boolean {
            return Device.Type.NormalOnline == device.type
        }

        override fun name(): String {
            return device.username
        }

        override fun canCopy(): Boolean = false

        override fun canMove(): Boolean = false

        override fun canShare(): Boolean = false

        override fun canRemove(): Boolean = false

        override fun canRename(): Boolean = false

        override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
            throw UnsupportedOperationException()
        }

        override fun dateCreated(): Long = device.lastUsageTime

        override fun dateModified(): Long = device.lastUsageTime

        override fun dateSupported(): Boolean = true

        override fun filter(charSequence: CharSequence): Boolean = name().contains(charSequence)

        override fun id(): Long = device.uid.hashCode().toLong()

        override fun length(): Long {
            throw java.lang.UnsupportedOperationException()
        }

        override fun lengthSupported(): Boolean = false

        override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
            throw UnsupportedOperationException()
        }

        override fun remove(operationBackend: OperationBackend): Boolean {
            TODO("Not yet implemented")
        }

        override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
            throw UnsupportedOperationException()
        }
    }

    class DescriptionVirtualDevice(val description: NetworkDescription) : VirtualDevice() {
        override fun description(context: Context): String {
            return context.getString(R.string.text_trebleshotHotspot)
        }

        override fun isOnline(): Boolean {
            return true
        }

        override fun canCopy(): Boolean = false

        override fun canMove(): Boolean = false

        override fun canShare(): Boolean = false

        override fun canRemove(): Boolean = false

        override fun canRename(): Boolean = false

        override fun copy(operationBackend: OperationBackend, destination: Destination): Boolean {
            throw UnsupportedOperationException()
        }

        override fun dateCreated(): Long {
            throw UnsupportedOperationException()
        }

        override fun dateModified(): Long {
            throw UnsupportedOperationException()
        }

        override fun dateSupported(): Boolean = false

        override fun filter(charSequence: CharSequence): Boolean = description.ssid.equals(charSequence)

        override fun id(): Long = description.hashCode().toLong()

        override fun length(): Long {
            throw UnsupportedOperationException()
        }

        override fun lengthSupported(): Boolean = false

        override fun move(operationBackend: OperationBackend, destination: Destination): Boolean {
            TODO("Not yet implemented")
        }

        override fun remove(operationBackend: OperationBackend): Boolean {
            TODO("Not yet implemented")
        }

        override fun share(operationBackend: OperationBackend, sharingBackend: SharingBackend): Boolean {
            TODO("Not yet implemented")
        }

        override fun select(selected: Boolean) {
            TODO("Not yet implemented")
        }

        override fun name(): String {
            return description.ssid
        }
    }

    class NetworkDescription(var ssid: String, var bssid: String?, var password: String?) {
        constructor(result: ScanResult) : this(result.SSID, result.BSSID, null)

        override fun equals(other: Any?): Boolean {
            if (other is NetworkDescription) {
                return bssid == other.bssid || (bssid == null && ssid == other.ssid)
            }
            return super.equals(other)
        }

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
}