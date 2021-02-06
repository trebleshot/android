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
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.ui.callback.IconProvider
import com.genonbeta.android.framework.io.DocumentFile
import java.io.File
import java.util.*

/**
 * Created by: veli
 * Date: 5/30/17 10:47 AM
 */
class FileExplorerFragment : FileListFragment(), Activity.OnBackPressedListener, IconProvider {
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
                holder.index.data
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
                if (currentFile == null && "." == index.title)
                    index.title = getString(R.string.text_fileRoot)
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