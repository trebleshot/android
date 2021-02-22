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
package org.monora.uprotocol.client.android.dialog

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.activity.TransferDetailActivity
import org.monora.uprotocol.client.android.model.LoadedMember
import org.monora.uprotocol.client.android.model.TransferIndex
import org.monora.uprotocol.client.android.model.TransferItem
import org.monora.uprotocol.client.android.drawable.TextDrawable.IShapeBuilder
import org.monora.uprotocol.client.android.util.AppUtils
import org.monora.uprotocol.client.android.util.DeviceLoader
import org.monora.uprotocol.client.android.util.Transfers

class ToggleMultipleTransferDialog(
    val activity: TransferDetailActivity, index: TransferIndex,
) : AlertDialog.Builder(activity) {
    private val members: Array<LoadedMember> = index.members

    private val inflater: LayoutInflater = LayoutInflater.from(activity)

    private val iconBuilder: IShapeBuilder = AppUtils.getDefaultIconBuilder(activity)

    private fun startTransfer(activity: TransferDetailActivity, index: TransferIndex, member: LoadedMember) {
        if (activity.isDeviceRunning(member.deviceId)) {
            Transfers.pauseTransfer(activity, member)
        } else {
            Transfers.startTransferWithTest(activity, index.transfer, member)
        }
    }

    private inner class ActiveListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return members.size
        }

        override fun getItem(position: Int): Any {
            return members[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.list_toggle_transfer, parent, false)
            val member: LoadedMember = getItem(position) as LoadedMember
            val image: ImageView = view.findViewById(R.id.image)
            val text: TextView = view.findViewById(R.id.text)
            val actionImage: ImageView = view.findViewById(R.id.actionImage)
            text.text = member.device.username
            actionImage.setImageResource(
                when {
                    activity.isDeviceRunning(member.deviceId) -> R.drawable.ic_pause_white_24dp
                    TransferItem.Type.INCOMING == member.type -> R.drawable.ic_arrow_down_white_24dp
                    else -> R.drawable.ic_arrow_up_white_24dp
                }
            )
            DeviceLoader.showPictureIntoView(member.device, image, iconBuilder)
            return view
        }
    }

    init {
        if (members.isNotEmpty()) setAdapter(ActiveListAdapter()) { dialog: DialogInterface?, which: Int ->
            startTransfer(activity, index, members[which])
        }
        setNegativeButton(R.string.butn_close, null)
        if (index.hasOutgoing()) setNeutralButton(R.string.butn_addDevices) { dialog: DialogInterface?, which: Int ->
            activity.startDeviceAddingActivity()
        }
        var senderMember: LoadedMember? = null
        for (member in index.members) if (TransferItem.Type.INCOMING == member.type) {
            senderMember = member
            break
        }
        if (index.hasIncoming() && senderMember != null) {
            setPositiveButton(R.string.butn_receive) { dialog: DialogInterface?, which: Int ->
                startTransfer(activity, index, senderMember)
            }
        }
    }
}