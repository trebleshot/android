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
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import android.view.*
import androidx.appcompat.app.AlertDialog
import java.lang.Exception

class ApplicationListFragment : GroupEditableListFragment<PackageHolder?, GroupViewHolder?, ApplicationListAdapter?>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilteringSupported(true)
        setHasOptionsMenu(true)
        setDefaultOrderingCriteria(EditableListAdapter.Companion.MODE_SORT_ORDER_DESCENDING)
        setDefaultSortingCriteria(EditableListAdapter.Companion.MODE_SORT_BY_DATE)
        setDefaultGroupingCriteria(GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE)
        setItemOffsetDecorationEnabled(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListAdapter(ApplicationListAdapter(this))
        setEmptyListImage(R.drawable.ic_android_head_white_24dp)
        setEmptyListText(getString(R.string.text_listEmptyApp))
    }

    override fun onGroupingOptions(options: MutableMap<String?, Int?>) {
        super.onGroupingOptions(options)
        options[getString(R.string.text_groupByNothing)] = GroupEditableListAdapter.Companion.MODE_GROUP_BY_NOTHING
        options[getString(R.string.text_groupByDate)] = GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_application, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.show_system_apps) {
            val isShowingSystem = !AppUtils.getDefaultPreferences(getContext())!!.getBoolean(
                "show_system_apps",
                false
            )
            AppUtils.getDefaultPreferences(getContext())!!.edit()
                .putBoolean("show_system_apps", isShowingSystem)
                .apply()
            refreshList()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val menuSystemApps = menu.findItem(R.id.show_system_apps)
        menuSystemApps.isChecked = AppUtils.getDefaultPreferences(getContext())!!.getBoolean(
            "show_system_apps",
            false
        )
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_application)
    }

    override fun performLayoutClickOpen(
        holder: GroupViewHolder,
        `object`: PackageHolder
    ): Boolean {
        try {
            val launchIntent: Intent = requireContext().getPackageManager()
                .getLaunchIntentForPackage(`object`.packageName)
            if (launchIntent != null) {
                AlertDialog.Builder(requireActivity())
                    .setMessage(R.string.ques_launchApplication)
                    .setNegativeButton(R.string.butn_cancel, null)
                    .setPositiveButton(R.string.butn_appLaunch) { dialog: DialogInterface?, which: Int ->
                        startActivity(
                            launchIntent
                        )
                    }
                    .show()
            } else Toast.makeText(getActivity(), R.string.mesg_launchApplicationError, Toast.LENGTH_SHORT).show()
            return true
        } catch (ignore: Exception) {
        }
        return false
    }

    override fun performDefaultLayoutClick(
        holder: GroupViewHolder,
        `object`: PackageHolder
    ): Boolean {
        return performLayoutClickOpen(holder, `object`)
    }
}