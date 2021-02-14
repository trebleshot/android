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

import android.content.Context
import android.os.Bundle
import android.view.View
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.util.NsdDaemon

/**
 * created by: veli
 * date: 3/11/19 7:43 PM
 */
class OnlineDeviceListFragment : DeviceListFragment() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        hiddenDeviceTypes = arrayOf(Device.Type.Web, Device.Type.Normal)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(false)
        isFilteringSupported = false
        itemOffsetDecorationEnabled = false
        itemOffsetForEdgesEnabled = false
        defaultViewingGridSize = if (isScreenNormal()) 3 else 2
        defaultViewingGridSizeLandscape = if (isScreenNormal()) 5 else 3
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val daemon: NsdDaemon = App.from(requireActivity()).nsdDaemon
        listView.isNestedScrollingEnabled = true
        setDividerVisible(false)
        emptyListTextView.text = if (!daemon.enabled) {
            getString(R.string.text_nsdDisabled)
        } else if (!daemon.discovering) {
            getString(R.string.text_nsdNotDiscovering)
        } else {
            getString(R.string.text_noOnlineDevices)
        }

        val padding = requireContext().resources.getDimension(R.dimen.short_content_width_padding)
        listView.clipToPadding = false
        listView.setPadding(padding.toInt(), 0, padding.toInt(), 0)
    }
}