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
import android.widget.ImageView
import android.widget.TextView
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.app.IListingFragment
import org.monora.uprotocol.client.android.drawable.TextDrawable
import org.monora.uprotocol.client.android.model.LoadedMember
import org.monora.uprotocol.client.android.model.Transfer
import org.monora.uprotocol.client.android.model.TransferItem
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.DeviceLoader
import org.monora.uprotocol.client.android.util.Transfers
import org.monora.uprotocol.client.android.widget.ListingAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder

/**
 * created by: veli
 * date: 06.04.2018 12:46
 */
class TransferMemberListAdapter(
    fragment: IListingFragment<LoadedMember, ViewHolder>, val transfer: Transfer,
) : ListingAdapter<LoadedMember, ViewHolder>(fragment) {
    private val iconBuilder: TextDrawable.IShapeBuilder = AppUtils.getDefaultIconBuilder(fragment.requireContext())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(layoutInflater.inflate(R.layout.list_transfer_member_grid, parent, false))
        fragment.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.menu).setOnClickListener { v: View? ->
            fragment.performLayoutLongClick(holder)
        }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member: LoadedMember = getList()[position]
        val image = holder.itemView.findViewById<ImageView>(R.id.image)
        val text1: TextView = holder.itemView.findViewById(R.id.text1)
        val text2: TextView = holder.itemView.findViewById(R.id.text2)
        text1.setText(member.device.username)
        text2.setText(if (TransferItem.Type.INCOMING == member.type) R.string.text_sender else R.string.text_receiver)
        DeviceLoader.showPictureIntoView(member.device, image, iconBuilder)
    }

    override fun onLoad(): MutableList<LoadedMember> {
        return Transfers.loadMemberList(context, transfer.id, null).toMutableList()
    }
}