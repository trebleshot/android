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

import android.app.Activity
import android.content.DialogInterface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.R
import org.monora.uprotocol.client.android.task.OrganizeLocalSharingTask
import org.monora.uprotocol.client.android.model.ContentModel

class ChooseSharingMethodDialog(activity: Activity, listener: PickListener) : AlertDialog.Builder(activity) {
    private val layoutInflater = LayoutInflater.from(context)

    private val sharingMethods = SharingMethod.values()

    internal inner class SharingMethodListAdapter : BaseAdapter() {
        override fun getCount(): Int {
            return sharingMethods.size
        }

        override fun getItem(position: Int): Any {
            return sharingMethods[position]
        }

        override fun getItemId(position: Int): Long {
            // Since the list will be a static one, item ids do not have importance.
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: layoutInflater.inflate(R.layout.list_sharing_method, parent, false)
            val sharingMethod = getItem(position) as SharingMethod
            val image = view.findViewById<ImageView>(R.id.image)
            val text1: TextView = view.findViewById(R.id.text1)
            image.setImageResource(sharingMethod.iconRes)
            text1.text = context.getString(sharingMethod.titleRes)
            return view
        }
    }

    enum class SharingMethod(
        @field:DrawableRes @param:DrawableRes val iconRes: Int,
        @field:StringRes @param:StringRes val titleRes: Int,
    ) {
        WebShare(R.drawable.ic_web_white_24dp, R.string.butn_webShare),
        LocalShare(R.drawable.ic_compare_arrows_white_24dp, R.string.text_devicesWithAppInstalled);
    }

    interface PickListener {
        fun onShareMethod(sharingMethod: SharingMethod)
    }

    companion object {
        fun createLocalShareOrganizingTask(
            method: SharingMethod,
            list: List<ContentModel>,
        ): OrganizeLocalSharingTask {
            return when (method) {
                SharingMethod.WebShare -> OrganizeLocalSharingTask(list, addNewDevice = false, webShare = true)
                SharingMethod.LocalShare -> OrganizeLocalSharingTask(list, addNewDevice = true, webShare = false)
            }
        }
    }

    init {
        setTitle(R.string.text_chooseSharingMethod)
        setAdapter(SharingMethodListAdapter()) { dialog: DialogInterface?, which: Int ->
            listener.onShareMethod(sharingMethods[which])
        }
        setNegativeButton(R.string.butn_cancel, null)
    }
}