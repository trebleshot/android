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

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter

/**
 * created by: veli
 * date: 06.04.2018 12:46
 */
class TransferMemberListAdapter(fragment: IEditableListFragment<LoadedMember, ViewHolder>, transfer: Transfer) :
    EditableListAdapter<LoadedMember?, RecyclerViewAdapter.ViewHolder?>(fragment) {
    private val mTransfer: Transfer
    private val mIconBuilder: IShapeBuilder?
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(
            inflater.inflate(
                if (isHorizontalOrientation || isGridLayoutRequested) R.layout.list_transfer_member_grid else R.layout.list_transfer_member,
                parent,
                false
            )
        )
        fragment!!.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.menu)
            .setOnClickListener { v: View? -> fragment!!.performLayoutLongClick(holder) }
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member: LoadedMember = list[position]
        val image = holder.itemView.findViewById<ImageView>(R.id.image)
        val text1: TextView = holder.itemView.findViewById<TextView>(R.id.text1)
        val text2: TextView = holder.itemView.findViewById<TextView>(R.id.text2)
        text1.setText(member.device.username)
        text2.setText(if (TransferItem.Type.INCOMING == member.type) R.string.text_sender else R.string.text_receiver)
        DeviceLoader.showPictureIntoView(member.device, image, mIconBuilder)
    }

    override fun onLoad(): List<LoadedMember> {
        return Transfers.loadMemberList(context, mTransfer.id, null)
    }

    init {
        mIconBuilder = AppUtils.getDefaultIconBuilder(fragment.getContext())
        mTransfer = transfer
    }
}