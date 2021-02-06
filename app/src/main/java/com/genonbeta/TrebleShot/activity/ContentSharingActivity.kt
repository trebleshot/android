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
import android.os.*
import android.view.*
import android.widget.ProgressBar
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import androidx.viewpager.widget.ViewPager
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter.*
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.fragment.*
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.ui.callbackimport.LocalSharingCallback
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.google.android.material.tabs.TabLayout

/**
 * created by: veli
 * date: 13/04/18 19:45
 */
class ContentSharingActivity : Activity(), PerformerEngineProvider, LocalSharingCallback, AttachedTaskListener {
    private var mBackPressedListener: OnBackPressedListener? = null
    private val mPerformerEngine = PerformerEngine()
    private val mMenuCallback = SharingPerformerMenuCallback(
        this,
        this
    )
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mCardView: ViewGroup
    private lateinit var mActionMenuView: ActionMenuView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_content_sharing)
        mActionMenuView = findViewById(R.id.menu_view)
        mCardView = findViewById(R.id.activity_content_sharing_cardview)
        mProgressBar = findViewById(R.id.activity_content_sharing_progress_bar)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        if (supportActionBar != null) supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        val performerMenu = PerformerMenu(this, mMenuCallback)
        mMenuCallback.setLocalSharingCallback(this)
        mMenuCallback.setCancellable(false)
        performerMenu.load(mActionMenuView.getMenu())
        performerMenu.setUp(mPerformerEngine)
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
        arguments.putBoolean(EditableListFragment.Companion.ARG_SELECT_BY_CLICK, true)
        arguments.putBoolean(EditableListFragment.Companion.ARG_HAS_BOTTOM_SPACE, false)
        pagerAdapter.add(StableItem(0, ApplicationListFragment::class.java, arguments))
        pagerAdapter.add(
            StableItem(1, FileExplorerFragment::class.java, arguments).setTitle(
                getString(
                    R.string.text_files
                )
            )
        )
        pagerAdapter.add(StableItem(2, AudioListFragment::class.java, arguments))
        pagerAdapter.add(StableItem(3, ImageListFragment::class.java, arguments))
        pagerAdapter.add(StableItem(4, VideoListFragment::class.java, arguments))
        pagerAdapter.createTabs(tabLayout, false, true)
        viewPager.setAdapter(pagerAdapter)
        viewPager.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                viewPager.setCurrentItem(tab.getPosition())
                val fragment: Fragment = pagerAdapter.getItem(tab.getPosition())
                if (fragment is EditableListFragmentBase<*>) {
                    val editableListFragment = fragment as EditableListFragmentBase<*>
                    val adapter = editableListFragment.adapterImpl
                    attachListeners(editableListFragment)
                    if (editableListFragment.adapterImpl != null) Handler(Looper.getMainLooper()).postDelayed(
                        { adapter.syncAllAndNotify() }, 200
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
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onAttachTasks(taskList: List<BaseAttachableAsyncTask>) {
        super.onAttachTasks(taskList)
        for (task in taskList) if (task is OrganizeLocalSharingTask) task.anchor = this
    }

    override fun onShareLocal(shareableList: List<Shareable>) {
        ChooseSharingMethodDialog(this) { sharingMethod: SharingMethod? ->
            val task: OrganizeLocalSharingTask = ChooseSharingMethodDialog.Companion.createLocalShareOrganizingTask(
                sharingMethod,
                shareableList
            )
            runUiTask(task, this)
        }.show()
    }

    override fun onBackPressed() {
        if ((mBackPressedListener == null || !mBackPressedListener!!.onBackPressed()) && canExit()) {
            super.onBackPressed()
        }
    }

    fun attachListeners(fragment: EditableListFragmentBase<*>) {
        mMenuCallback.setForegroundConnection(fragment.engineConnection)
        mBackPressedListener = if (fragment is OnBackPressedListener) fragment else null
    }

    private fun canExit(): Boolean {
        if (SelectionUtils.getTotalSize(mPerformerEngine) > 0) {
            AlertDialog.Builder(this)
                .setMessage(R.string.ques_cancelSelection)
                .setNegativeButton(R.string.butn_no, null)
                .setPositiveButton(R.string.butn_yes) { dialog: DialogInterface?, which: Int -> finish() }
                .show()
            return false
        }
        return true
    }

    override fun getPerformerEngine(): IPerformerEngine? {
        return mPerformerEngine
    }

    override fun onTaskStateChange(task: BaseAttachableAsyncTask, state: AsyncTask.State?) {
        if (task is OrganizeLocalSharingTask) {
            when (state) {
                AsyncTask.State.Finished -> {
                    mActionMenuView!!.visibility = View.VISIBLE
                    mProgressBar!!.visibility = View.GONE
                    TransitionManager.beginDelayedTransition(mCardView!!)
                }
                AsyncTask.State.Starting -> {
                    mActionMenuView!!.visibility = View.GONE
                    mProgressBar!!.visibility = View.VISIBLE
                    TransitionManager.beginDelayedTransition(mCardView!!)
                }
            }
        }
    }

    override fun onTaskMessage(message: TaskMessage): Boolean {
        return false
    }

    companion object {
        val TAG = ContentSharingActivity::class.java.simpleName
    }
}