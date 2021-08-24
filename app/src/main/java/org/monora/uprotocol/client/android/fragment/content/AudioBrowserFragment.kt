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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.setMargins
import androidx.core.view.setPadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.content.Album
import org.monora.uprotocol.client.android.content.Artist
import org.monora.uprotocol.client.android.content.Song
import org.monora.uprotocol.client.android.data.MediaRepository
import org.monora.uprotocol.client.android.data.SelectionRepository
import org.monora.uprotocol.client.android.databinding.LayoutEmptyContentBinding
import org.monora.uprotocol.client.android.databinding.ListAlbumBinding
import org.monora.uprotocol.client.android.databinding.ListArtistBinding
import org.monora.uprotocol.client.android.databinding.ListSongBinding
import org.monora.uprotocol.client.android.util.Activities
import org.monora.uprotocol.client.android.viewmodel.EmptyContentViewModel
import org.monora.uprotocol.client.android.viewmodel.SharingSelectionViewModel
import javax.inject.Inject

@AndroidEntryPoint
class AudioBrowserFragment : Fragment(R.layout.layout_audio_browser) {
    private val browserViewModel: AudioBrowserViewModel by viewModels()

    private val selectionViewModel: SharingSelectionViewModel by activityViewModels()

    private val backPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            when (browserViewModel.showingContent.value) {
                is AudioBrowserViewModel.Content.AllAlbums, is AudioBrowserViewModel.Content.AllArtists -> {
                    browserViewModel.showAllSongs()
                }
                is AudioBrowserViewModel.Content.ArtistAlbums -> browserViewModel.showArtists()
                is AudioBrowserViewModel.Content.AlbumSongs -> browserViewModel.showAlbums()
                else -> isEnabled = false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group)
        val filterChip = view.findViewById<Chip>(R.id.filter_chip)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val emptyView = LayoutEmptyContentBinding.bind(view.findViewById(R.id.emptyView))
        val songsAdapter = SongBrowserAdapter { song, clickType ->
            when (clickType) {
                SongBrowserAdapter.ClickType.Default -> Activities.view(view.context, song.uri, song.mimeType)
                SongBrowserAdapter.ClickType.ToggleSelect -> {
                    selectionViewModel.setSelected(song, song.isSelected)
                }
            }
        }
        val albumsAdapter = AlbumBrowserAdapter {
            browserViewModel.showAlbumSongs(it)
        }
        val artistsAdapter = ArtistBrowserAdapter {
            browserViewModel.showArtistAlbums(it)
        }
        val emptyContentViewModel = EmptyContentViewModel()
        val layoutManager = recyclerView.layoutManager

        check(layoutManager is GridLayoutManager) {
            "Expected a grid layout manager"
        }

