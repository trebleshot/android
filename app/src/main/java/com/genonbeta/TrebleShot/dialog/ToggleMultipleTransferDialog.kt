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
package com.genonbeta.TrebleShot.dialog

import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Transfers

class ToggleMultipleTransferDialog(activity: TransferDetailActivity, index: TransferIndex) :
    AlertDialog.Builder(activity) {
    private val mActivity: TransferDetailActivity
    private val mMembers: Array<LoadedMember>
    private val mInflater: LayoutInflater
    private val mIconBuilder: IShapeBuilder?
    private fun startTransfer(activity: TransferDetailActivity, index: TransferIndex, member: LoadedMember?) {
        if (mActivity.isDeviceRunning(member.deviceId)) Transfers.pauseTransfer(
            activity,
            member
        ) else Transfers.startTransferWithTest(activity, index.transfer, member)
    }

    private inner class ActiveListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return mMembers.size
        }

        override fun getItem(position: Int): Any {
            return mMembers[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) convertView = mInflater.inflate(R.layout.list_toggle_transfer, parent, false)
            val member: LoadedMember = getItem(position) as LoadedMember
            val image = convertView.findViewById<ImageView>(R.id.image)
            val text: TextView = convertView.findViewById<TextView>(R.id.text)
            val actionImage = convertView.findViewById<ImageView>(R.id.actionImage)
            text.setText(member.device.username)
            actionImage.setImageResource(if (mActivity.isDeviceRunning(member.deviceId)) R.drawable.ic_pause_white_24dp else if (TransferItem.Type.INCOMING == member.type) R.drawable.ic_arrow_down_white_24dp else R.drawable.ic_arrow_up_white_24dp)
            DeviceLoader.showPictureIntoView(member.device, image, mIconBuilder)
            return convertView
        }
    }

    init {
        mActivity = activity
        mInflater = LayoutInflater.from(activity)
        mIconBuilder = AppUtils.getDefaultIconBuilder(activity)
        mMembers = index.members
        if (mMembers.size > 0) setAdapter(
            ActiveListAdapter(),
            DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                startTransfer(
                    activity,
                    index,
                    mMembers[which]
                )
            })
        setNegativeButton(R.string.butn_close, null)
        if (index.hasOutgoing()) setNeutralButton(R.string.butn_addDevices) { dialog: DialogInterface?, which: Int -> activity.startDeviceAddingActivity() }
        var senderMember: LoadedMember? = null
        for (member in index.members) if (TransferItem.Type.INCOMING == member.type) {
            senderMember = member
            break
        }
        if (index.hasIncoming() && senderMember != null) {
            val finalSenderMember: LoadedMember? = senderMember
            setPositiveButton(R.string.butn_receive) { dialog: DialogInterface?, which: Int ->
                startTransfer(
                    activity, index,
                    finalSenderMember
                )
            }
        }
    }
}