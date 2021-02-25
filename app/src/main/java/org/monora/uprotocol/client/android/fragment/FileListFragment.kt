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
package org.monora.uprotocol.client.android.fragment

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.FileListAdapter
import org.monora.uprotocol.client.android.adapter.FileListAdapter.FileHolder
import org.monora.uprotocol.client.android.app.ListingFragment
import org.monora.uprotocol.client.android.app.ListingFragmentBase
import org.monora.uprotocol.client.android.database.Kuick
import org.monora.uprotocol.client.android.dialog.FileRenameDialog
import org.monora.uprotocol.client.android.dialogimport.FileDeletionDialog
import org.monora.uprotocol.client.android.ui.callback.SharingPerformerMenuCallback
import org.monora.uprotocol.client.android.util.AppUtils
import com.genonbeta.android.database.KuickDb
import com.genonbeta.android.framework.io.DocumentFile
import com.genonbeta.android.framework.ui.PerformerMenu
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider
import com.genonbeta.android.framework.util.actionperformer.SelectionModel
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import com.google.android.material.snackbar.Snackbar
import org.monora.uprotocol.client.android.model.ContentModel
import java.io.FileNotFoundException

abstract class FileListFragment : ListingFragment<FileHolder, ViewHolder, FileListAdapter>() {
    private var lastKnownPath: DocumentFile? = null

    private var pathChangedListener: OnPathChangedListener? = null

    private val intentFilter = IntentFilter()

