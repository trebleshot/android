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
import com.genonbeta.android.framework.util.Files
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.Video
import org.monora.uprotocol.client.android.data.MediaRepository
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListVideoBinding
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import javax.inject.Inject

@AndroidEntryPoint
class VideoBrowserFragment : Fragment(R.layout.layout_image_browser) {
    private val browserViewModel: VideoBrowserViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = VideoBrowserAdapter()
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.text_listEmptyVideo)
        emptyView.emptyImage.setImageResource(R.drawable.ic_photo_white_24dp)
        emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        browserViewModel.allVideos.observe(viewLifecycleOwner) {
            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }
    }
}

class VideoBrowserAdapter : ListAdapter<Video, VideoViewHolder>(VideoItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VideoViewHolder {
        return VideoViewHolder(ListVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VideoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class VideoItemCallback : DiffUtil.ItemCallback<Video>() {
    override fun areItemsTheSame(oldItem: Video, newItem: Video): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Video, newItem: Video): Boolean {
        return oldItem == newItem
    }
}

class VideoContentViewModel(video: Video) {
    val title = video.title

    val size = Files.formatLength(video.size, false)

    val uri = video.uri
}

class VideoViewHolder(private val binding: ListVideoBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(video: Video) {
        binding.viewModel = VideoContentViewModel(video)
        binding.executePendingBindings()
    }
}

@HiltViewModel
class VideoBrowserViewModel @Inject internal constructor(
    mediaRepository: MediaRepository,
) : ViewModel() {
    val allVideos = mediaRepository.getAllVideos()
}