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

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.util.AppUtils
import java.util.*

/**
 * created by: veli
 * date: 4/4/19 10:06 AM
 */
class ChooseMemberDialog(
    activity: Activity, memberList: List<LoadedMember>,
    clickListener: DialogInterface.OnClickListener?
) : AlertDialog.Builder(activity) {
    private val mList: MutableList<LoadedMember> = ArrayList<LoadedMember>()
    private val mInflater: LayoutInflater
    private val mIconBuilder: IShapeBuilder?

    private inner class ListAdapter : BaseAdapter() {
        val count: Int
            get() = mList.size

        override fun getItem(position: Int): Any {
            return mList[position]
        }

        override fun getItemId(position: Int): Long {
            return 0
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) convertView =
                mInflater.inflate(R.layout.list_transfer_member_selector, parent, false)
            val member: LoadedMember = getItem(position) as LoadedMember
            val image = convertView.findViewById<ImageView>(R.id.image)
            val actionImage = convertView.findViewById<ImageView>(R.id.actionImage)
            val text: TextView = convertView.findViewById<TextView>(R.id.text)
            text.setText(member.device.username)
            actionImage.setImageResource(if (TransferItem.Type.INCOMING == member.type) R.drawable.ic_arrow_down_white_24dp else R.drawable.ic_arrow_up_white_24dp)
            DeviceLoader.showPictureIntoView(member.device, image, mIconBuilder)
            return convertView
        }
    }

    init {
        mList.addAll(memberList)
        mInflater = LayoutInflater.from(activity)
        mIconBuilder = AppUtils.getDefaultIconBuilder(activity)
        if (memberList.size > 0) setAdapter(ListAdapter(), clickListener) else setMessage(R.string.text_listEmpty)
        setTitle(R.string.butn_useKnownDevice)
        setNegativeButton(R.string.butn_close, null)
    }
}