    private val receiver = object : BroadcastReceiver() {
        private var updateSnackbar: Snackbar? = null
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_FILE_LIST_CHANGED == intent.action && intent.hasExtra(EXTRA_FILE_PARENT)) {
                try {
                    val parentUri: Uri? = intent.getParcelableExtra(EXTRA_FILE_PARENT)
                    if (parentUri == null && adapter.path == null) {
                        refreshList()
                    } else if (parentUri != null) {
                        val parentFile = Files.fromUri(requireContext(), parentUri)
                        val path = adapter.path

                        if (path != null && parentFile.getUri() == path.getUri())
                            refreshList()
                        else if (intent.hasExtra(EXTRA_FILE_NAME)) {
                            if (updateSnackbar == null) updateSnackbar =
                                createSnackbar(R.string.mesg_newFilesReceived)
                            updateSnackbar?.setText(
                                getString(
                                    R.string.mesg_fileReceived,
                                    intent.getStringExtra(EXTRA_FILE_NAME)
                                )
                            )
                                ?.setAction(R.string.butn_show) { _: View? -> goPath(parentFile) }
                                ?.show()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (ACTION_FILE_RENAME_COMPLETED == intent.action)
                refreshList()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        filteringSupported = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = FileListAdapter(this)
        emptyListImageView.setImageResource(R.drawable.ic_folder_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmptyFiles)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        intentFilter.addAction(ACTION_FILE_LIST_CHANGED)
        intentFilter.addAction(ACTION_FILE_RENAME_COMPLETED)
        intentFilter.addAction(KuickDb.ACTION_DATABASE_CHANGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK)
            if (requestCode == REQUEST_WRITE_ACCESS) {
                val pathUri = data!!.data
                if (Build.VERSION.SDK_INT >= 21 && pathUri != null && context != null) {
                    context?.contentResolver?.takePersistableUriPermission(
                        pathUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )

                    // TODO: 2/25/21 Save the mounted dir
                }
            }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_file_list, menu)
        val mountDirectory = menu.findItem(R.id.actions_file_list_mount_directory)
        if (Build.VERSION.SDK_INT >= 21 && mountDirectory != null) mountDirectory.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val path = adapter.path
        if (id == R.id.actions_file_list_mount_directory) {
            requestMountStorage()
        } else if (id == R.id.actions_file_list_toggle_shortcut && path != null) {
            // FIXME: 2/20/21 Shortcut item
            //shortcutItem(this, FileHolder(requireContext(), path))
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val shortcutMenuItem = menu.findItem(R.id.actions_file_list_toggle_shortcut)
        if (shortcutMenuItem != null) {
            val path = adapter.path

            shortcutMenuItem.isEnabled = path != null
            if (path != null) try {
                // FIXME: 2/20/21 Reconstruct item
                //AppUtils.getKuick(requireContext()).reconstruct(FileHolder(requireContext(), path))
                shortcutMenuItem.setTitle(R.string.butn_removeShortcut)
            } catch (e: Exception) {
                shortcutMenuItem.setTitle(R.string.butn_addShortcut)
            }
        }
    }

    override fun onCreatePerformerMenu(context: Context): PerformerMenu? {
        return PerformerMenu(context, SelectionCallback(this, this))
    }

    override fun onResume() {
        super.onResume()
        requireActivity().registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        requireActivity().unregisterReceiver(receiver)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        adapter.path?.let {
            outState.putString(EXTRA_FILE_LOCATION, it.getUri().toString())
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState != null && savedInstanceState.containsKey(EXTRA_FILE_LOCATION)) {
            try {
                goPath(Files.fromUri(requireContext(), Uri.parse(savedInstanceState.getString(EXTRA_FILE_LOCATION))))
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            }
        }
    }

    override fun onListRefreshed() {
        super.onListRefreshed()

        // If the current path is different from the older one, move the scroll position
        // to the top.
        val pathOnTrial: DocumentFile? = adapter.path
        if (!(lastKnownPath == null && adapter.path == null)
            && lastKnownPath != null && lastKnownPath != pathOnTrial
        ) listView.scrollToPosition(0)
        lastKnownPath = pathOnTrial
    }

    fun goPath(file: DocumentFile?) {
        if (file != null && !file.canRead()) {
            createSnackbar(R.string.mesg_errorReadFolder, file.getName())?.show()
            return
        }

        pathChangedListener?.onPathChanged(file)
        adapter.path = file
        refreshList()
    }

    override fun performDefaultLayoutClick(holder: ViewHolder, target: FileHolder): Boolean {
        // FIXME: 2/20/21 Fixe directory selection
        if (false) {//target.file.isDirectory()) {
            //goPath(target.file)
            AppUtils.showFolderSelectionHelp(this)
        } else performLayoutClickOpen(holder, target)
        return true
    }

    fun requestMountStorage() {
        if (Build.VERSION.SDK_INT < 21) return
        startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), REQUEST_WRITE_ACCESS)
        Toast.makeText(activity, R.string.mesg_mountDirectoryHelp, Toast.LENGTH_LONG).show()
    }

    interface OnPathChangedListener {
        fun onPathChanged(file: DocumentFile?)
    }

    private class SelectionCallback(
        private val fragment: FileListFragment,
        provider: PerformerEngineProvider,
    ) : SharingPerformerMenuCallback(fragment.requireActivity(), provider) {
        override fun onPerformerMenuList(
            performerMenu: PerformerMenu,
            inflater: MenuInflater,
            targetMenu: Menu,
        ): Boolean {
            super.onPerformerMenuList(performerMenu, inflater, targetMenu)
            inflater.inflate(R.menu.action_mode_file, targetMenu)
            return true
        }

        override fun onPerformerMenuSelected(performerMenu: PerformerMenu, item: MenuItem): Boolean {
            val performerEngine = getPerformerEngine() ?: return false
            val selectionModelList: List<SelectionModel> = ArrayList<SelectionModel>(performerEngine.getSelectionList())
            val fileList: MutableList<FileHolder> = ArrayList()

            for (selectionModel in selectionModelList)
                if (selectionModel is FileHolder)
                    fileList.add(selectionModel)

            return if (fileList.size <= 0 || !handleEditingAction(item, fragment, fileList))
                super.onPerformerMenuSelected(performerMenu, item)
            else
                true
        }
    }

    companion object {
        val TAG = FileListFragment::class.java.simpleName
        const val REQUEST_WRITE_ACCESS = 264
        const val ACTION_FILE_LIST_CHANGED = "org.monora.uprotocol.client.android.action.FILE_LIST_CHANGED"
        const val ACTION_FILE_RENAME_COMPLETED = "org.monora.uprotocol.client.android.action.FILE_RENAME_COMPLETED"
        const val EXTRA_FILE_PARENT = "extraPath"
        const val EXTRA_FILE_NAME = "extraFile"
        const val EXTRA_FILE_LOCATION = "extraFileLocation"

        fun handleEditingAction(
            item: MenuItem, fragment: FileListFragment,
            selectedItemList: List<FileHolder>,
        ): Boolean {
            val id = item.itemId
            val activity = fragment.activity

            if (activity != null && id == R.id.action_mode_file_delete) {
                FileDeletionDialog(activity, selectedItemList).show()
            } else if (activity != null && id == R.id.action_mode_file_rename) {
                FileRenameDialog(activity, selectedItemList).show()
            } else if (id == R.id.action_mode_file_copy_here) {
                // FIXME: 8/17/20 Sharing with third-party has problems. This should be fixed.
                /*} else if (id == R.id.action_mode_file_share_all_apps) {
            Intent intent = new Intent(selectedItemList.size() > 1 ? Intent.ACTION_SEND_MULTIPLE : Intent.ACTION_SEND)
                    .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            if (selectedItemList.size() > 1) {
                MIMEGrouper mimeGrouper = new MIMEGrouper();
                ArrayList<Uri> uriList = new ArrayList<>();

                for (FileHolder sharedItem : selectedItemList) {

                    uriList.add();

                    if (!mimeGrouper.isLocked())
                        mimeGrouper.process(sharedItem.mimeType);
                }

                intent.setType(mimeGrouper.toString())
                        .putParcelableArrayListExtra(Intent.EXTRA_STREAM, uriList);
            } else if (selectedItemList.size() == 1) {
                Shareable sharedItem = selectedItemList.get(0);

                intent.setType(sharedItem.mimeType)
                        .putExtra(Intent.EXTRA_STREAM, sharedItem.uri);
            }

            try {
                fragment.requireActivity().startActivity(Intent.createChooser(intent, fragment.getString(
                        R.string.text_fileShareAppChoose)));
                return true;
            } catch (ActivityNotFoundException e) {
                fragment.createSnackbar(R.string.mesg_noActivityFound, Toast.LENGTH_SHORT).show();
            } catch (Throwable e) {
                e.printStackTrace();
                fragment.createSnackbar(R.string.mesg_somethingWentWrong, Toast.LENGTH_SHORT).show();
            }*/
            } else return false
            return true
        }

        fun <T : ContentModel> shortcutItem(fragment: ListingFragmentBase<T>, holder: FileHolder) {
            // TODO: 2/25/21 Shortcut item
        }
    }
}