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

package org.monora.uprotocol.client.android.fragment

import android.annotation.TargetApi
import android.content.DialogInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.os.bundleOf
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListFilePickerBinding
import org.monora.uprotocol.client.android.databinding.ListSectionTitleBinding
import org.monora.uprotocol.client.android.fragment.content.PathAdapter
import org.monora.uprotocol.client.android.itemcallback.ContentModelItemCallback
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.model.TitleSectionContentModel
import org.monora.uprotocol.client.android.viewholder.TitleSectionViewHolder
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.FilesViewModel
import org.monora.uprotocol.client.android.viewmodel.content.FileContentViewModel

@AndroidEntryPoint
class FilePickerFragment : Fragment(R.layout.layout_file_picker) {
    @TargetApi(19)
    private val addAccess = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        val context = context

        if (uri != null && context != null && Build.VERSION.SDK_INT >= 21) {
            viewModel.insertSafFolder(uri)
        }
    }

    private val args: FilePickerFragmentArgs by navArgs()

    private val viewModel: FilesViewModel by viewModels()

    private val createFolderDialog by lazy {
        val view = layoutInflater.inflate(R.layout.layout_create_folder, null, false)
        val editText = view.findViewById<EditText>(R.id.editText)

        AlertDialog.Builder(requireActivity())
            .setTitle(R.string.create_folder)
            .setView(view)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.create, null)
            .create().also { dialog ->
                dialog.setOnShowListener {
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                        val displayName = editText.text.toString().trim()
                        editText.error = if (displayName.isEmpty()) {
                            getString(R.string.error_empty_field)
                        } else if (viewModel.createFolder(displayName)) {
                            dialog.dismiss()
                            editText.text.clear()
                            null
                        } else {
                            getString(R.string.create_folder_failure)
                        }
                    }
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = FilePickerAdapter(args.selectionType) { fileModel, clickType ->
            when (clickType) {
                FilePickerAdapter.ClickType.Open -> {
                    viewModel.requestPath(fileModel.file)
                }
                FilePickerAdapter.ClickType.Select -> {
                    setFragmentResult(RESULT_FILE_PICKED, bundleOf(EXTRA_DOCUMENT_FILE to fileModel.file))
                    findNavController().navigateUp()
                }
            }
        }
        val emptyContentViewModel = EmptyContentViewModel()
        val pathRecyclerView = view.findViewById<RecyclerView>(R.id.pathRecyclerView)
        val pathSelectorButton = view.findViewById<View>(R.id.pathSelectorButton)
        val approveFolderButton = view.findViewById<FloatingActionButton>(R.id.approveFolderButton)
        val floatingViewsContainer = view.findViewById<CoordinatorLayout>(R.id.floatingViewsContainer)
        val pathAdapter = PathAdapter {
            viewModel.requestPath(it.file)
        }
        val nonWritableWarning = Snackbar.make(
            floatingViewsContainer, R.string.folder_not_writable, Snackbar.LENGTH_INDEFINITE
        )
        val pathsPopupMenu = PopupMenu(requireContext(), pathSelectorButton).apply {
            MenuCompat.setGroupDividerEnabled(menu, true)
        }

        approveFolderButton.visibility = if (args.selectionType == SelectionType.Folder) View.VISIBLE else View.GONE

        pathSelectorButton.setOnClickListener { button ->
            if (Build.VERSION.SDK_INT < 21) {
                viewModel.requestStorageFolder()
                return@setOnClickListener
            }

            pathsPopupMenu.show()
        }

        approveFolderButton.setOnClickListener {
            viewModel.path.value?.let {
                setFragmentResult(RESULT_FILE_PICKED, bundleOf(EXTRA_DOCUMENT_FILE to it.file))
                findNavController().navigateUp()
            }
        }

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.empty_files_list)
        emptyView.emptyImage.setImageResource(R.drawable.ic_insert_drive_file_white_24dp)
        emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        pathAdapter.setHasStableIds(true)
        pathRecyclerView.adapter = pathAdapter

        pathAdapter.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    pathRecyclerView.scrollToPosition(pathAdapter.itemCount - 1)
                }
            }
        )

        viewModel.files.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }

        viewModel.pathTree.observe(viewLifecycleOwner) {
            pathAdapter.submitList(it)
        }

        viewModel.path.observe(viewLifecycleOwner) {
            if (args.selectionType == SelectionType.Folder) {
                approveFolderButton.isEnabled = it.file.canWrite()
                if (it.file.canWrite()) nonWritableWarning.dismiss() else nonWritableWarning.show()
            }
        }

        viewModel.safFolders.observe(viewLifecycleOwner) {
            pathsPopupMenu.menu.clear()
            pathsPopupMenu.setOnMenuItemClickListener { menuItem ->
                if (menuItem.itemId == R.id.storage_folder) {
                    viewModel.requestStorageFolder()
                } else if (menuItem.itemId == R.id.default_storage_folder) {
                    viewModel.requestDefaultStorageFolder()
                } else if (menuItem.groupId == R.id.locations_custom) {
                    viewModel.requestPath(it[menuItem.itemId])
                } else if (menuItem.itemId == R.id.add_storage) {
                    addAccess.launch(null)
                } else if (menuItem.itemId == R.id.clear_storage_list) {
                    viewModel.clearStorageList()
                } else {
                    return@setOnMenuItemClickListener false
                }

                return@setOnMenuItemClickListener true
            }
            pathsPopupMenu.inflate(R.menu.file_browser)
            pathsPopupMenu.menu.findItem(R.id.storage_folder).isVisible = viewModel.isCustomStorageFolder
            pathsPopupMenu.menu.findItem(R.id.clear_storage_list).isVisible = it.isNotEmpty()
            it.forEachIndexed { index, safFolder ->
                pathsPopupMenu.menu.add(R.id.locations_custom, index, Menu.NONE, safFolder.name).apply {
                    setIcon(R.drawable.ic_save_white_24dp)
                }
            }
        }

        activity?.onBackPressedDispatcher?.addCallback(
            viewLifecycleOwner, object : OnBackPressedCallback(true) {
                private var afterPopup = false

                override fun handleOnBackPressed() {
                    if (viewModel.goUp()) {
                        afterPopup = false
                    } else if (afterPopup) {
                        isEnabled = false
                        activity?.onBackPressedDispatcher?.onBackPressed()
                    } else {
                        afterPopup = true
                        pathsPopupMenu.show()
                    }
                }
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        createFolderDialog.dismiss()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.file_picker, menu)

        viewModel.path.observe(viewLifecycleOwner) {
            menu.findItem(R.id.create_folder).isEnabled = it.file.canWrite()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.create_folder) {
            createFolderDialog.show()
        } else {
            return super.onOptionsItemSelected(item)
        }

        return true
    }

    enum class SelectionType {
        Folder,
        File,
    }

    companion object {
        const val RESULT_FILE_PICKED = "resultFilePicked"

        const val EXTRA_DOCUMENT_FILE = "extraDocumentFile"
    }
}

