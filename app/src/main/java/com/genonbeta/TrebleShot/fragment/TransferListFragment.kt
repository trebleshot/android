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
import android.content.*
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
import android.os.Parcelable
import android.os.Parcel
import com.genonbeta.TrebleShot.io.Containable
import android.os.Parcelable.Creator
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.AddDeviceActivity.AvailableFragment
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
import androidx.appcompat.app.AppCompatActivity
import com.genonbeta.TrebleShot.service.backgroundservice.BaseAttachableAsyncTask
import android.os.Bundle
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
import android.os.PowerManager
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
import com.genonbeta.TrebleShot.app.EditableListFragment.LayoutClickListener
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.view.LongTextBubbleFastScrollViewProvider
import com.genonbeta.TrebleShot.widget.recyclerview.ItemOffsetDecoration
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase
import android.os.Looper
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import android.view.View.OnLongClickListener
import com.genonbeta.TrebleShot.dataobject.Device
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.IllegalStateException
import java.util.ArrayList

/**
 * created by: Veli
 * date: 10.11.2017 00:15
 */
class TransferListFragment : GroupEditableListFragment<TransferIndex?, GroupViewHolder?, TransferListAdapter?>(),
    IconProvider {
    private val mFilter = IntentFilter()
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: BroadcastData = KuickDb.toData(intent)
                if (data != null && (Kuick.Companion.TABLE_TRANSFER == data.tableName || Kuick.Companion.TABLE_TRANSFERITEM == data.tableName)) refreshList()
            } else if (App.Companion.ACTION_TASK_CHANGE == intent.action) updateTasks()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilteringSupported(true)
        setDefaultOrderingCriteria(EditableListAdapter.Companion.MODE_SORT_ORDER_DESCENDING)
        setDefaultSortingCriteria(EditableListAdapter.Companion.MODE_SORT_BY_DATE)
        setDefaultGroupingCriteria(GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE)
        setItemOffsetDecorationEnabled(true)
        setItemOffsetForEdgesEnabled(true)
        setDefaultItemOffsetPadding(getResources().getDimension(R.dimen.padding_list_content_parent_layout))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListAdapter(TransferListAdapter(this))
        setEmptyListImage(R.drawable.ic_compare_arrows_white_24dp)
        setEmptyListText(getString(R.string.text_listEmptyTransfer))
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        mFilter.addAction(App.Companion.ACTION_TASK_CHANGE)
    }

    override fun onCreatePerformerMenu(context: Context?): PerformerMenu? {
        return PerformerMenu(getContext(), SelectionCallback(getActivity(), this))
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(mReceiver, mFilter)
        updateTasks()
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(mReceiver)
    }

    override fun onSortingOptions(options: MutableMap<String, Int>) {
        options[getString(R.string.text_sortByDate)] = EditableListAdapter.Companion.MODE_SORT_BY_DATE
        options[getString(R.string.text_sortBySize)] = EditableListAdapter.Companion.MODE_SORT_BY_SIZE
    }

    override fun onGroupingOptions(options: MutableMap<String?, Int?>) {
        options[getString(R.string.text_groupByNothing)] = GroupEditableListAdapter.Companion.MODE_GROUP_BY_NOTHING
        options[getString(R.string.text_groupByDate)] = GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_transfers)
    }

    override fun getIconRes(): Int {
        return R.drawable.ic_swap_vert_white_24dp
    }

    override fun performDefaultLayoutClick(
        holder: GroupViewHolder,
        `object`: TransferIndex
    ): Boolean {
        TransferDetailActivity.Companion.startInstance(requireActivity(), `object`.transfer)
        return true
    }

    fun updateTasks() {
        try {
            val tasks: List<FileTransferTask> = App.Companion.from(requireActivity()).getTaskListOf<FileTransferTask>(
                FileTransferTask::class.java
            )
            val activeTaskList: MutableList<Long> = ArrayList()
            for (task in tasks) if (task.transfer != null) activeTaskList.add(task.transfer.id)
            getAdapter().updateActiveList(activeTaskList)
            refreshList()
        } catch (ignored: IllegalStateException) {
        }
    }

    private class SelectionCallback(activity: Activity?, provider: PerformerEngineProvider) :
        EditableListFragment.SelectionCallback(activity, provider) {
        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu
        ): Boolean {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu)
            inflater.inflate(R.menu.action_mode_group, targetMenu)
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            val id = item.itemId
            val kuick = AppUtils.getKuick(activity)
            val engine = performerEngine ?: return false
            val genericList: List<Selectable> = ArrayList<Selectable>(engine.selectionList)
            val indexList: List<TransferIndex> = ListUtils.typedListOf<E, T>(genericList, TransferIndex::class.java)
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
                kuick.update<Device, TransferIndex>(changedList)
                kuick.broadcast()
            } else super.onPerformerMenuSelected(performerMenu, item)
            return false
        }
    }
}