        val recylerViewLayoutParams = recyclerView.layoutParams
        val defaultSpanCount = layoutManager.spanCount
        val listMargin = resources.getDimension(R.dimen.short_content_width_padding_between).toInt()
        val listPadding = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics
        ).toInt()

        emptyView.viewModel = emptyContentViewModel
        emptyView.executePendingBindings()
        songsAdapter.setHasStableIds(true)
        albumsAdapter.setHasStableIds(true)

        filterChip.setOnCloseIconClickListener {
            browserViewModel.showAllSongs()
        }

        browserViewModel.showingContent.observe(viewLifecycleOwner) {
            layoutManager.spanCount = if (it.isList) 1 else defaultSpanCount

            if (it.isList) {
                recyclerView.setPadding(0, listPadding, 0, listPadding)
            } else {
                recyclerView.setPadding(listPadding)
            }

            if (recylerViewLayoutParams is ViewGroup.MarginLayoutParams) {
                if (it.isList) {
                    recylerViewLayoutParams.setMargins(listMargin, 0, listMargin, 0)
                } else {
                    recylerViewLayoutParams.setMargins(0)
                }
            }

            filterChip.visibility = if (it.isFiltered) View.VISIBLE else View.GONE

            if (it.isFiltered) {
                chipGroup.check(R.id.filter_chip)
            }

            when (it) {
                is AudioBrowserViewModel.Content.AllAlbums -> {
                    recyclerView.adapter = albumsAdapter
                    albumsAdapter.submitList(it.list)
                    chipGroup.check(R.id.show_albums)
                }
                is AudioBrowserViewModel.Content.AllArtists -> {
                    recyclerView.adapter = artistsAdapter
                    artistsAdapter.submitList(it.list)
                    chipGroup.check(R.id.show_artists)
                }
                is AudioBrowserViewModel.Content.AllSongs -> {
                    recyclerView.adapter = songsAdapter
                    songsAdapter.submitList(it.list)
                    chipGroup.check(R.id.show_all_songs)
                }
                is AudioBrowserViewModel.Content.AlbumSongs -> {
                    filterChip.setChipIconResource(R.drawable.baseline_album_24)
                    filterChip.text = it.album.title
                    recyclerView.adapter = songsAdapter
                    songsAdapter.submitList(it.list)
                }
                is AudioBrowserViewModel.Content.ArtistAlbums -> {
                    filterChip.setChipIconResource(R.drawable.round_person_24)
                    filterChip.text = it.artist.name
                    recyclerView.adapter = albumsAdapter
                    albumsAdapter.submitList(it.list)
                }
            }

            when (it.type) {
                AudioBrowserViewModel.Content.Type.Songs -> {
                    emptyView.emptyText.setText(R.string.text_listEmptyMusic)
                    emptyView.emptyImage.setImageResource(R.drawable.ic_music_note_white_24dp)
                    emptyContentViewModel.with(recyclerView, songsAdapter.currentList.isNotEmpty())
                }
                AudioBrowserViewModel.Content.Type.Albums -> {
                    emptyView.emptyText.setText(R.string.no_albums)
                    emptyView.emptyImage.setImageResource(R.drawable.baseline_album_24)
                    emptyContentViewModel.with(recyclerView, albumsAdapter.currentList.isNotEmpty())
                }
                AudioBrowserViewModel.Content.Type.Artists -> {
                    emptyView.emptyText.setText(R.string.no_artists)
                    emptyView.emptyImage.setImageResource(R.drawable.round_person_24)
                    emptyContentViewModel.with(recyclerView, artistsAdapter.currentList.isNotEmpty())
                }
            }

            backPressedCallback.isEnabled = it !is AudioBrowserViewModel.Content.AllSongs
        }

        selectionViewModel.externalState.observe(viewLifecycleOwner) {
            songsAdapter.notifyDataSetChanged()
        }

        chipGroup.setOnCheckedChangeListener { group, checkedId ->
            when (checkedId) {
                R.id.show_all_songs -> browserViewModel.showAllSongs()
                R.id.show_albums -> browserViewModel.showAlbums()
                R.id.show_artists -> browserViewModel.showArtists()
            }
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

class AlbumBrowserAdapter(
    val clickListener: (Album) -> Unit
) : ListAdapter<Album, AlbumViewHolder>(AlbumItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlbumViewHolder {
        return AlbumViewHolder(
            ListAlbumBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            clickListener,
        )
    }

    override fun onBindViewHolder(holder: AlbumViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_ALBUM
    }

    companion object {
        const val VIEW_TYPE_ALBUM = 0
    }
}

class AlbumContentViewModel(album: Album) {
    val artist = album.artist

    val title = album.title

    val uri = album.uri
}

class AlbumItemCallback : DiffUtil.ItemCallback<Album>() {
    override fun areItemsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Album, newItem: Album): Boolean {
        return oldItem == newItem
    }
}

class AlbumViewHolder(
    private val binding: ListAlbumBinding,
    private val clickListener: (Album) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(album: Album) {
        binding.viewModel = AlbumContentViewModel(album)
        binding.root.setOnClickListener {
            clickListener(album)
        }
    }
}

