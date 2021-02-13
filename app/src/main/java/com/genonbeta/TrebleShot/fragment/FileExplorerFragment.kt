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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter
import com.genonbeta.TrebleShot.app.Activity
import com.genonbeta.TrebleShot.dialog.FolderCreationDialog
import com.genonbeta.TrebleShot.dialog.FolderCreationDialog.OnFolderCreatedListener
import com.genonbeta.TrebleShot.ui.callback.IconProvider
import com.genonbeta.android.framework.io.DocumentFile
import com.google.android.material.snackbar.Snackbar
import java.io.File
import java.util.*

/**
 * Created by: veli
 * Date: 5/30/17 10:47 AM
 */
class FileExplorerFragment : FileListFragment(), Activity.OnBackPressedListener, IconProvider {
    override val iconRes: Int = R.drawable.ic_folder_white_24dp

    lateinit var pathView: RecyclerView
        private set

    private lateinit var pathAdapter: FilePathResolverRecyclerAdapter
        private set

    private var requestedPath: DocumentFile? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        layoutResId = R.layout.layout_file_explorer
        dividerResId = R.id.fragment_fileexplorer_separator
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        pathView = view.findViewById(R.id.fragment_fileexplorer_pathresolver)
        pathAdapter = FilePathResolverRecyclerAdapter(requireContext())
        val layoutManager = LinearLayoutManager(
            activity, LinearLayoutManager.HORIZONTAL,
            false
        )
        layoutManager.stackFromEnd = true
        pathView.layoutManager = layoutManager
        pathView.setHasFixedSize(true)
        pathView.adapter = pathAdapter
        pathAdapter.clickListener = object : PathResolverRecyclerAdapter.OnClickListener<DocumentFile?> {
            override fun onClick(holder: PathResolverRecyclerAdapter.Holder<DocumentFile?>) {
                goPath(holder.index.data)
            }
        }
        if (requestedPath != null) requestPath(requestedPath)
    }

    override fun onBackPressed(): Boolean {
        val path = adapter.path ?: return false
        val parentFile = getReadableFolder(path)
        if (parentFile == null || File.separator == parentFile.getName()) goPath(null) else goPath(parentFile)
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.actions_file_explorer, menu)
    }

    override fun onListRefreshed() {
        super.onListRefreshed()
        pathAdapter.goTo(adapter.path)
        pathAdapter.notifyDataSetChanged()
        if (pathAdapter.itemCount > 0) pathView.smoothScrollToPosition(pathAdapter.itemCount - 1)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.actions_file_explorer_create_folder) {
            val path = adapter.path
            if (path != null && path.canWrite()) {
                FolderCreationDialog(
                    requireContext(),
                    path,
                    object : OnFolderCreatedListener {
                        override fun onFolderCreated(directoryFile: DocumentFile?) {
                            refreshList()
                        }
                    }
                ).show()
            } else Snackbar.make(
                listView, R.string.mesg_currentPathUnavailable, Snackbar.LENGTH_SHORT
            ).show()
        } else return super.onOptionsItemSelected(item)
        return true
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_fileExplorer)
    }

    fun requestPath(file: DocumentFile?) {
        if (!isAdded) {
            requestedPath = file
            return
        }
        requestedPath = null
        goPath(file)
    }

    private inner class FilePathResolverRecyclerAdapter(
        context: Context,
    ) : PathResolverRecyclerAdapter<DocumentFile?>(context) {
        override fun onFirstItem(): Index<DocumentFile?> {
            return Index(context.getString(R.string.text_home), null, R.drawable.ic_home_white_24dp)
        }

        fun goTo(file: DocumentFile?) {
            val pathIndex = ArrayList<Index<DocumentFile?>>()
            var currentFile = file
            while (currentFile != null) {
                val index: Index<DocumentFile?> = Index(currentFile.getName(), currentFile)
                pathIndex.add(index)
                currentFile = currentFile.getParentFile()
                if (currentFile == null && "." == index.title)
                    index.title = getString(R.string.text_fileRoot)
            }
            initAdapter()

            synchronized(list) {
                while (pathIndex.size != 0) {
                    val currentStage = pathIndex.size - 1
                    list.add(pathIndex[currentStage])
                    pathIndex.removeAt(currentStage)
                }
            }
        }
    }

    companion object {
        val TAG = FileExplorerFragment::class.java.simpleName
        fun getReadableFolder(documentFile: DocumentFile): DocumentFile? {
            val parent = documentFile.getParentFile() ?: return null
            return if (parent.canRead()) parent else getReadableFolder(parent)
        }
    }
}