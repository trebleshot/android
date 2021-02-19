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
package com.genonbeta.TrebleShot.adapter

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.AudioListAdapter.AudioItemHolder
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupLister.CustomGroupLister
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.android.framework.util.listing.Merger
import com.genonbeta.android.framework.util.listing.merger.StringMerger
import java.io.File

class AudioListAdapter(fragment: IEditableListFragment<AudioItemHolder, GroupViewHolder>) :
    GroupEditableListAdapter<AudioItemHolder, GroupViewHolder>(fragment, MODE_GROUP_BY_ARTIST),
    CustomGroupLister<AudioItemHolder> {
    private val resolver: ContentResolver = context.contentResolver

    override fun onLoad(lister: GroupLister<AudioItemHolder>) {
        val albumList: MutableMap<Int, AlbumHolder> = ArrayMap()

        resolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null,
            null, null, null
        )?.use { albumCursor: Cursor ->
            if (!albumCursor.moveToFirst())
                return

            val idIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums._ID)
            val artIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM_ART)
            val titleIndex = albumCursor.getColumnIndex(MediaStore.Audio.Albums.ALBUM)
            do {
                albumList[albumCursor.getInt(idIndex)] = AlbumHolder(
                    albumCursor.getInt(idIndex),
                    albumCursor.getString(titleIndex), albumCursor.getString(artIndex)
                )
            } while (albumCursor.moveToNext())
        }

        resolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
            MediaStore.Audio.Media.IS_MUSIC + "=?", arrayOf(1.toString()), null
        )?.use { songCursor: Cursor ->
            if (!songCursor.moveToFirst())
                return

            val idIndex = songCursor.getColumnIndex(MediaStore.Audio.Media._ID)
            val artistIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val songIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val folderIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DATA)
            val albumIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val nameIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
            val dateIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.DATE_MODIFIED)
            val sizeIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.SIZE)
            val typeIndex = songCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            do {
                lister.offerObliged(
                    this, AudioItemHolder(
                        songCursor.getLong(idIndex),
                        songCursor.getString(nameIndex), songCursor.getString(artistIndex),
                        songCursor.getString(songIndex), extractFolderName(songCursor.getString(folderIndex)),
                        songCursor.getString(typeIndex), songCursor.getInt(albumIndex),
                        albumList[songCursor.getInt(albumIndex)], songCursor.getLong(dateIndex) * 1000,
                        songCursor.getLong(sizeIndex), Uri.parse(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.toString() + "/"
                                    + songCursor.getInt(idIndex)
                        )
                    )
                )
            } while (songCursor.moveToNext())
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        if (viewType == VIEW_TYPE_REPRESENTATIVE) return GroupViewHolder(
            layoutInflater.inflate(R.layout.layout_list_title, parent, false), R.id.layout_list_title_text
        )
        val holder = GroupViewHolder(layoutInflater.inflate(R.layout.list_music, parent, false))

        fragment.registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.visitView).setOnClickListener {
            fragment.performLayoutClickOpen(holder)
        }
        holder.itemView.findViewById<View>(R.id.selector).setOnClickListener {
            fragment.setItemSelected(holder, true)
        }
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val itemHolder: AudioItemHolder = getItem(position)
            val parentView: View = holder.itemView
            if (!holder.tryBinding(itemHolder)) {
                val image = parentView.findViewById<ImageView>(R.id.image)
                val text1: TextView = parentView.findViewById(R.id.text)
                val text2: TextView = parentView.findViewById(R.id.text2)
                val text3: TextView = parentView.findViewById(R.id.text3)
                val textSeparator1: TextView = parentView.findViewById(R.id.textSeparator1)
                text1.text = itemHolder.song
                if (groupBy == MODE_GROUP_BY_ALBUM || groupBy == MODE_GROUP_BY_ARTIST) {
                    text2.text = if (groupBy == MODE_GROUP_BY_ALBUM) itemHolder.artist else itemHolder.albumHolder.title
                    text3.visibility = View.GONE
                    textSeparator1.setVisibility(View.GONE)
                } else {
                    text2.text = itemHolder.artist
                    text3.text = itemHolder.albumHolder.title
                    text3.visibility = View.VISIBLE
                    textSeparator1.visibility = View.VISIBLE
                }
                parentView.isSelected = itemHolder.isSelectableSelected()
                GlideApp.with(context)
                    .load(itemHolder.albumHolder.art)
                    .placeholder(R.drawable.ic_music_note_white_24dp)
                    .override(160)
                    .centerCrop()
                    .into(image)
            }
        } catch (ignored: Exception) {
        }
    }

    override fun onCustomGroupListing(
        lister: GroupLister<AudioItemHolder>,
        mode: Int,
        holder: AudioItemHolder,
    ): Boolean {
        when (mode) {
            MODE_GROUP_BY_ALBUM -> lister.offer(holder, StringMerger(holder.albumHolder.title))
            MODE_GROUP_BY_ARTIST -> lister.offer(holder, StringMerger(holder.artist))
            MODE_GROUP_BY_FOLDER -> lister.offer(holder, StringMerger(holder.folder))
            else -> return false
        }
        return true
    }

    override fun createLister(loadedList: MutableList<AudioItemHolder>, groupBy: Int): GroupLister<AudioItemHolder> {
        return super.createLister(loadedList, groupBy).also { it.customLister = this }
    }

    fun extractFolderName(folder: String): String {
        if (folder.contains(File.separator)) {
            val split = folder.split(File.separator.toRegex()).toTypedArray()
            if (split.size >= 2) return split[split.size - 2]
        }
        return folder
    }

    override fun getSectionName(position: Int, item: AudioItemHolder): String {
        if (!item.isGroupRepresentative()) {
            return when {
                groupBy == MODE_GROUP_BY_ARTIST -> item.artist
                groupBy == MODE_GROUP_BY_FOLDER -> item.folder
                groupBy == MODE_GROUP_BY_ALBUM -> item.albumHolder.title
                else -> item.friendlyName
            }
        }
        return super.getSectionName(position, item)
    }

    class AudioItemHolder : GroupShareable {
        lateinit var artist: String

        lateinit var song: String

        lateinit var folder: String

        lateinit var albumHolder: AlbumHolder

        var albumId = 0

        constructor(representativeText: String) : super(
            VIEW_TYPE_REPRESENTATIVE,
            representativeText
        )

        constructor(
            id: Long, displayName: String, artist: String, song: String, folder: String, mimeType: String,
            albumId: Int, albumHolder: AlbumHolder?, date: Long, size: Long, uri: Uri,
        ) {
            initialize(id, "$song - $artist", displayName, mimeType, date, size, uri)
            this.artist = artist
            this.song = song
            this.folder = folder
            this.albumId = albumId
            this.albumHolder = albumHolder ?: AlbumHolder(albumId, "-", null)
        }

        override fun applyFilter(filteringKeywords: Array<String>): Boolean {
            if (super.applyFilter(filteringKeywords)) return true
            for (keyword in filteringKeywords) if (TextUtils.searchWord(folder, keyword)
                || TextUtils.searchWord(artist, keyword) || TextUtils.searchWord(song, keyword)
                || TextUtils.searchWord(albumHolder.title, keyword)
            ) return true
            return false
        }

        override fun getComparableName(): String {
            return song
        }
    }

    class AlbumHolder(var id: Int, var title: String, var art: String?) {
        override fun equals(other: Any?): Boolean {
            return if (other is AlbumHolder) other.id == id else super.equals(other)
        }
    }

    companion object {
        val MODE_GROUP_BY_ALBUM: Int = MODE_GROUP_BY_NOTHING + 1
        val MODE_GROUP_BY_ARTIST = MODE_GROUP_BY_ALBUM + 1
        val MODE_GROUP_BY_FOLDER = MODE_GROUP_BY_ARTIST + 1
    }
}