class ArtistBrowserAdapter(
    val clickListener: (Artist) -> Unit
) : ListAdapter<Artist, ArtistViewHolder>(ArtistItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArtistViewHolder {
        return ArtistViewHolder(
            ListArtistBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            clickListener,
        )
    }

    override fun onBindViewHolder(holder: ArtistViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_ARTIST
    }

    companion object {
        const val VIEW_TYPE_ARTIST = 0
    }
}

class ArtistViewHolder(
    private val binding: ListArtistBinding,
    private val clickListener: (Artist) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(artist: Artist) {
        binding.viewModel = ArtistContentViewModel(artist)
        binding.root.setOnClickListener {
            clickListener(artist)
        }
    }
}

class ArtistContentViewModel(artist: Artist) {
    val name = artist.name

    val numberOfAlbums = artist.numberOfAlbums
}

class ArtistItemCallback : DiffUtil.ItemCallback<Artist>() {
    override fun areItemsTheSame(oldItem: Artist, newItem: Artist): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Artist, newItem: Artist): Boolean {
        return oldItem == newItem
    }
}

class SongBrowserAdapter(
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

    val albumUri = song.albumUri
}

class SongViewHolder(
    private val binding: ListSongBinding,
    private val clickListener: (Song, SongBrowserAdapter.ClickType) -> Unit
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(song: Song) {
        binding.viewModel = SongContentViewModel(song)
        binding.root.setOnClickListener {
            clickListener(song, SongBrowserAdapter.ClickType.Default)
        }
        binding.selection.setOnClickListener {
            song.isSelected = !song.isSelected
            it.isSelected = song.isSelected
            clickListener(song, SongBrowserAdapter.ClickType.ToggleSelect)
        }
        binding.selection.isSelected = song.isSelected
        binding.executePendingBindings()
    }
}

@HiltViewModel
class AudioBrowserViewModel @Inject internal constructor(
    private val mediaRepository: MediaRepository,
    private val selectionRepository: SelectionRepository,
) : ViewModel() {
    private val _showingContent = MutableLiveData<Content>()

    val showingContent = liveData<Content> {
        emitSource(_showingContent)
    }

    private fun filterSongs(list: List<Song>): List<Song> {
        selectionRepository.whenContains(list) { item, selected -> item.isSelected = selected }
        return list
    }

    fun showAllSongs() {
        viewModelScope.launch(Dispatchers.IO) {
            _showingContent.postValue(Content.AllSongs(filterSongs(mediaRepository.getAllSongs())))
        }
    }

    fun showAlbums() {
        viewModelScope.launch(Dispatchers.IO) {
            _showingContent.postValue(Content.AllAlbums(mediaRepository.getAllAlbums()))
        }
    }

    fun showArtists() {
        viewModelScope.launch(Dispatchers.IO) {
            _showingContent.postValue(Content.AllArtists(mediaRepository.getAllArtists()))
        }
    }

    fun showAlbumSongs(album: Album) {
        viewModelScope.launch(Dispatchers.IO) {
            _showingContent.postValue(Content.AlbumSongs(album, filterSongs(mediaRepository.getAlbumSongs(album))))
        }
    }

    fun showArtistAlbums(artist: Artist) {
        viewModelScope.launch(Dispatchers.IO) {
            _showingContent.postValue(Content.ArtistAlbums(artist, mediaRepository.getArtistAlbums(artist)))
        }
    }

    init {
        showAllSongs()
    }

    sealed class Content(val type: Type, val isList: Boolean = false, val isFiltered: Boolean = false) {
        class AllSongs(val list: List<Song>) : Content(Type.Songs, isList = true)

        class AllAlbums(val list: List<Album>) : Content(Type.Albums)

        class AllArtists(val list: List<Artist>) : Content(Type.Artists)

        class AlbumSongs(val album: Album, val list: List<Song>) : Content(Type.Songs, isList = true, isFiltered = true)

        class ArtistAlbums(val artist: Artist, val list: List<Album>) : Content(Type.Albums, isFiltered = true)

        enum class Type {
            Songs,
            Albums,
            Artists,
        }
    }
}
