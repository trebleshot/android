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
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.Song
import org.monora.uprotocol.client.android.data.MediaRepository
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListSongBinding
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import javax.inject.Inject

@AndroidEntryPoint
class AudioBrowserFragment : Fragment(R.layout.layout_audio_browser) {
    private val browserViewModel: AudioBrowserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = AudioBrowserAdapter()
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
    }
}

class AudioBrowserAdapter : ListAdapter<Song, SongViewHolder>(SongItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        return SongViewHolder(ListSongBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        holder.bind(getItem(position))
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
}

class SongViewHolder(private val binding: ListSongBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(song: Song) {
        binding.viewModel = SongContentViewModel(song)
        binding.executePendingBindings()
    }
}

@HiltViewModel
class AudioBrowserViewModel @Inject internal constructor(
    mediaRepository: MediaRepository,
) : ViewModel() {
    val allSongs = mediaRepository.getAllSongs()
}