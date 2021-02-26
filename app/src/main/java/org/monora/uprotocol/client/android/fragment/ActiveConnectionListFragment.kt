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
package org.monora.uprotocol.client.android.fragment

import android.bluetooth.BluetoothA2dp
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager.CONNECTIVITY_ACTION
import android.net.Uri
import android.net.wifi.WifiManager.WIFI_STATE_CHANGED_ACTION
import android.net.wifi.p2p.WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.cardview.widget.CardView
import androidx.preference.PreferenceManager
import androidx.transition.TransitionManager
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.ActiveConnectionListAdapter
import org.monora.uprotocol.client.android.app.ListingFragment
import org.monora.uprotocol.client.android.dialog.WebShareDetailsDialog
import org.monora.uprotocol.client.android.fragment.NetworkManagerFragment.Companion.WIFI_AP_STATE_CHANGED
import org.monora.uprotocol.client.android.model.NetworkInterfaceModel
import org.monora.uprotocol.client.android.util.Networks
import org.monora.uprotocol.client.android.util.TextUtils

/**
 * created by: veli
 * date: 4/7/19 10:59 PM
 */
class ActiveConnectionListFragment :
    ListingFragment<NetworkInterfaceModel, ViewHolder, ActiveConnectionListAdapter>() {
    private val filter = IntentFilter()

    private val preferences by lazy {
        PreferenceManager.getDefaultSharedPreferences(requireContext())
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (WIFI_AP_STATE_CHANGED == intent.action || CONNECTIVITY_ACTION == intent.action
                || WIFI_STATE_CHANGED_ACTION == intent.action || WIFI_P2P_CONNECTION_CHANGED_ACTION == intent.action
                || BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED == intent.action
            ) refreshList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        layoutResId = R.layout.layout_active_connection
        filteringSupported = true
        filter.addAction(WIFI_AP_STATE_CHANGED)
        filter.addAction(CONNECTIVITY_ACTION)
        filter.addAction(WIFI_STATE_CHANGED_ACTION)
        filter.addAction(WIFI_P2P_CONNECTION_CHANGED_ACTION)
        filter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ActiveConnectionListAdapter(this)
        emptyListImageView.setImageResource(R.drawable.ic_share_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmptyConnection)
        val webShareInfo = view.findViewById<CardView>(R.id.card_web_share_info)
        val webShareInfoHideButton = view.findViewById<Button>(R.id.card_web_share_info_hide_button)
        val helpWebShareInfo = "help_webShareInfo"
        if (preferences.getBoolean(helpWebShareInfo, true)) {
            webShareInfo.visibility = View.VISIBLE
            webShareInfoHideButton.setOnClickListener { v: View? ->
                webShareInfo.visibility = View.GONE
                TransitionManager.beginDelayedTransition((webShareInfo.parent as ViewGroup))
                preferences.edit()
                    .putBoolean(helpWebShareInfo, false)
                    .apply()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
    }

    override fun performDefaultLayoutClick(holder: ViewHolder, target: NetworkInterfaceModel): Boolean {
        WebShareDetailsDialog(
            requireActivity(), TextUtils.makeWebShareLink(
                requireContext(),
                Networks.getFirstInet4Address(target)?.hostAddress
            )
        ).show()
        return true
    }

    override fun performLayoutClickOpen(
        holder: ViewHolder,
        target: NetworkInterfaceModel,
    ): Boolean {
        if (!super.performLayoutClickOpen(holder, target)) startActivity(
            Intent(Intent.ACTION_VIEW).setData(
                Uri.parse(
                    TextUtils.makeWebShareLink(
                        requireContext(),
                        Networks.getFirstInet4Address(target)?.hostAddress
                    )
                )
            )
        )
        return true
    }
}