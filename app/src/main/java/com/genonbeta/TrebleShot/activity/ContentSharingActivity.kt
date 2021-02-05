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

import com.genonbeta.TrebleShot.dataobject.MappedSelectable.Companion.compileFrom
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withORs
import com.genonbeta.TrebleShot.dataobject.Identifier.Companion.from
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesPending
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.bytesValue
import com.genonbeta.TrebleShot.dataobject.TransferItem.flag
import com.genonbeta.TrebleShot.dataobject.TransferItem.putFlag
import com.genonbeta.TrebleShot.dataobject.Identity.Companion.withANDs
import com.genonbeta.TrebleShot.dataobject.TransferItem.Companion.from
import com.genonbeta.TrebleShot.dataobject.DeviceAddress.hostAddress
import com.genonbeta.TrebleShot.dataobject.Container.expand
import com.genonbeta.TrebleShot.dataobject.Device.equals
import com.genonbeta.TrebleShot.dataobject.TransferItem.flags
import com.genonbeta.TrebleShot.dataobject.TransferItem.getFlag
import com.genonbeta.TrebleShot.dataobject.TransferItem.Flag.toString
import com.genonbeta.TrebleShot.dataobject.TransferItem.reconstruct
import com.genonbeta.TrebleShot.dataobject.Device.generatePictureId
import com.genonbeta.TrebleShot.dataobject.TransferItem.setDeleteOnRemoval
import com.genonbeta.TrebleShot.dataobject.MappedSelectable.selectableTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasOutgoing
import com.genonbeta.TrebleShot.dataobject.TransferIndex.hasIncoming
import com.genonbeta.TrebleShot.dataobject.Comparable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableDate
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Comparable.comparableName
import com.genonbeta.TrebleShot.dataobject.Editable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Editable.id
import com.genonbeta.TrebleShot.dataobject.Shareable.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.initialize
import com.genonbeta.TrebleShot.dataobject.Shareable.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.Shareable.comparisonSupported
import com.genonbeta.TrebleShot.dataobject.Shareable.comparableSize
import com.genonbeta.TrebleShot.dataobject.Shareable.applyFilter
import com.genonbeta.TrebleShot.dataobject.Device.hashCode
import com.genonbeta.TrebleShot.dataobject.TransferIndex.percentage
import com.genonbeta.TrebleShot.dataobject.TransferIndex.getMemberAsTitle
import com.genonbeta.TrebleShot.dataobject.TransferIndex.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfCompleted
import com.genonbeta.TrebleShot.dataobject.TransferIndex.numberOfTotal
import com.genonbeta.TrebleShot.dataobject.TransferIndex.bytesTotal
import com.genonbeta.TrebleShot.dataobject.TransferItem.isSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.setSelectableSelected
import com.genonbeta.TrebleShot.dataobject.TransferItem.senderFlagList
import com.genonbeta.TrebleShot.dataobject.TransferItem.getPercentage
import com.genonbeta.TrebleShot.dataobject.TransferItem.setId
import com.genonbeta.TrebleShot.dataobject.TransferItem.comparableDate
import com.genonbeta.TrebleShot.dataobject.Identity.equals
import com.genonbeta.TrebleShot.dataobject.Transfer.equals
import com.genonbeta.TrebleShot.dataobject.TransferMember.reconstruct
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import android.content.DialogInterface
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
import android.content.Intent
import com.genonbeta.TrebleShot.activity.AddDeviceActivity
import androidx.annotation.DrawableRes
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.TrebleShot.ui.callback.LocalSharingCallback
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback
import com.genonbeta.TrebleShot.dataobject.MappedSelectable
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.PickListener
import com.genonbeta.TrebleShot.dialog.ChooseSharingMethodDialog.SharingMethod
import com.genonbeta.TrebleShot.task.OrganizeLocalSharingTask
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.util.NotificationUtils
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.content.IntentFilter
import android.content.BroadcastReceiver
import androidx.annotation.StyleRes
import android.content.pm.PackageManager
import com.genonbeta.TrebleShot.activity.WelcomeActivity
import com.genonbeta.TrebleShot.GlideApp
import com.bumptech.glide.request.target.CustomTarget
import android.graphics.drawable.Drawable
import android.graphics.Bitmap
import com.genonbeta.TrebleShot.config.AppConfig
import kotlin.jvm.Synchronized
import com.genonbeta.TrebleShot.service.BackgroundService
import android.graphics.BitmapFactory
import com.genonbeta.TrebleShot.dialog.RationalePermissionRequest
import com.genonbeta.TrebleShot.service.backgroundservice.AttachedTaskListener
import com.genonbeta.TrebleShot.service.backgroundservice.AttachableAsyncTask
import com.genonbeta.TrebleShot.dialog.ProfileEditorDialog
import android.widget.ProgressBar
import kotlin.jvm.JvmOverloads
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.android.framework.util.actionperformer.IEngineConnection
import com.genonbeta.android.framework.util.actionperformer.EngineConnection
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine
import com.genonbeta.TrebleShot.app.EditableListFragment.FilteringDelegate
import android.database.ContentObserver
import android.os.*
import android.view.*
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.ActionMenuView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.transition.TransitionManager
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.service.backgroundservice.AsyncTask
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`

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
    private var mProgressBar: ProgressBar? = null
    private var mCardView: ViewGroup? = null
    private var mActionMenuView: ActionMenuView? = null
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
        val tabLayout: TabLayout = findViewById<TabLayout>(R.id.activity_content_sharing_tab_layout)
        val viewPager: ViewPager = findViewById<ViewPager>(R.id.activity_content_sharing_view_pager)
        val pagerAdapter: SmartFragmentPagerAdapter = object : SmartFragmentPagerAdapter(
            this,
            supportFragmentManager
        ) {
            override fun onItemInstantiated(item: StableItem) {
                val fragment: Fragment = item.getInitiatedItem()
                if (fragment is EditableListFragmentBase<*>) {
                    val fragmentImpl = fragment as EditableListFragmentBase<*>
                    if (viewPager.getCurrentItem() == item.getCurrentPosition()) attachListeners(fragmentImpl)
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
        viewPager.addOnPageChangeListener(TabLayoutOnPageChangeListener(tabLayout))
        tabLayout.addOnTabSelectedListener(object : OnTabSelectedListener {
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