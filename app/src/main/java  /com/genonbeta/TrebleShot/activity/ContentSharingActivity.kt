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
package com.genonbeta.TrebleShot.activity

import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter.StableItem
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.fragment.*
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.TaskMessage
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.util.Selections
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.google.android.material.tabs.TabLayout
import org.monora.uprotocol.client.android.model.ContentModel

/**
 * created by: veli
 * date: 13/04/18 19:45
 */
class ContentSharingActivity : Activity(), PerformerEngineProvider, LocalSharingCallback, AttachedTaskListener {
    private var backPressedListener: OnBackPressedListener? = null

    private val menuCallback = SharingPerformerMenuCallback(this, this)

    private val performerEngine = PerformerEngine()

    private lateinit var progressBar: ProgressBar

    private lateinit var cardView: ViewGroup

    private lateinit var actionMenuView: ActionMenuView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_sharing)
        actionMenuView = findViewById(R.id.menu_view)
        cardView = findViewById(R.id.activity_content_sharing_cardview)
        progressBar = findViewById(R.id.activity_content_sharing_progress_bar)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val performerMenu = PerformerMenu(this, menuCallback)
        menuCallback.localSharingCallback = this
        menuCallback.cancellable = false
        performerMenu.load(actionMenuView.menu)
        performerMenu.setUp(performerEngine)
        val tabLayout: TabLayout = findViewById(R.id.activity_content_sharing_tab_layout)
        val viewPager: ViewPager = findViewById(R.id.activity_content_sharing_view_pager)
        val pagerAdapter: SmartFragmentPagerAdapter = object : SmartFragmentPagerAdapter(
            this, supportFragmentManager
        ) {
            override fun onItemInstantiated(item: StableItem) {
                val fragment: Fragment? = item.initiatedItem
                if (fragment is EditableListFragmentBase<*>) {
                    val fragmentImpl = fragment as EditableListFragmentBase<*>
                    if (viewPager.currentItem == item.currentPosition)
                        attachListeners(fragmentImpl)
                }
            }
        }
        val arguments = Bundle()
        arguments.putBoolean(EditableListFragment.ARG_SELECT_BY_CLICK, true)
        arguments.putBoolean(EditableListFragment.ARG_HAS_BOTTOM_SPACE, false)
        pagerAdapter.add(StableItem(0, ApplicationListFragment::class.qualifiedName!!, arguments))
        /*pagerAdapter.add(
            StableItem(1, FileExplorerFragment::class.qualifiedName!!, arguments, getString(R.string.text_files))
        )
        pagerAdapter.add(StableItem(2, AudioListFragment::class.qualifiedName!!, arguments))
        pagerAdapter.add(StableItem(3, ImageListFragment::class.qualifiedName!!, arguments))
        pagerAdapter.add(StableItem(4, VideoListFragment::class.qualifiedName!!, arguments))*/
        pagerAdapter.createTabs(tabLayout, icons = false, text = true)
        viewPager.adapter = pagerAdapter
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.currentItem = tab.position
                val fragment = pagerAdapter.getItem(tab.position)

                if (fragment is EditableListFragmentBase<*>) {
                    val editableListFragment = fragment as EditableListFragmentBase<*>
                    attachListeners(editableListFragment)
                    Handler(Looper.getMainLooper()).postDelayed(
                        { editableListFragment.adapterImpl.syncAllAndNotify() }, 200
                    )
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == android.R.id.home) {
            if (canExit()) finish()
        } else
            return super.onOptionsItemSelected(item)
        return true
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList) if (task is OrganizeLocalSharingTask) task.anchor = this
    }

    override fun onShareLocal(shareableList: List<ContentModel>) {
        ChooseSharingMethodDialog(
            this,
            object : ChooseSharingMethodDialog.PickListener {
                override fun onShareMethod(sharingMethod: ChooseSharingMethodDialog.SharingMethod) {
                    val task = ChooseSharingMethodDialog.createLocalShareOrganizingTask(sharingMethod, shareableList)
                    runUiTask(task, this@ContentSharingActivity)
                }
            }
        ).show()
    }

    override fun onBackPressed() {
        if ((backPressedListener == null || !backPressedListener!!.onBackPressed()) && canExit()) {
            super.onBackPressed()
        }
    }

    fun attachListeners(fragment: EditableListFragmentBase<*>) {
        menuCallback.foregroundConnection = fragment.engineConnection
        backPressedListener = if (fragment is OnBackPressedListener) fragment else null
    }

    private fun canExit(): Boolean {
        if (Selections.getTotalSize(performerEngine) > 0) {
            AlertDialog.Builder(this)
                .setMessage(R.string.ques_cancelSelection)
                .setNegativeButton(R.string.butn_no, null)
                .setPositiveButton(R.string.butn_yes) { dialog: DialogInterface?, which: Int -> finish() }
                .show()
            return false
        }
        return true
    }

    override fun getPerformerEngine(): IPerformerEngine {
        return performerEngine
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State) {
        if (task is OrganizeLocalSharingTask) {
            when (state) {
                AsyncTask.State.Finished -> {
                    actionMenuView.visibility = View.VISIBLE
                    progressBar.visibility = View.GONE
                    TransitionManager.beginDelayedTransition(cardView)
                }
                AsyncTask.State.Starting -> {
                    actionMenuView.visibility = View.GONE
                    progressBar.visibility = View.VISIBLE
                    TransitionManager.beginDelayedTransition(cardView)
                }
            }
        }
    }

    override fun onTaskMessage(taskMessage: TaskMessage): Boolean {
        return false
    }

    companion object {
        val TAG = ContentSharingActivity::class.java.simpleName
    }
}