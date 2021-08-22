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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.Song
import org.monora.uprotocol.client.android.data.MediaRepository
import org.monora.uprotocol.client.android.data.SelectionRepository
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListSongBinding
import org.monora.uprotocol.client.android.util.Activities
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class AudioBrowserFragment : Fragment(R.layout.layout_audio_browser) {
    private val browserViewModel: AudioBrowserViewModel by viewModels()

    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = AudioBrowserAdapter { song, clickType ->
            when (clickType) {
                AudioBrowserAdapter.ClickType.Default -> Activities.view(view.context, song.uri, song.mimeType)
                AudioBrowserAdapter.ClickType.ToggleSelect -> {
                    selectionViewModel.setSelected(song, song.isSelected)
                }
            }
        }
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.text_listEmptyMusic)
        emptyView.emptyImage.setImageResource(R.drawable.ic_music_note_white_24dp)
        emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        browserViewModel.allSongs.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }

        selectionViewModel.externalState.observe(viewLifecycleOwner) {
            adapter.notifyDataSetChanged()
        }
    }
}

class AudioBrowserAdapter(
    val clickListener: (Song, ClickType) -> Unit
) : ListAdapter<Song, SongViewHolder>(SongItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(
            ListSongBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            clickListener,
        )
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_SONG
    }

    enum class ClickType {
        Default,
        ToggleSelect,
    }

    companion object {
        const val VIEW_TYPE_SONG = 0
    }
}

class SongItemCallback : DiffUtil.ItemCallback<Song>() {
    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean {
        return oldItem == newItem
    }
}

class SongContentViewModel(song: Song) {
    val artist = song.artist

    val title = song.title

    val mimeType = song.mimeType

    val uri = song.uri
}

class SongViewHolder(
    private val binding: ListSongBinding,
    private val clickListener: (Song, AudioBrowserAdapter.ClickType) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(song: Song) {
        binding.viewModel = SongContentViewModel(song)
        binding.root.setOnClickListener {
            clickListener(song, AudioBrowserAdapter.ClickType.Default)
        }
        binding.selection.setOnClickListener {
            song.isSelected = !song.isSelected
            it.isSelected = song.isSelected
            clickListener(song, AudioBrowserAdapter.ClickType.ToggleSelect)
        }
        binding.selection.isSelected = song.isSelected
        binding.executePendingBindings()
    }
}

@HiltViewModel
class AudioBrowserViewModel @Inject internal constructor(
    mediaRepository: MediaRepository,
    private val selectionRepository: SelectionRepository,
) : ViewModel() {
    val allSongs = Transformations.map(mediaRepository.getAllSongs()) {
        selectionRepository.whenContains(it) { item, selected -> item.isSelected = selected }
        it
    }
}
