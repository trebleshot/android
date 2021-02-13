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
import android.net.Uri
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.activity.FilePickerActivity
import com.genonbeta.TrebleShot.adapter.TransferItemListAdapter
import com.genonbeta.TrebleShot.adapter.TransferItemListAdapter.*
import com.genonbeta.TrebleShot.app.Activity.OnBackPressedListener
import com.genonbeta.TrebleShot.app.EditableListFragment
import com.genonbeta.TrebleShot.app.GroupEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.LoadedMember
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferIndex
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.dialog.ChooseMemberDialog
import com.genonbeta.TrebleShot.dialog.DialogUtils
import com.genonbeta.TrebleShot.dialog.TransferInfoDialog
import com.genonbeta.TrebleShot.task.ChangeSaveDirectoryTask
import com.genonbeta.TrebleShot.ui.callback.TitleProvider
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.util.Transfers
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.database.exception.ReconstructionFailedException
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.android.framework.util.actionperformer.Selectable
import java.io.File
import java.util.*

open class TransferItemListFragment :
    GroupEditableListFragment<GenericItem, GroupViewHolder, TransferItemListAdapter>(),
    TitleProvider, OnBackPressedListener {
    private lateinit var transfer: Transfer

    private lateinit var index: TransferIndex

    private var lastKnownPath: String? = null

    private val intentFilter = IntentFilter()

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (KuickDb.ACTION_DATABASE_CHANGE == intent.action) {
                val data: KuickDb.BroadcastData = KuickDb.toData(intent)
                if (Kuick.TABLE_TRANSFERITEM == data.tableName || Kuick.TABLE_TRANSFER == data.tableName) refreshList()
            } else if (ChangeSaveDirectoryTask.ACTION_SAVE_PATH_CHANGED == intent.action && intent.hasExtra(
                    ChangeSaveDirectoryTask.EXTRA_TRANSFER
                )
            ) {
                val transfer: Transfer = intent.getParcelableExtra(ChangeSaveDirectoryTask.EXTRA_TRANSFER)
                if (transfer == this@TransferItemListFragment.transfer) createSnackbar(R.string.mesg_pathSaved)?.show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isFilteringSupported = true
        defaultOrderingCriteria = EditableListAdapter.MODE_SORT_ORDER_ASCENDING
        defaultSortingCriteria = EditableListAdapter.MODE_SORT_BY_NAME
        defaultGroupingCriteria = TransferItemListAdapter.MODE_GROUP_BY_DEFAULT
        intentFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        intentFilter.addAction(ChangeSaveDirectoryTask.ACTION_SAVE_PATH_CHANGED)

        transfer = Transfer(arguments?.getLong(ARG_TRANSFER_ID, -1) ?: -1).also {
            try {
                AppUtils.getKuick(requireContext())
            } catch (ignored: ReconstructionFailedException) {
            }
        }

        index = TransferIndex(transfer)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TransferItemListAdapter(this)
        emptyListImageView.setImageResource(R.drawable.ic_compare_arrows_white_24dp)

        arguments?.let { args ->
            if (args.containsKey(ARG_TRANSFER_ID)) goPath(
                args.getString(ARG_PATH), args.getLong(ARG_TRANSFER_ID), args.getString(ARG_DEVICE_ID),
                args.getString(ARG_TYPE)
            )
        }
    }

    override fun onCreatePerformerMenu(context: Context): PerformerMenu {
        return PerformerMenu(context, SelectionCallback(requireActivity(), this))
    }

    override fun onResume() {
        super.onResume()
        requireContext().registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireContext().unregisterReceiver(receiver)
    }

    override fun onGridSpanSize(viewType: Int, currentSpanSize: Int): Int {
        return if (viewType == GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE) {
            currentSpanSize
        } else super.onGridSpanSize(viewType, currentSpanSize)
    }

    override fun onListRefreshed() {
        super.onListRefreshed()
        val path = adapter.path
        if (!(lastKnownPath == null && path == null) && lastKnownPath != null && lastKnownPath != path) {
            listView.scrollToPosition(0)
        }
        lastKnownPath = path
    }

    override fun onBackPressed(): Boolean {
        val path = adapter.path ?: return false
        val slashPos = path.lastIndexOf(File.separator)
        goPath(if (slashPos == -1 && path.isNotEmpty()) null else path.substring(0, slashPos))
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (data != null && resultCode == Activity.RESULT_OK && requestCode == REQUEST_CHOOSE_FOLDER
            && data.hasExtra(FilePickerActivity.EXTRA_CHOSEN_PATH)
        ) {
            val selectedPath = data.getParcelableExtra<Uri>(FilePickerActivity.EXTRA_CHOSEN_PATH)
            if (selectedPath == null) {
                createSnackbar(R.string.mesg_somethingWentWrong)?.show()
            } else if (selectedPath.toString() == transfer.savePath) {
                createSnackbar(R.string.mesg_pathSameError)?.show()
            } else {
                val task = ChangeSaveDirectoryTask(transfer, selectedPath)
                AlertDialog.Builder(requireActivity())
                    .setTitle(R.string.ques_checkOldFiles)
                    .setMessage(R.string.text_checkOldFiles)
                    .setNeutralButton(R.string.butn_cancel, null)
                    .setNegativeButton(R.string.butn_skip) { dialogInterface: DialogInterface?, i: Int ->
                        App.run(requireActivity(), task.also { it.skipMoving = true })
                    }
                    .setPositiveButton(R.string.butn_proceed) { dialogInterface: DialogInterface?, i: Int ->
                        App.run(requireActivity(), task)
                    }
                    .show()
            }
        }
    }

    fun changeSavePath(initialPath: String?) {
        startActivityForResult(
            Intent(requireActivity(), FilePickerActivity::class.java)
                .setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY)
                .putExtra(FilePickerActivity.EXTRA_START_PATH, initialPath)
                .putExtra(FilePickerActivity.EXTRA_ACTIVITY_TITLE, getString(R.string.butn_saveTo)),
            REQUEST_CHOOSE_FOLDER
        )
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_transfers)
    }

    fun goPath(path: String?, transferId: Long, deviceId: String?, type: String?) {
        if (deviceId != null && type != null) try {
            val member = LoadedMember(transferId, deviceId, TransferItem.Type.valueOf(type))
            AppUtils.getKuick(requireContext()).reconstruct(member)
            Transfers.loadMemberInfo(requireContext(), member)
            adapter.member = member
        } catch (ignored: Exception) {
        }
        goPath(path, transferId)
    }

    fun goPath(path: String?, transferId: Long) {
        adapter.setTransferId(transferId)
        goPath(path)
    }

    fun goPath(path: String?) {
        adapter.path = path
        refreshList()
    }

    override fun performDefaultLayoutClick(
        holder: GroupViewHolder,
        item: GenericItem,
    ): Boolean {
        if (item is DetailsTransferFolder) {
            val list = Transfers.loadMemberList(requireContext(), transfer.id, null)
            if (list.isNotEmpty()) {
                val listClickListener = DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    adapter.member = list[which]
                    adapter.path = adapter.path
                    refreshList()
                }
                val noLimitListener = DialogInterface.OnClickListener { dialog: DialogInterface?, which: Int ->
                    adapter.member = null
                    adapter.path = adapter.path
                    refreshList()
                }
                ChooseMemberDialog(requireActivity(), list, listClickListener)
                    .setTitle(R.string.text_limitTo)
                    .setNeutralButton(R.string.butn_showAll, noLimitListener)
                    .show()
            } else createSnackbar(R.string.text_noDeviceForTransfer)?.show()
        } else if (item is StorageStatusItem) {
            if (item.hasIssues(adapter)) {
                AlertDialog.Builder(requireActivity())
                    .setMessage(getString(R.string.mesg_notEnoughSpace))
                    .setNegativeButton(R.string.butn_close, null)
                    .setPositiveButton(R.string.butn_saveTo) { dialog: DialogInterface?, which: Int ->
                        changeSavePath(item.directory)
                    }
                    .show()
            } else changeSavePath(item.directory)
        } else if (item is TransferFolder) {
            adapter.path = item.directoryLocal
            refreshList()
            AppUtils.showFolderSelectionHelp(this)
        } else TransferInfoDialog(requireActivity(), index, item, adapter.getDeviceId()).show()
        return true
    }

    override fun setItemSelected(holder: GroupViewHolder): Boolean {
        return if (adapter.getItem(holder.adapterPosition) is TransferFolder) {
            false
        } else super.setItemSelected(holder)
    }

    private class SelectionCallback(activity: Activity, provider: PerformerEngineProvider) :
        EditableListFragment.SelectionCallback(activity, provider) {
        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu,
        ): Boolean {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu)
            inflater.inflate(R.menu.action_mode_transfer, targetMenu)
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            val id = item.itemId
            val engine = getPerformerEngine() ?: return false
            val genericList: List<Selectable> = ArrayList(engine.getSelectionList())
            val selectionList: MutableList<GenericItem> = ArrayList<GenericItem>()
            for (selectable in genericList) {
                if (selectable is GenericItem) selectionList.add(selectable)
            }
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