/*
 * Copyright (C) 2020 Veli Tasalı
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
package com.genonbeta.TrebleShot.ui.callback

import android.app.Activity
import android.view.*
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import java.util.*

open class SharingPerformerMenuCallback(activity: Activity?, provider: PerformerEngineProvider) :
    EditableListFragment.SelectionCallback(activity, provider) {
    private var mLocalSharingCallback: LocalSharingCallback? = null
    override fun onPerformerMenuList(performerMenu: PerformerMenu, inflater: MenuInflater, targetMenu: Menu): Boolean {
        super.onPerformerMenuList(performerMenu, inflater, targetMenu)
        inflater.inflate(R.menu.action_mode_share, targetMenu)
        return true
    }

    override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
        val id = item.itemId
        val performerEngine = performerEngine ?: return false
        val shareableList = compileShareableListFrom(compileFrom(performerEngine))
        if (id == R.id.action_mode_share_trebleshot) {
            if (shareableList.size > 0) {
                if (mLocalSharingCallback != null) mLocalSharingCallback!!.onShareLocal(shareableList) else ChooseSharingMethodDialog(
                    activity
                ) { method: SharingMethod? ->
                    val task: OrganizeLocalSharingTask =
                        ChooseSharingMethodDialog.Companion.createLocalShareOrganizingTask(
                            method, ArrayList(shareableList)
                        )
                    App.Companion.run<OrganizeLocalSharingTask>(activity, task)
                }.show()
            }
        } else return super.onPerformerMenuSelected(performerMenu, item)

        // I want the menus to keep showing because sharing does not alter data. If it is so descendants should
        // check and return 'true'.
        return false
    }

    fun setLocalSharingCallback(callback: LocalSharingCallback?) {
        mLocalSharingCallback = callback
    }

    companion object {
        private fun compileShareableListFrom(mappedSelectableList: List<MappedSelectable<*>>): List<Shareable> {
            val shareableList: MutableList<Shareable> = ArrayList()
            for (mappedSelectable in mappedSelectableList) if (mappedSelectable.selectable is Shareable) shareableList.add(
                mappedSelectable.selectable as Shareable
            )
            return shareableList
        }
    }
}