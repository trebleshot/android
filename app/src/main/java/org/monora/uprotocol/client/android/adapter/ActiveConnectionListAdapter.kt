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

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.app.IListingFragment
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.model.NetworkInterfaceModel
import org.monora.uprotocol.client.android.util.Networks
import org.monora.uprotocol.client.android.util.TextManipulators
import org.monora.uprotocol.client.android.util.TextManipulators.toNetworkTitle
import org.monora.uprotocol.client.android.widget.ListingAdapter
import java.net.NetworkInterface
import java.util.*

/**
 * created by: veli
 * date: 4/7/19 10:35 PM
 */
class ActiveConnectionListAdapter(
    fragment: IListingFragment<NetworkInterfaceModel, ViewHolder>,
) : ListingAdapter<NetworkInterfaceModel, ViewHolder>(fragment) {
    override fun onLoad(): MutableList<NetworkInterfaceModel> {
        val resultList: MutableList<NetworkInterfaceModel> = ArrayList()
        val interfaceList: List<NetworkInterface> = Networks.getInterfaces(
            true,
            AppConfig.DEFAULT_DISABLED_INTERFACES
        )
        val filteringDelegate = fragment.filteringDelegate
        for (networkInterface in interfaceList) {
            val editableInterface = NetworkInterfaceModel(
                networkInterface, networkInterface.toNetworkTitle(context)
            )
            if (filteringDelegate.disabled() || filteringDelegate.filter(fragment, editableInterface)) {
                resultList.add(editableInterface)
            }
        }
        return resultList
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(layoutInflater.inflate(R.layout.list_active_connection, parent, false))
        fragment.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.visitView)
            .setOnClickListener { v: View? -> fragment.performLayoutClickOpen(holder) }
        holder.itemView.findViewById<View>(R.id.selector)
            .setOnClickListener { v: View? -> fragment.setItemSelected(holder, true) }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val text1: TextView = holder.itemView.findViewById(R.id.text)
        val text2: TextView = holder.itemView.findViewById(R.id.text2)
        val firstAddress = Networks.getFirstInet4Address(item)
        text1.text = item.name()
        text2.text = firstAddress?.let { TextManipulators.makeWebShareLink(context, it.hostAddress) }
    }
}