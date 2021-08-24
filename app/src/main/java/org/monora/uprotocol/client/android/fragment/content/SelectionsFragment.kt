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

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.adapter.FileAdapter
import org.monora.uprotocol.client.android.content.App
import org.monora.uprotocol.client.android.content.Image
import org.monora.uprotocol.client.android.content.Song
import org.monora.uprotocol.client.android.content.Video
import org.monora.uprotocol.client.android.databinding.ListAppBinding
import org.monora.uprotocol.client.android.databinding.ListFileNouveauBinding
import org.monora.uprotocol.client.android.databinding.ListImageBinding
import org.monora.uprotocol.client.android.databinding.ListSectionTitleBinding
import org.monora.uprotocol.client.android.databinding.ListSongBinding
import org.monora.uprotocol.client.android.databinding.ListVideoBinding
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.model.TitleSectionContentModel
import org.monora.uprotocol.client.android.viewholder.FileViewHolder
import org.monora.uprotocol.client.android.viewholder.TitleSectionViewHolder
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel

@AndroidEntryPoint
class SelectionsFragment : BottomSheetDialogFragment() {
    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_selections, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val layoutManager = recyclerView.layoutManager
        val adapter = SelectionsAdapter { any, selected ->
            selectionViewModel.setSelected(any, selected)
        }

        check(layoutManager is GridLayoutManager) {
            "Grid layout manager is needed!"
        }

        recyclerView.adapter = adapter

        selectionViewModel.getEditorList().observe(viewLifecycleOwner) {
            layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    return when (it[position]) {
                        is Image, is Video, is App -> 1
                        else -> layoutManager.spanCount
                    }
                }
            }

            adapter.submitList(it)
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        selectionViewModel.externalState.value = Unit
    }
}

class SelectionsItemCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        // We don't plan on updating the list!
        return true
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        // We don't plan on updating the list!
        return true
    }
}

class SelectionsAdapter(
    private val selectionClickListener: (Any, Boolean) -> Unit
) : ListAdapter<Any, RecyclerView.ViewHolder>(SelectionsItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_SECTION_TITLE -> TitleSectionViewHolder(
                ListSectionTitleBinding.inflate(inflater, parent, false)
            )
            VIEW_TYPE_APP -> AppViewHolder(
                ListAppBinding.inflate(inflater, parent, false)
            ) { app, clickType ->
                if (clickType == AppBrowserAdapter.ClickType.ToggleSelect) {
                    selectionClickListener(app, app.isSelected)
                }
            }
            VIEW_TYPE_FILE -> FileViewHolder(
                ListFileNouveauBinding.inflate(inflater, parent, false)
            ) { fileModel, clickType ->
                if (clickType == FileAdapter.ClickType.ToggleSelect) {
                    selectionClickListener(fileModel, fileModel.isSelected)
                }
            }
            VIEW_TYPE_SONG -> SongViewHolder(
                ListSongBinding.inflate(inflater, parent, false)
            ) { song, clickType ->
                if (clickType == SongBrowserAdapter.ClickType.ToggleSelect) {
                    selectionClickListener(song, song.isSelected)
                }
            }
            VIEW_TYPE_IMAGE -> ImageViewHolder(
                ListImageBinding.inflate(inflater, parent, false)
            ) { image, clickType ->
                if (clickType == ImageBrowserAdapter.ClickType.ToggleSelect) {
                    selectionClickListener(image, image.isSelected)
                }
            }
            VIEW_TYPE_VIDEO -> VideoViewHolder(
                ListVideoBinding.inflate(inflater, parent, false)
            ) { video, clickType ->
                if (clickType == VideoBrowserAdapter.ClickType.ToggleSelect) {
                    selectionClickListener(video, video.isSelected)
                }
            }
            else -> throw IllegalStateException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is TitleSectionViewHolder -> if (item is TitleSectionContentModel) holder.bind(item)
            is AppViewHolder -> if (item is App) holder.bind(item)
            is FileViewHolder -> if (item is FileModel) holder.bind(item)
            is SongViewHolder -> if (item is Song) holder.bind(item)
            is ImageViewHolder -> if (item is Image) holder.bind(item)
            is VideoViewHolder -> if (item is Video) holder.bind(item)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is TitleSectionContentModel -> VIEW_TYPE_SECTION_TITLE
            is App -> VIEW_TYPE_APP
            is FileModel -> VIEW_TYPE_FILE
            is Song -> VIEW_TYPE_SONG
            is Image -> VIEW_TYPE_IMAGE
            is Video -> VIEW_TYPE_VIDEO
            else -> throw IllegalStateException()
        }
    }

    companion object {
        const val VIEW_TYPE_SECTION_TITLE = 1

        const val VIEW_TYPE_APP = 2

        const val VIEW_TYPE_FILE = 3

        const val VIEW_TYPE_SONG = 4

        const val VIEW_TYPE_IMAGE = 5

        const val VIEW_TYPE_VIDEO = 6
    }
}
