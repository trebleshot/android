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
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import android.os.Bundle
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.*
import android.widget.PopupMenu
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.dataobject.Transfer
import java.lang.Exception

/**
 * created by: veli
 * date: 06.04.2018 12:58
 */
class TransferMemberListFragment :
    EditableListFragment<LoadedMember, RecyclerViewAdapter.ViewHolder, TransferMemberListAdapter?>() {
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: BroadcastData = KuickDb.toData(intent)
                if (Kuick.Companion.TABLE_TRANSFERMEMBER == data.tableName) refreshList() else if (Kuick.Companion.TABLE_TRANSFER == data.tableName) updateTransferGroup()
            }
        }
    }
    private var mHeldGroup: Transfer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setFilteringSupported(false)
        isSortingSupported = false
        //setUseDefaultPaddingDecoration(true);
        //setUseDefaultPaddingDecorationSpaceForEdges(true);
        if (isScreenLarge) setDefaultViewingGridSize(4, 6) else if (isScreenNormal) setDefaultViewingGridSize(
            3,
            5
        ) else setDefaultViewingGridSize(2, 4)

        //setDefaultPaddingDecorationSize(getResources().getDimension(R.dimen.padding_list_content_parent_layout));
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = TransferMemberListAdapter(this, getTransferGroup())
        setEmptyListImage(R.drawable.ic_device_hub_white_24dp)
        setEmptyListText(getString(R.string.text_noDeviceForTransfer))
        updateTransferGroup()
        val paddingRecyclerView = resources
            .getDimension(R.dimen.padding_list_content_parent_layout).toInt()
        listView.setPadding(paddingRecyclerView, paddingRecyclerView, paddingRecyclerView, paddingRecyclerView)
        listView.clipToPadding = false
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(mReceiver, IntentFilter(KuickDb.ACTION_DATABASE_CHANGE))
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(mReceiver)
    }

    override fun performDefaultLayoutClick(holder: RecyclerViewAdapter.ViewHolder, `object`: LoadedMember): Boolean {
        DeviceInfoDialog(requireActivity(), `object`.device).show()
        return true
    }

    override fun performDefaultLayoutLongClick(
        holder: RecyclerViewAdapter.ViewHolder,
        `object`: LoadedMember
    ): Boolean {
        showPopupMenu<LoadedMember>(this, adapter, getTransferGroup(), holder, holder.itemView, `object`)
        return true
    }

    override fun isHorizontalOrientation(): Boolean {
        return (arguments != null && arguments!!.getBoolean(ARG_USE_HORIZONTAL_VIEW)
                || super.isHorizontalOrientation())
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_deviceList)
    }

    fun getTransferGroup(): Transfer {
        if (mHeldGroup == null) {
            mHeldGroup = Transfer(
                if (arguments == null) -1 else arguments!!.getLong(
                    ARG_TRANSFER_ID,
                    -1
                )
            )
            updateTransferGroup()
        }
        return mHeldGroup!!
    }

    private fun updateTransferGroup() {
        try {
            AppUtils.getKuick(context).reconstruct(mHeldGroup)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val ARG_TRANSFER_ID = "transferId"
        const val ARG_USE_HORIZONTAL_VIEW = "useHorizontalView"
        fun <T : Editable?> showPopupMenu(
            fragment: EditableListFragmentBase<T>,
            adapter: TransferMemberListAdapter?, transfer: Transfer?,
            clazz: RecyclerViewAdapter.ViewHolder?, v: View?,
            member: LoadedMember
        ) {
            val popupMenu = PopupMenu(fragment.context, v)
            val menu = popupMenu.menu
            popupMenu.menuInflater.inflate(R.menu.popup_fragment_transfer_member, menu)
            popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                val id = item.itemId
                if (id == R.id.popup_device_details) DeviceInfoDialog(
                    fragment.activity,
                    member.device
                ).show() else if (id == R.id.popup_remove) {
                    AppUtils.getKuick(fragment.context)
                        .removeAsynchronous<Transfer, LoadedMember>(fragment.activity, member, transfer)
                } else return@setOnMenuItemClickListener false
                true
            }
            popupMenu.show()
        }
    }
}