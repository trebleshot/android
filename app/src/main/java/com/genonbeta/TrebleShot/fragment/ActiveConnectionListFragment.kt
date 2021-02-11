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
import com.genonbeta.TrebleShot.util.AppUtils
import android.os.Bundle
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import android.net.Uri
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.ViewGroup
import android.view.View
import android.widget.Button
import androidx.transition.TransitionManager
import com.genonbeta.TrebleShot.util.TextUtils

/**
 * created by: veli
 * date: 4/7/19 10:59 PM
 */
class ActiveConnectionListFragment :
    EditableListFragment<EditableNetworkInterface?, RecyclerViewAdapter.ViewHolder?, ActiveConnectionListAdapter?>(),
    IconProvider {
    private val mFilter = IntentFilter()
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (NetworkManagerFragmentWIFI_AP_STATE_CHANGED == intent.action || ConnectivityManager.CONNECTIVITY_ACTION == intent.action || WifiManager.WIFI_STATE_CHANGED_ACTION == intent.action || WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION == intent.action || BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED == intent.action) refreshList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setLayoutResId(R.layout.layout_active_connection)
        isSortingSupported = false
        setFilteringSupported(true)
        setItemOffsetDecorationEnabled(true)
        setItemOffsetForEdgesEnabled(true)
        setDefaultItemOffsetPadding(resources.getDimension(R.dimen.padding_list_content_parent_layout))
        mFilter.addAction(NetworkManagerFragment.WIFI_AP_STATE_CHANGED)
        mFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        mFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
        mFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
        mFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = ActiveConnectionListAdapter(this)
        setEmptyListImage(R.drawable.ic_share_white_24dp)
        setEmptyListText(getString(R.string.text_listEmptyConnection))
        val webShareInfo: CardView = view.findViewById(R.id.card_web_share_info)
        val webShareInfoHideButton = view.findViewById<Button>(R.id.card_web_share_info_hide_button)
        val helpWebShareInfo = "help_webShareInfo"
        if (AppUtils.getDefaultPreferences(context)!!.getBoolean(helpWebShareInfo, true)) {
            webShareInfo.setVisibility(View.VISIBLE)
            webShareInfoHideButton.setOnClickListener { v: View? ->
                webShareInfo.setVisibility(View.GONE)
                TransitionManager.beginDelayedTransition((webShareInfo.getParent() as ViewGroup))
                AppUtils.getDefaultPreferences(context)!!.edit()
                    .putBoolean(helpWebShareInfo, false)
                    .apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(mReceiver, mFilter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(mReceiver)
    }

    override fun getIconRes(): Int {
        return R.drawable.ic_web_white_24dp
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_webShare)
    }

    override fun performDefaultLayoutClick(
        holder: RecyclerViewAdapter.ViewHolder,
        target: EditableNetworkInterface
    ): Boolean {
        WebShareDetailsDialog(
            requireActivity(), TextUtils.makeWebShareLink(
                requireContext(),
                Networks.getFirstInet4Address(target).getHostAddress()
            )
        ).show()
        return true
    }

    override fun performLayoutClickOpen(
        holder: RecyclerViewAdapter.ViewHolder?,
        target: EditableNetworkInterface?
    ): Boolean {
        if (!super.performLayoutClickOpen(holder, target)) startActivity(
            Intent(Intent.ACTION_VIEW).setData(
                Uri.parse(
                    TextUtils.makeWebShareLink(
                        requireContext(),
                        Networks.getFirstInet4Address(target).getHostAddress()
                    )
                )
            )
        )
        return true
    }
}