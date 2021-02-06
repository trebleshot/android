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
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask

class ChooseSharingMethodDialog(activity: Activity?, listener: PickListener) : AlertDialog.Builder(
    activity!!
) {
    private val mLayoutInflater: LayoutInflater
    private val mSharingMethods = SharingMethod.values()

    internal inner class SharingMethodListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return mSharingMethods.size
        }

        override fun getItem(position: Int): Any {
            return mSharingMethods[position]
        }

        override fun getItemId(position: Int): Long {
            // Since the list will be a static one, item ids do not have importance.
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
            var convertView = convertView
            if (convertView == null) convertView = mLayoutInflater.inflate(R.layout.list_sharing_method, parent, false)
            val sharingMethod = getItem(position) as SharingMethod
            val image = convertView.findViewById<ImageView>(R.id.image)
            val text1: TextView = convertView.findViewById<TextView>(R.id.text1)
            image.setImageResource(sharingMethod.mIconRes)
            text1.setText(context.getString(sharingMethod.mTitleRes))
            return convertView
        }
    }

    enum class SharingMethod(
        @field:DrawableRes @param:DrawableRes val mIconRes: Int,
        @field:StringRes @param:StringRes val mTitleRes: Int
    ) {
        WebShare(
            R.drawable.ic_web_white_24dp,
            R.string.butn_webShare
        ),
        LocalShare(R.drawable.ic_compare_arrows_white_24dp, R.string.text_devicesWithAppInstalled);
    }

    interface PickListener {
        fun onShareMethod(sharingMethod: SharingMethod?)
    }

    companion object {
        fun createLocalShareOrganizingTask(
            method: SharingMethod?,
            shareableList: List<Shareable>
        ): OrganizeLocalSharingTask {
            return when (method) {
                SharingMethod.WebShare -> OrganizeLocalSharingTask(shareableList, false, true)
                SharingMethod.LocalShare -> OrganizeLocalSharingTask(shareableList, true, false)
                else -> OrganizeLocalSharingTask(shareableList, true, false)
            }
        }
    }

    init {
        mLayoutInflater = LayoutInflater.from(context)
        setTitle(R.string.text_chooseSharingMethod)
        setAdapter(SharingMethodListAdapter(), DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
            listener.onShareMethod(
                mSharingMethods[which]
            )
        })
        setNegativeButton(R.string.butn_cancel, null)
    }
}