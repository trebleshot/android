/*
 * Copyright (C) 2021 Veli Tasalı
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

package org.monora.uprotocol.client.android.fragment.content

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.activity.SharingSelectionViewModel
import org.monora.uprotocol.client.android.adapter.FileAdapter
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListPathBinding
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.FilesViewModel
import java.io.File

@AndroidEntryPoint
class FileFragment : Fragment(R.layout.layout_file_fragment) {
    private val viewModel: FilesViewModel by viewModels()

    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = FileAdapter { fileModel, clickType ->
            when (clickType) {
                FileAdapter.ClickType.Default -> {
                    if (fileModel.file.isDirectory()) {
                        viewModel.requestPath(fileModel.file)
                    }
                }
                FileAdapter.ClickType.ToggleSelect -> {
                    selectionViewModel.setSelected(fileModel, fileModel.isSelected)
                }
            }
        }
        val emptyContentViewModel = EmptyContentViewModel()
        val pathRecyclerView = view.findViewById<RecyclerView>(R.id.pathRecyclerView)
        val pathAdapter = PathAdapter {
            viewModel.requestPath(it.file)
        }

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.text_listEmptyFiles)
        emptyView.emptyImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp)
        emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        pathAdapter.setHasStableIds(true)
        pathRecyclerView.adapter = pathAdapter

        viewModel.files.observe(viewLifecycleOwner) {
            it.forEach { model ->
                if (model is FileModel && selectionViewModel.contains(model)) model.isSelected = true
            }

            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }

        viewModel.path.observe(viewLifecycleOwner) {
            pathAdapter.submitList(it)
        }
    }
}

class PathContentViewModel(fileModel: FileModel) {
    val isRoot = fileModel.file.getUri() == ROOT_URI

    val title = fileModel.name()

    companion object {
        val ROOT_URI: Uri = Uri.fromFile(File("/"))
    }
}

class FilePathViewHolder constructor(
    private val clickListener: (FileModel) -> Unit,
    private var binding: ListPathBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(fileModel: FileModel) {
        binding.viewModel = PathContentViewModel(fileModel)
        binding.button.setOnClickListener {
            clickListener(fileModel)
        }
        binding.executePendingBindings()
    }
}

class PathAdapter(
    private val clickListener: (FileModel) -> Unit,
) : ListAdapter<FileModel, FilePathViewHolder>(FileModelItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilePathViewHolder {
        return FilePathViewHolder(
            clickListener,
            ListPathBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FilePathViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id()
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_PATH
    }

    companion object {
        const val VIEW_TYPE_PATH = 0
    }
}

class FileModelItemCallback : DiffUtil.ItemCallback<FileModel>() {
    override fun areItemsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: FileModel, newItem: FileModel): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}