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
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.util.Files
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.Image
import org.monora.uprotocol.client.android.content.ImageBucket
import org.monora.uprotocol.client.android.data.MediaRepository
import org.monora.uprotocol.client.android.data.SelectionRepository
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListImageBinding
import org.monora.uprotocol.client.android.databinding.ListImageBucketBinding
import org.monora.uprotocol.client.android.util.Activities
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class ImageBrowserFragment : Fragment(R.layout.layout_image_browser) {
    private val browserViewModel: ImageBrowserViewModel by viewModels()

    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            browserViewModel.showBuckets()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val titleView = view.findViewById<TextView>(R.id.titleText)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val imageAdapter = ImageBrowserAdapter { image, clickType ->
            when (clickType) {
                ImageBrowserAdapter.ClickType.Default -> Activities.view(view.context, image.uri, image.mimeType)
                ImageBrowserAdapter.ClickType.ToggleSelect -> {
                    selectionViewModel.setSelected(image, image.isSelected)
                }
            }
        }
        val bucketAdapter = ImageBucketBrowserAdapter {
            browserViewModel.showImages(it)
        }
        val emptyContentViewModel = EmptyContentViewModel()

        emptyView.viewModel = emptyContentViewModel
        emptyView.emptyText.setText(R.string.empty_photos_list)
        emptyView.emptyImage.setImageResource(R.drawable.ic_photo_white_24dp)
        emptyView.executePendingBindings()
        imageAdapter.setHasStableIds(true)
        recyclerView.adapter = imageAdapter

        browserViewModel.showingContent.observe(viewLifecycleOwner) {
            when (it) {
                is ImageBrowserViewModel.Content.Buckets -> {
                    backPressedCallback.isEnabled = false
                    titleView.text = getString(R.string.folders)
                    recyclerView.adapter = bucketAdapter
                    bucketAdapter.submitList(it.list)
                    emptyContentViewModel.with(recyclerView, it.list.isNotEmpty())
                }
                is ImageBrowserViewModel.Content.Images -> {
                    backPressedCallback.isEnabled = true
                    titleView.text = it.imageBucket.name
                    recyclerView.adapter = imageAdapter
                    imageAdapter.submitList(it.list)
                    emptyContentViewModel.with(recyclerView, it.list.isNotEmpty())
                }
            }
        }

        selectionViewModel.externalState.observe(viewLifecycleOwner) {
            imageAdapter.notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, backPressedCallback)
    }

    override fun onPause() {
        super.onPause()
        backPressedCallback.remove()
    }
}

class ImageBrowserAdapter(
    private val clickListener: (Image, ClickType) -> Unit,
) : ListAdapter<Image, ImageViewHolder>(ImageItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            ListImageBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            clickListener,
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
    private val binding: ListImageBinding,
    private val clickListener: (Image, ImageBrowserAdapter.ClickType) -> Unit,
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

class ImageBucketItemCallback : DiffUtil.ItemCallback<ImageBucket>() {
    override fun areItemsTheSame(oldItem: ImageBucket, newItem: ImageBucket): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: ImageBucket, newItem: ImageBucket): Boolean {
        return oldItem == newItem
    }
}

class ImageBucketContentViewModel(imageBucket: ImageBucket) {
    val name = imageBucket.name

    val thumbnailUri = imageBucket.thumbnailUri
}

class ImageBucketViewHolder(
    private val binding: ListImageBucketBinding,
    private val clickListener: (ImageBucket) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(imageBucket: ImageBucket) {
        R.layout.list_image_bucket
        binding.viewModel = ImageBucketContentViewModel(imageBucket)
        binding.root.setOnClickListener {
            clickListener(imageBucket)
        }
        binding.executePendingBindings()
    }
}

class ImageBucketBrowserAdapter(
    private val clickListener: (ImageBucket) -> Unit,
) : ListAdapter<ImageBucket, ImageBucketViewHolder>(ImageBucketItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageBucketViewHolder {
        return ImageBucketViewHolder(
            ListImageBucketBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            clickListener,
        )
    }

    override fun onBindViewHolder(holder: ImageBucketViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_BUCKET
    }

    companion object {
        const val VIEW_TYPE_BUCKET = 0
    }
}

@HiltViewModel
class ImageBrowserViewModel @Inject internal constructor(
    private val mediaRepository: MediaRepository,
    private val selectionRepository: SelectionRepository,
) : ViewModel() {
    private val _showingContent = MutableLiveData<Content>()

    val showingContent = liveData {
        emitSource(_showingContent)
    }

    fun showBuckets() {
        viewModelScope.launch(Dispatchers.IO) {
            _showingContent.postValue(Content.Buckets(mediaRepository.getImageBuckets()))
        }
    }

    fun showImages(bucket: ImageBucket) {
        viewModelScope.launch(Dispatchers.IO) {
            val images = mediaRepository.getImages(bucket)
            selectionRepository.whenContains(images) { item, selected -> item.isSelected = selected }
            _showingContent.postValue(Content.Images(bucket, images))
        }
    }

    init {
        showBuckets()
    }

    sealed class Content {
        class Buckets(val list: List<ImageBucket>) : Content()

        class Images(val imageBucket: ImageBucket, val list: List<Image>) : Content()
    }
}
