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
import android.net.Uri
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
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.io.File
import java.lang.Exception
import java.util.ArrayList

open class TransferItemListFragment :
    GroupEditableListFragment<GenericItem?, GroupViewHolder?, TransferItemListAdapter?>(), TitleProvider,
    OnBackPressedListener {
    private var mTransfer: Transfer? = null
    private var mIndex: TransferIndex? = null
    private var mLastKnownPath: String? = null
    private val mIntentFilter = IntentFilter()
    private val mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: BroadcastData = KuickDb.toData(intent)
                if (Kuick.Companion.TABLE_TRANSFERITEM == data.tableName || Kuick.Companion.TABLE_TRANSFER == data.tableName) refreshList()
            } else if (ChangeSaveDirectoryTask.Companion.ACTION_SAVE_PATH_CHANGED == intent.action && intent.hasExtra(
                    ChangeSaveDirectoryTask.Companion.EXTRA_TRANSFER
                )
            ) {
                val transfer: Transfer = intent.getParcelableExtra(ChangeSaveDirectoryTask.Companion.EXTRA_TRANSFER)
                if (transfer != null && transfer.equals(mTransfer)) createSnackbar(R.string.mesg_pathSaved).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilteringSupported(true)
        setDefaultOrderingCriteria(EditableListAdapter.Companion.MODE_SORT_ORDER_ASCENDING)
        setDefaultSortingCriteria(EditableListAdapter.Companion.MODE_SORT_BY_NAME)
        setDefaultGroupingCriteria(TransferItemListAdapter.Companion.MODE_GROUP_BY_DEFAULT)
        mIntentFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        mIntentFilter.addAction(ChangeSaveDirectoryTask.Companion.ACTION_SAVE_PATH_CHANGED)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListAdapter(TransferItemListAdapter(this))
        setEmptyListImage(R.drawable.ic_compare_arrows_white_24dp)
        val args: Bundle = getArguments()
        if (args != null && args.containsKey(ARG_TRANSFER_ID)) {
            goPath(
                args.getString(ARG_PATH), args.getLong(ARG_TRANSFER_ID), args.getString(ARG_DEVICE_ID),
                args.getString(ARG_TYPE)
            )
        }
    }

    override fun onCreatePerformerMenu(context: Context?): PerformerMenu? {
        return PerformerMenu(context, SelectionCallback(getActivity(), this))
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(mReceiver, mIntentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(mReceiver)
    }

    override fun onGridSpanSize(viewType: Int, currentSpanSize: Int): Int {
        return if (viewType == GroupEditableListAdapter.Companion.VIEW_TYPE_REPRESENTATIVE) currentSpanSize else super.onGridSpanSize(
            viewType,
            currentSpanSize
        )
    }

    protected override fun onListRefreshed() {
        super.onListRefreshed()
        val pathOnTrial: String = getAdapter().getPath()
        if (!(mLastKnownPath == null && getAdapter().getPath() == null)
            && mLastKnownPath != null && mLastKnownPath != pathOnTrial
        ) getListView().scrollToPosition(0)
        mLastKnownPath = pathOnTrial
    }

    override fun onBackPressed(): Boolean {
        val path: String = getAdapter().getPath() ?: return false
        val slashPos = path.lastIndexOf(File.separator)
        goPath(if (slashPos == -1 && path.length > 0) null else path.substring(0, slashPos))
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == REQUEST_CHOOSE_FOLDER && data.hasExtra(
                FilePickerActivity.Companion.EXTRA_CHOSEN_PATH
            )
        ) {
            val selectedPath = data.getParcelableExtra<Uri>(FilePickerActivity.Companion.EXTRA_CHOSEN_PATH)
            if (selectedPath == null) {
                createSnackbar(R.string.mesg_somethingWentWrong).show()
            } else if (selectedPath.toString() == getTransfer()!!.savePath) {
                createSnackbar(R.string.mesg_pathSameError).show()
            } else {
                val task = ChangeSaveDirectoryTask(mTransfer, selectedPath)
                AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.ques_checkOldFiles)
                    .setMessage(R.string.text_checkOldFiles)
                    .setNeutralButton(R.string.butn_cancel, null)
                    .setNegativeButton(R.string.butn_skip) { dialogInterface: DialogInterface?, i: Int ->
                        App.Companion.run<ChangeSaveDirectoryTask>(
                            requireActivity(),
                            task.setSkipMoving(true)
                        )
                    }
                    .setPositiveButton(R.string.butn_proceed) { dialogInterface: DialogInterface?, i: Int ->
                        App.Companion.run<ChangeSaveDirectoryTask>(
                            requireActivity(),
                            task
                        )
                    }
                    .show()
            }
        }
    }

    fun changeSavePath(initialPath: String?) {
        startActivityForResult(
            Intent(getActivity(), FilePickerActivity::class.java)
                .setAction(FilePickerActivity.Companion.ACTION_CHOOSE_DIRECTORY)
                .putExtra(FilePickerActivity.Companion.EXTRA_START_PATH, initialPath)
                .putExtra(FilePickerActivity.Companion.EXTRA_ACTIVITY_TITLE, getString(R.string.butn_saveTo)),
            REQUEST_CHOOSE_FOLDER
        )
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_transfers)
    }

    fun getTransfer(): Transfer? {
        if (mTransfer == null) {
            val arguments: Bundle = getArguments()
            if (arguments != null) {
                mTransfer = Transfer(arguments.getLong(ARG_TRANSFER_ID, -1))
                try {
                    AppUtils.getKuick(getContext()).reconstruct(mTransfer)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return mTransfer
    }

    fun getIndex(): TransferIndex? {
        if (mIndex == null) mIndex = TransferIndex(getTransfer())
        return mIndex
    }

    fun goPath(path: String?, transferId: Long, deviceId: String?, type: String?) {
        if (deviceId != null && type != null) try {
            val member = LoadedMember(transferId, deviceId, TransferItem.Type.valueOf(type))
            AppUtils.getKuick(getContext()).reconstruct<Transfer, LoadedMember>(member)
            Transfers.loadMemberInfo(getContext(), member)
            getAdapter().setMember(member)
        } catch (ignored: Exception) {
        }
        goPath(path, transferId)
    }

    fun goPath(path: String?, transferId: Long) {
        getAdapter().setTransferId(transferId)
        goPath(path)
    }

    fun goPath(path: String?) {
        getAdapter().setPath(path)
        refreshList()
    }

    override fun performDefaultLayoutClick(
        holder: GroupViewHolder,
        `object`: GenericItem
    ): Boolean {
        if (`object` is DetailsTransferFolder) {
            val list: List<LoadedMember?>? = Transfers.loadMemberList(getContext(), getTransfer()!!.id, null)
            if (list!!.size > 0) {
                val listClickListener = DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    getAdapter().setMember(list[which])
                    getAdapter().setPath(getAdapter().getPath())
                    refreshList()
                }
                val noLimitListener = DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    getAdapter().setMember(null)
                    getAdapter().setPath(getAdapter().getPath())
                    refreshList()
                }
                val dialog = ChooseMemberDialog(requireActivity(), list, listClickListener)
                dialog.setTitle(R.string.text_limitTo)
                    .setNeutralButton(R.string.butn_showAll, noLimitListener)
                    .show()
            } else createSnackbar(R.string.text_noDeviceForTransfer).show()
        } else if (`object` is StorageStatusItem) {
            val statusItem: StorageStatusItem = `object` as StorageStatusItem
            if (statusItem.hasIssues(getAdapter())) AlertDialog.Builder(requireActivity())
                .setMessage(getString(R.string.mesg_notEnoughSpace))
                .setNegativeButton(R.string.butn_close, null)
                .setPositiveButton(
                    R.string.butn_saveTo,
                    DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int -> changeSavePath(statusItem.directory) })
                .show() else changeSavePath(statusItem.directory)
        } else if (`object` is TransferFolder) {
            getAdapter().setPath(`object`.directory)
            refreshList()
            AppUtils.showFolderSelectionHelp<GenericItem>(this)
        } else TransferInfoDialog(requireActivity(), getIndex(), `object`, getAdapter().getDeviceId()).show()
        return true
    }

    override fun setItemSelected(holder: GroupViewHolder): Boolean {
        return if (getAdapterImpl().getItem(holder.getAdapterPosition()) is TransferFolder) false else super.setItemSelected(
            holder
        )
    }

    private class SelectionCallback(activity: Activity?, provider: PerformerEngineProvider) :
        EditableListFragment.SelectionCallback(activity, provider) {
        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu
        ): Boolean {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu)
            inflater.inflate(R.menu.action_mode_transfer, targetMenu)
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            val id = item.itemId
            val engine = performerEngine ?: return false
            val genericList: List<Selectable> = ArrayList<Selectable>(engine.selectionList)
            val selectionList: MutableList<GenericItem> = ArrayList<GenericItem>()
            for (selectable in genericList) if (selectable is GenericItem) selectionList.add(selectable as GenericItem)
            return if (id == R.id.action_mode_transfer_delete) {
                DialogUtils.showRemoveTransferObjectListDialog(activity, selectionList)
                true
            } else super.onPerformerMenuSelected(performerMenu, item)
        }
    }

    companion object {
        const val TAG = "TransferListFragment"
        const val ARG_DEVICE_ID = "argDeviceId"
        const val ARG_TRANSFER_ID = "argGroupId"
        const val ARG_TYPE = "argType"
        const val ARG_PATH = "argPath"
        const val REQUEST_CHOOSE_FOLDER = 1
    }
}