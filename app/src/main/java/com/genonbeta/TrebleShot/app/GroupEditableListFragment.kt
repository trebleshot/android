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
package com.genonbeta.TrebleShot.app

import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupEditable
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder

/**
 * created by: veli
 * date: 30.03.2018 16:10
 */
abstract class GroupEditableListFragment<T : GroupEditable, V : GroupViewHolder, E : GroupEditableListAdapter<T, V>>() :
    EditableListFragment<T, V, E>() {
    override var adapter: E
        get() = super.adapter
        set(value) {
            super.adapter = value
            value.setGroupBy(groupingCriteria)
        }

    private val groupingOptions: MutableMap<String, Int> = ArrayMap()

    var defaultGroupingCriteria: Int = GroupEditableListAdapter.MODE_GROUP_BY_NOTHING

    override fun onGridSpanSize(viewType: Int, currentSpanSize: Int): Int {
        return if (viewType == GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE
            || viewType == GroupEditableListAdapter.VIEW_TYPE_ACTION_BUTTON
        ) currentSpanSize else super.onGridSpanSize(viewType, currentSpanSize)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (!isUsingLocalSelection || !isLocalSelectionActivated) {
            val options: MutableMap<String, Int> = ArrayMap()
            onGroupingOptions(options)
            groupingOptions.clear()
            groupingOptions.putAll(options)
            if (groupingOptions.isNotEmpty()) {
                inflater.inflate(R.menu.actions_abs_group_shareable_list, menu)
                applyDynamicMenuItems(
                    menu.findItem(R.id.actions_abs_group_shareable_grouping),
                    R.id.actions_abs_group_shareable_group_grouping,
                    groupingOptions
                )
            }
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        if (!isUsingLocalSelection || !isLocalSelectionActivated) {
            checkPreferredDynamicItem(
                menu.findItem(R.id.actions_abs_group_shareable_grouping), groupingCriteria,
                groupingOptions
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.groupId == R.id.actions_abs_group_shareable_group_grouping)
            groupingCriteria = item.order
        else
            return super.onOptionsItemSelected(item)
        return true
    }

    open fun onGroupingOptions(options: MutableMap<String, Int>) {}

    var groupingCriteria: Int
        get() = viewPreferences.getInt(getUniqueSettingKey("GroupBy"), defaultGroupingCriteria)
        set(value) {
            viewPreferences.edit()
                .putInt(getUniqueSettingKey("GroupBy"), value)
                .apply()
            adapter.setGroupBy(value)
            refreshList()
        }
}