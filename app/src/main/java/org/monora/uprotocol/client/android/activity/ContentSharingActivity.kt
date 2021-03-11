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
package org.monora.uprotocol.client.android.activity

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
import androidx.viewpager2.widget.ViewPager2
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.MainFragmentStateAdapter
import org.monora.uprotocol.client.android.adapter.MainFragmentStateAdapter.PageItem
import org.monora.uprotocol.client.android.app.Activity
import org.monora.uprotocol.client.android.app.ListingFragment
import org.monora.uprotocol.client.android.app.ListingFragmentBase
import org.monora.uprotocol.client.android.dialog.ChooseSharingMethodDialog
import org.monora.uprotocol.client.android.fragment.*
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.service.backgroundservice.AsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.AttachedTaskListener
import org.monora.uprotocol.client.android.service.backgroundservice.BaseAttachableAsyncTask
import org.monora.uprotocol.client.android.service.backgroundservice.TaskMessage
import org.monora.uprotocol.client.android.task.OrganizeLocalSharingTask
import org.monora.uprotocol.client.android.ui.callback.LocalSharingCallback
import org.monora.uprotocol.client.android.ui.callback.SharingPerformerMenuCallback
import org.monora.uprotocol.client.android.util.Selections

/**
 * created by: veli
 * date: 13/04/18 19:45
 */
@AndroidEntryPoint
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
        val viewPager: ViewPager2 = findViewById(R.id.activity_content_sharing_view_pager)
        val pagerAdapter: MainFragmentStateAdapter = object : MainFragmentStateAdapter(
            this, supportFragmentManager, lifecycle
        ) {
            override fun onItemInstantiated(item: PageItem) {
                val fragment: Fragment? = item.fragment
                if (fragment is ListingFragmentBase<*>) {
                    if (viewPager.currentItem == item.currentPosition) {
                        attachListeners(fragment)
                    }
                }
            }
        }
        val arguments = Bundle()
        arguments.putBoolean(ListingFragment.ARG_SELECT_BY_CLICK, true)
        arguments.putBoolean(ListingFragment.ARG_HAS_BOTTOM_SPACE, false)
        // FIXME: 2/21/21 Sharing fragments were here
        pagerAdapter.add(
            PageItem(
                0,
                R.drawable.ic_short_text_white_24dp,
                getString(R.string.text_sharedTexts),
                SharedTextFragment::class.qualifiedName!!,
                arguments
            )
        )
        pagerAdapter.add(
            PageItem(
                1,
                R.drawable.ic_short_text_white_24dp,
                getString(R.string.text_files),
                SharedTextFragment::class.qualifiedName!!,
                arguments
            )
        )
        /*pagerAdapter.add(StableItem(0, ApplicationListFragment::class.qualifiedName!!, arguments))
        pagerAdapter.add(
            StableItem(1, FileExplorerFragment::class.qualifiedName!!, arguments, getString(R.string.text_files))
        )
        pagerAdapter.add(StableItem(2, AudioListFragment::class.qualifiedName!!, arguments))
        pagerAdapter.add(StableItem(3, ImageListFragment::class.qualifiedName!!, arguments))
        pagerAdapter.add(StableItem(4, VideoListFragment::class.qualifiedName!!, arguments))*/
        pagerAdapter.createTabs(tabLayout, withIcon = false, withText = true)
        viewPager.adapter = pagerAdapter

        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    tab?.let {
                        viewPager.setCurrentItem(tab.position, true)
                    }
                }

                override fun onTabUnselected(tab: TabLayout.Tab?) {

                }

                override fun onTabReselected(tab: TabLayout.Tab?) {

                }
            }
        )

        viewPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    super.onPageSelected(position)
                    tabLayout.getTabAt(position)?.select()
                    val fragment = pagerAdapter.getItem(position).fragment
                    if (fragment is ListingFragmentBase<*>) {
                        val editableListFragment = fragment as ListingFragmentBase<*>
                        attachListeners(editableListFragment)
                        Handler(Looper.getMainLooper()).postDelayed(
                            { editableListFragment.adapterImpl.syncAllAndNotify() }, 200
                        )
                    }
                }
            }
        )
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            if (canExit()) finish()
        } else {
            return super.onOptionsItemSelected(item)
        }
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
        if (backPressedListener?.onBackPressed() != true && canExit()) {
            super.onBackPressed()
        }
    }

    fun attachListeners(fragment: ListingFragmentBase<*>) {
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
        private val TAG = ContentSharingActivity::class.simpleName
    }
}