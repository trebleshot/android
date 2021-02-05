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
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter
import com.genonbeta.android.framework.util.actionperformer.SelectableNotFoundException
import com.genonbeta.android.framework.util.actionperformer.CouldNotAlterException
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.TrebleShot.util.SelectionUtils
import com.genonbeta.TrebleShot.dialog.SelectionEditorDialog
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.io.DocumentFile
import java.io.File
import java.util.ArrayList

/**
 * Created by: veli
 * Date: 5/30/17 10:47 AM
 */
class FileExplorerFragment : FileListFragment(), OnBackPressedListener, IconProvider {
    private var mPathView: RecyclerView? = null
    private var mPathAdapter: FilePathResolverRecyclerAdapter? = null
    private var mRequestedPath: DocumentFile? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        setLayoutResId(R.layout.layout_file_explorer)
        setDividerView(R.id.fragment_fileexplorer_separator)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPathView = view.findViewById(R.id.fragment_fileexplorer_pathresolver)
        mPathAdapter = FilePathResolverRecyclerAdapter(context)
        val layoutManager = LinearLayoutManager(
            activity, LinearLayoutManager.HORIZONTAL,
            false
        )
        layoutManager.setStackFromEnd(true)
        mPathView.setLayoutManager(layoutManager)
        mPathView.setHasFixedSize(true)
        mPathView.setAdapter(mPathAdapter)
        mPathAdapter.setOnClickListener(PathResolverRecyclerAdapter.OnClickListener { holder: PathResolverRecyclerAdapter.Holder<DocumentFile?> ->
            goPath(
                holder.index.`object`
            )
        })
        if (mRequestedPath != null) requestPath(mRequestedPath)
    }

    override fun onBackPressed(): Boolean {
        val path = adapter.path ?: return false
        val parentFile = getReadableFolder(path)
        if (parentFile == null || File.separator == parentFile.name) goPath(null) else goPath(parentFile)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_file_explorer, menu)
    }

    override fun onListRefreshed() {
        super.onListRefreshed()
        mPathAdapter!!.goTo(adapter.path)
        mPathAdapter.notifyDataSetChanged()
        if (mPathAdapter.getItemCount() > 0) mPathView!!.smoothScrollToPosition(mPathAdapter.getItemCount() - 1)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.actions_file_explorer_create_folder) {
            if (adapter.path != null && adapter.path.canWrite()) FolderCreationDialog(
                context,
                adapter.path,
                OnFolderCreatedListener { directoryFile: DocumentFile? -> refreshList() }).show() else Snackbar.make(
                listView, R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT
            ).show()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun getIconRes(): Int {
        return R.drawable.ic_folder_white_24dp
    }

    fun getPathView(): RecyclerView? {
        return mPathView
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_fileExplorer)
    }

    fun requestPath(file: DocumentFile?) {
        if (!isAdded) {
            mRequestedPath = file
            return
        }
        mRequestedPath = null
        goPath(file)
    }

    private inner class FilePathResolverRecyclerAdapter(context: Context?) :
        PathResolverRecyclerAdapter<DocumentFile?>(context) {
        override fun onFirstItem(): Index<DocumentFile?> {
            return Index(
                context.getString(R.string.text_home), R.drawable.ic_home_white_24dp, null
            )
        }

        fun goTo(file: DocumentFile?) {
            val pathIndex = ArrayList<Index<DocumentFile>>()
            var currentFile = file
            while (currentFile != null) {
                val index = Index(currentFile.name, currentFile)
                pathIndex.add(index)
                currentFile = currentFile.parentFile
                if (currentFile == null && "." == index.title) index.title = getString(R.string.text_fileRoot)
            }
            initAdapter()
            synchronized(getList()) {
                while (pathIndex.size != 0) {
                    val currentStage = pathIndex.size - 1
                    getList().add(pathIndex[currentStage])
                    pathIndex.removeAt(currentStage)
                }
            }
        }
    }

    companion object {
        val TAG = FileExplorerFragment::class.java.simpleName
        fun getReadableFolder(documentFile: DocumentFile): DocumentFile? {
            val parent = documentFile.parentFile ?: return null
            return if (parent.canRead()) parent else getReadableFolder(parent)
        }
    }
}