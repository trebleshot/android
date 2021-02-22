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
import android.os.Bundle
import android.view.*
import android.widget.PopupMenu
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.TransferMemberListAdapter
import com.genonbeta.TrebleShot.app.ListingFragment
import com.genonbeta.TrebleShot.app.ListingFragmentBase
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.model.LoadedMember
import com.genonbeta.TrebleShot.model.Transfer
import com.genonbeta.TrebleShot.dialog.DeviceInfoDialog
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.*
import org.monora.uprotocol.client.android.model.ContentModel

/**
 * created by: veli
 * date: 06.04.2018 12:58
 */
class TransferMemberListFragment : ListingFragment<LoadedMember, ViewHolder, TransferMemberListAdapter>() {
    private val transfer: Transfer by lazy {
        Transfer(arguments?.getLong(ARG_TRANSFER_ID, -1) ?: -1)
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: KuickDb.BroadcastData = KuickDb.toData(intent)
                if (Kuick.TABLE_TRANSFERMEMBER == data.tableName) {
                    refreshList()
                } else if (Kuick.TABLE_TRANSFER == data.tableName) {
                    updateTransferGroup()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        filteringSupported = false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TransferMemberListAdapter(this, transfer)
        emptyListImageView.setImageResource(R.drawable.ic_device_hub_white_24dp)
        emptyListTextView.text = getString(R.string.text_noDeviceForTransfer)
        updateTransferGroup()
        val paddingRecyclerView = resources
            .getDimension(R.dimen.padding_list_content_parent_layout).toInt()
        listView.setPadding(paddingRecyclerView, paddingRecyclerView, paddingRecyclerView, paddingRecyclerView)
        listView.clipToPadding = false
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, IntentFilter(KuickDb.ACTION_DATABASE_CHANGE))
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
    }

    override fun performDefaultLayoutClick(holder: ViewHolder, target: LoadedMember): Boolean {
        DeviceInfoDialog(requireActivity(), target.device).show()
        return true
    }

    override fun performDefaultLayoutLongClick(
        holder: ViewHolder,
        target: LoadedMember,
    ): Boolean {
        showPopupMenu(this, adapter, transfer, holder, holder.itemView, target)
        return true
    }

    fun updateTransferGroup() {
        AppUtils.getKuick(requireContext()).reconstruct(transfer)
    }

    companion object {
        const val ARG_TRANSFER_ID = "transferId"

        const val ARG_USE_HORIZONTAL_VIEW = "useHorizontalView"

        fun <T : ContentModel> showPopupMenu(
            fragment: ListingFragmentBase<T>,
            adapter: TransferMemberListAdapter,
            transfer: Transfer?,
            clazz: ViewHolder?,
            v: View?,
            member: LoadedMember,
        ) {
            val popupMenu = PopupMenu(fragment.getContext(), v)
            val menu = popupMenu.menu
            popupMenu.menuInflater.inflate(R.menu.popup_fragment_transfer_member, menu)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                val id = item.itemId
                if (id == R.id.popup_device_details) {
                    DeviceInfoDialog(fragment.requireActivity(), member.device).show()
                } else if (id == R.id.popup_remove) {
                    AppUtils.getKuick(fragment.requireContext()).removeAsynchronous(
                        fragment.requireActivity(), member, transfer
                    )
                } else return@setOnMenuItemClickListener false
                true
            }
            popupMenu.show()
        }
    }
}