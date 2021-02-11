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
package com.genonbeta.TrebleShot.fragment

import android.content.*
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.App
import android.os.Bundle
import android.view.View
import com.genonbeta.TrebleShot.dataobject.Device

/**
 * created by: veli
 * date: 3/11/19 7:43 PM
 */
class OnlineDeviceListFragment : DeviceListFragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        setHiddenDeviceTypes(arrayOf<Device.Type?>(Device.Type.Web, Device.Type.Normal))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        setFilteringSupported(false)
        setItemOffsetDecorationEnabled(false)
        setItemOffsetForEdgesEnabled(false)
        if (isScreenLarge) setDefaultViewingGridSize(4, 5) else if (isScreenNormal) setDefaultViewingGridSize(
            3,
            4
        ) else setDefaultViewingGridSize(2, 3)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val daemon: NsdDaemon = App.from(requireActivity()).getNsdDaemon()
        listView.isNestedScrollingEnabled = true
        setDividerVisible(false)
        if (!daemon.isServiceEnabled()) setEmptyListText(getString(R.string.text_nsdDisabled)) else if (!daemon.isDiscovering()) setEmptyListText(
            getString(R.string.text_nsdNotDiscovering)
        ) else setEmptyListText(getString(R.string.text_noOnlineDevices))
        if (context != null) {
            val padding = context!!.resources.getDimension(R.dimen.short_content_width_padding)
            listView.clipToPadding = false
            listView.setPadding(padding.toInt(), 0, padding.toInt(), 0)
        }
    }
}