class FilePickerViewHolder(
    private val selectionType: FilePickerFragment.SelectionType,
    private val clickListener: (FileModel, FilePickerAdapter.ClickType) -> Unit,
    private val binding: ListFilePickerBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(fileModel: FileModel) {
        binding.viewModel = FileContentViewModel(fileModel)
        binding.selectionType = selectionType
        binding.selectButton.setOnClickListener {
            clickListener(fileModel, FilePickerAdapter.ClickType.Select)
        }
        if (fileModel.file.isDirectory()) {
            binding.root.setOnClickListener {
                clickListener(fileModel, FilePickerAdapter.ClickType.Open)
            }
        } else {
            binding.root.setOnClickListener(null)
        }
        binding.executePendingBindings()
    }
}

class FilePickerAdapter(
    private val selectionType: FilePickerFragment.SelectionType,
    private val clickListener: (FileModel, ClickType) -> Unit,
) : ListAdapter<ContentModel, RecyclerView.ViewHolder>(ContentModelItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder = when (viewType) {
        VIEW_TYPE_FILE -> FilePickerViewHolder(
            selectionType,
            clickListener,
            ListFilePickerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        VIEW_TYPE_SECTION -> TitleSectionViewHolder(
            ListSectionTitleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        else -> throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FileModel -> if (holder is FilePickerViewHolder) holder.bind(item)
            is TitleSectionContentModel -> if (holder is TitleSectionViewHolder) holder.bind(item)
            else -> throw IllegalStateException()
        }
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id()
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is FileModel -> VIEW_TYPE_FILE
        is TitleSectionContentModel -> VIEW_TYPE_SECTION
        else -> throw IllegalStateException()
    }

    companion object {
        const val VIEW_TYPE_SECTION = 0

        const val VIEW_TYPE_FILE = 1
    }

    enum class ClickType {
        Open,
        Select,
    }
}
