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

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.TransferDetailActivity
import com.genonbeta.TrebleShot.adapter.TransferListAdapter
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.model.Transfer
import com.genonbeta.TrebleShot.model.TransferIndex
import com.genonbeta.TrebleShot.dialog.DialogUtils
import com.genonbeta.TrebleShot.task.FileTransferTask
import com.genonbeta.TrebleShot.ui.callback.IconProvider
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Lists
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.framework.util.actionperformer.SelectionModel
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider

/**
 * created by: Veli
 * date: 10.11.2017 00:15
 */
class TransferListFragment : GroupEditableListFragment<TransferIndex, GroupViewHolder, TransferListAdapter>(),
    IconProvider {
    private val filter = IntentFilter()

    override val iconRes: Int = R.drawable.ic_swap_vert_white_24dp

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: KuickDb.BroadcastData = KuickDb.toData(intent)
                if (Kuick.TABLE_TRANSFER == data.tableName || Kuick.TABLE_TRANSFERITEM == data.tableName) refreshList()
            } else if (App.ACTION_TASK_CHANGE == intent.action) updateTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filteringSupported = true
        defaultOrderingCriteria = EditableListAdapter.MODE_SORT_ORDER_DESCENDING
        defaultSortingCriteria = EditableListAdapter.MODE_SORT_BY_DATE
        defaultGroupingCriteria = GroupEditableListAdapter.MODE_GROUP_BY_DATE
        itemOffsetDecorationEnabled = true
        itemOffsetForEdgesEnabled = true
        defaultItemOffsetPadding = resources.getDimension(R.dimen.padding_list_content_parent_layout)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TransferListAdapter(this)
        emptyListImageView.setImageResource(R.drawable.ic_compare_arrows_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmptyTransfer)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        filter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        filter.addAction(App.ACTION_TASK_CHANGE)
    }

    override fun onCreatePerformerMenu(context: Context): PerformerMenu {
        return PerformerMenu(context, SelectionCallback(requireActivity(), this))
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, filter)
        updateTasks()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
    }

    override fun onSortingOptions(options: MutableMap<String, Int>) {
        options[getString(R.string.text_sortByDate)] = EditableListAdapter.MODE_SORT_BY_DATE
        options[getString(R.string.text_sortBySize)] = EditableListAdapter.MODE_SORT_BY_SIZE
    }

    override fun onGroupingOptions(options: MutableMap<String, Int>) {
        options[getString(R.string.text_groupByNothing)] = GroupEditableListAdapter.MODE_GROUP_BY_NOTHING
        options[getString(R.string.text_groupByDate)] = GroupEditableListAdapter.MODE_GROUP_BY_DATE
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_transfers)
    }

    override fun performDefaultLayoutClick(holder: GroupViewHolder, target: TransferIndex): Boolean {
        TransferDetailActivity.startInstance(requireActivity(), target.transfer)
        return true
    }

    fun updateTasks() {
        try {
            val tasks: List<FileTransferTask> = App.from(requireActivity()).getTaskListOf(FileTransferTask::class.java)
            val activeTaskList: MutableList<Long> = ArrayList()
            for (task in tasks) if (task.transfer != null) activeTaskList.add(task.transfer.id)
            adapter.updateActiveList(activeTaskList)
            refreshList()
        } catch (ignored: IllegalStateException) {
        }
    }

    private class SelectionCallback(
        activity: Activity, provider: PerformerEngineProvider,
    ) : EditableListFragment.SelectionCallback(activity, provider) {
        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu,
        ): Boolean {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu)
            inflater.inflate(R.menu.action_mode_group, targetMenu)
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            val id = item.itemId
            val kuick = AppUtils.getKuick(activity)
            val engine = getPerformerEngine() ?: return false
            val genericList: List<SelectionModel> = ArrayList<SelectionModel>(engine.getSelectionList())
            val indexList: List<TransferIndex> = Lists.typedListOf(genericList, TransferIndex::class.java)
            if (id == R.id.action_mode_group_delete) {
                val groupList: MutableList<Transfer> = ArrayList()
                for (index in indexList) groupList.add(index.transfer)
                DialogUtils.showRemoveTransferGroupListDialog(activity, groupList)
                return true
            } else if (id == R.id.action_mode_group_serve_on_web || id == R.id.action_mode_group_hide_on_web) {
                val served = id == R.id.action_mode_group_serve_on_web
                val changedList: MutableList<TransferIndex> = ArrayList<TransferIndex>()
                for (index in indexList) {
                    if (!index.hasOutgoing() || index.transfer.isServedOnWeb == served) continue
                    index.transfer.isServedOnWeb = served
                    changedList.add(index)
                }
                kuick.update(changedList)
                kuick.broadcast()
            } else super.onPerformerMenuSelected(performerMenu, item)
            return false
        }
    }
}