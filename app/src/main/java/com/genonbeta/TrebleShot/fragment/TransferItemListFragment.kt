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
import com.genonbeta.TrebleShot.R
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.TrebleShot.App
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import android.os.Bundle
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import android.net.Uri
import com.genonbeta.TrebleShot.app.EditableListFragment
import android.view.*
import androidx.appcompat.app.AlertDialog
import com.genonbeta.TrebleShot.dataobject.Transfer
import com.genonbeta.TrebleShot.dataobject.TransferItem
import com.genonbeta.TrebleShot.util.Transfers
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
                if (Kuick.TABLE_TRANSFERITEM == data.tableName || Kuick.TABLE_TRANSFER == data.tableName) refreshList()
            } else if (ChangeSaveDirectoryTask.ACTION_SAVE_PATH_CHANGED == intent.action && intent.hasExtra(
                    ChangeSaveDirectoryTask.EXTRA_TRANSFER
                )
            ) {
                val transfer: Transfer = intent.getParcelableExtra(ChangeSaveDirectoryTask.EXTRA_TRANSFER)
                if (transfer != null && transfer.equals(mTransfer)) createSnackbar(R.string.mesg_pathSaved).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilteringSupported(true)
        setDefaultOrderingCriteria(EditableListAdapter.MODE_SORT_ORDER_ASCENDING)
        setDefaultSortingCriteria(EditableListAdapter.MODE_SORT_BY_NAME)
        setDefaultGroupingCriteria(TransferItemListAdapter.MODE_GROUP_BY_DEFAULT)
        mIntentFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
        mIntentFilter.addAction(ChangeSaveDirectoryTask.ACTION_SAVE_PATH_CHANGED)
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
        return if (viewType == GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE) currentSpanSize else super.onGridSpanSize(
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
                FilePickerActivity.EXTRA_CHOSEN_PATH
            )
        ) {
            val selectedPath = data.getParcelableExtra<Uri>(FilePickerActivity.EXTRA_CHOSEN_PATH)
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
                        App.run<ChangeSaveDirectoryTask>(
                            requireActivity(),
                            task.setSkipMoving(true)
                        )
                    }
                    .setPositiveButton(R.string.butn_proceed) { dialogInterface: DialogInterface?, i: Int ->
                        App.run<ChangeSaveDirectoryTask>(
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
                .setAction(FilePickerActivity.ACTION_CHOOSE_DIRECTORY)
                .putExtra(FilePickerActivity.EXTRA_START_PATH, initialPath)
                .putExtra(FilePickerActivity.EXTRA_ACTIVITY_TITLE, getString(R.string.butn_saveTo)),
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