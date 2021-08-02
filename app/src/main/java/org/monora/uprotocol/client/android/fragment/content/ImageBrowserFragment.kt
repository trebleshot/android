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
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.util.Files
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.Image
import org.monora.uprotocol.client.android.data.MediaRepository
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListImageBinding
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ImageBrowserFragment : Fragment(R.layout.layout_image_browser) {
    private val browserViewModel: ImageBrowserViewModel by viewModels()

    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val adapter = ImageBrowserAdapter { image, clickType ->
            when (clickType) {
                ImageBrowserAdapter.ClickType.Default -> {
                }
                ImageBrowserAdapter.ClickType.ToggleSelect -> {
                    selectionViewModel.setSelected(image, image.isSelected)
                }
            }
        }
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.text_listEmptyImage)
        emptyView.emptyImage.setImageResource(R.drawable.ic_photo_white_24dp)
        emptyView.executePendingBindings()
        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter

        browserViewModel.allImages.observe(viewLifecycleOwner) {
            it.forEach { image ->
                if (selectionViewModel.contains(image)) image.isSelected = true
            }

            adapter.submitList(it)
            emptyContentViewModel.with(recyclerView, it.isNotEmpty())
        }
    }
}

class ImageBrowserAdapter(
    private val clickListener: (Image, ClickType) -> Unit,
) : ListAdapter<Image, ImageViewHolder>(ImageItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            clickListener, ListImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_IMAGE
    }

    enum class ClickType {
        Default,
        ToggleSelect,
    }

    companion object {
        const val VIEW_TYPE_IMAGE = 0
    }
}

class ImageItemCallback : DiffUtil.ItemCallback<Image>() {
    override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
        return oldItem == newItem
    }
}

class ImageContentViewModel(image: Image) {
    val title = image.title

    val size = Files.formatLength(image.size, false)

    val uri = image.uri
}

class ImageViewHolder(
    private val clickListener: (Image, ImageBrowserAdapter.ClickType) -> Unit,
    private val binding: ListImageBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(image: Image) {
        binding.viewModel = ImageContentViewModel(image)
        binding.root.setOnClickListener {
            clickListener(image, ImageBrowserAdapter.ClickType.Default)
        }
        binding.selection.setOnClickListener {
            image.isSelected = !image.isSelected
            it.isSelected = image.isSelected
            clickListener(image, ImageBrowserAdapter.ClickType.ToggleSelect)
        }
        binding.selection.isSelected = image.isSelected
        binding.executePendingBindings()
    }
}

@HiltViewModel
class ImageBrowserViewModel @Inject internal constructor(
    mediaRepository: MediaRepository,
) : ViewModel() {
    val allImages = mediaRepository.getAllImages()
}