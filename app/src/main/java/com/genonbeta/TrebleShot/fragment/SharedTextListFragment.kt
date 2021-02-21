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
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.TextEditorActivity
import com.genonbeta.TrebleShot.adapter.SharedTextListAdapter
import com.genonbeta.TrebleShot.app.ListingFragment
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Selections
import com.genonbeta.TrebleShot.widget.ListingAdapter
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.android.framework.util.actionperformer.SelectionModel
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import org.monora.uprotocol.client.android.database.model.SharedTextModel

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
class SharedTextListFragment : ListingFragment<SharedTextModel, ViewHolder, SharedTextListAdapter>() {
    override fun onAttach(context: Context) {
        super.onAttach(context)
        bottomSpaceShown = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        layoutResId = R.layout.layout_text_stream
        filteringSupported = true
        defaultOrderingCriteria = ListingAdapter.MODE_SORT_ORDER_DESCENDING
        defaultSortingCriteria = ListingAdapter.MODE_SORT_BY_DATE
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = SharedTextListAdapter(this)
        emptyListImageView.setImageResource(R.drawable.ic_forum_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmptyTextStream)
        view.findViewById<View>(R.id.layout_text_stream_fab).setOnClickListener { v: View? ->
            startActivity(
                Intent(activity, TextEditorActivity::class.java).setAction(TextEditorActivity.ACTION_EDIT_TEXT)
            )
        }
    }

    override fun onCreatePerformerMenu(context: Context): PerformerMenu {
        return PerformerMenu(context, SelectionCallback(requireActivity(), this))
    }

    override fun performDefaultLayoutClick(holder: ViewHolder, target: SharedTextModel): Boolean {
        startActivity(
            Intent(context, TextEditorActivity::class.java)
                .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, target)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
        return true
    }

    private class SelectionCallback(
        activity: Activity, provider: PerformerEngineProvider,
    ) : ListingFragment.SelectionCallback(activity, provider) {
        private lateinit var shareWithTrebleShot: MenuItem

        private lateinit var shareWithOthers: MenuItem

        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu,
        ): Boolean {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu)

            // Sharing text with this menu is unnecessary since only one item can be sent at a time. So, this will be
            // disabled until it is possible to send multiple items.
            //inflater.inflate(R.menu.action_mode_share, targetMenu);
            inflater.inflate(R.menu.action_mode_text_stream, targetMenu)

            // FIXME: 8/15/20 Using Editable Selection Callback which doesn't have the properties below
            shareWithTrebleShot = targetMenu.findItem(R.id.action_mode_share_trebleshot)
            //mShareWithOthers = targetMenu.findItem(R.id.action_mode_share_all_apps);
            getPerformerEngine()?.let { updateShareMethods(it) }
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            val id = item.itemId
            val engine = getPerformerEngine() ?: return false
            val genericSelectionList: List<SelectionModel> = ArrayList<SelectionModel>(engine.getSelectionList())
            val selectionList: MutableList<SharedTextModel> = ArrayList()
            val kuick = AppUtils.getKuick(activity)
            val context: Context = activity
            for (selectionModel in genericSelectionList) {
                if (selectionModel is SharedTextModel) {
                    selectionList.add(selectionModel)
                }
            }
            if (id == R.id.action_mode_text_stream_delete) {
                //kuick.remove(selectionList)
                kuick.broadcast()
                return true
                // FIXME: 8/15/20 Enable sharing with all apps
                //} else if (id == R.id.action_mode_share_all_apps || id == R.id.action_mode_share_trebleshot) {
            } else if (id == R.id.action_mode_share_trebleshot) {
                if (selectionList.size == 1) {
                    // FIXME: 2/21/21 Sharing text model
                    /*
                    val streamObject: TextStreamObject = selectionList[0]
                    val shareLocally = id == R.id.action_mode_share_trebleshot
                    val intent: Intent = (if (shareLocally) Intent(context, ShareActivity::class.java) else Intent())
                        .setAction(Intent.ACTION_SEND)
                        .putExtra(Intent.EXTRA_TEXT, streamObject.text)
                        */
                    //.setType("text/*")
                    /*activity.startActivity(
                        if (shareLocally) intent else Intent.createChooser(
                            intent, context.getString(R.string.text_fileShareAppChoose)
                        )
                    )*/
                } else Toast.makeText(context, R.string.mesg_textShareLimit, Toast.LENGTH_SHORT).show()
            } else return super.onPerformerMenuSelected(performerMenu, item)
            return false
        }

        override fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection, selectionModel: SelectionModel, isSelected: Boolean,
            position: Int,
        ) {
            super.onPerformerMenuItemSelected(performerMenu, engine, owner, selectionModel, isSelected, position)
            updateShareMethods(engine)
        }

        override fun onPerformerMenuItemSelected(
            performerMenu: PerformerMenu, engine: IPerformerEngine,
            owner: IBaseEngineConnection, selectionModelList: MutableList<out SelectionModel>,
            isSelected: Boolean, positions: IntArray,
        ) {
            super.onPerformerMenuItemSelected(performerMenu, engine, owner, selectionModelList, isSelected, positions)
            updateShareMethods(engine)
        }

        private fun updateShareMethods(engine: IPerformerEngine) {
            val totalSelections = Selections.getTotalSize(engine)
            shareWithOthers.isEnabled = totalSelections == 1
            shareWithTrebleShot.isEnabled = totalSelections == 1
        }
    }
}