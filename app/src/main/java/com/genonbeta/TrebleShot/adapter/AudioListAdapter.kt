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

import android.database.Cursor
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.collection.ArrayMap
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.android.framework.util.listing.Merger
import java.io.File

class AudioListAdapter(fragment: IEditableListFragment<AudioItemHolder?, GroupViewHolder?>?) :
    GroupEditableListAdapter<AudioItemHolder?, GroupViewHolder?>(fragment, MODE_GROUP_BY_ARTIST),
    CustomGroupLister<AudioItemHolder?> {
    private val mResolver: ContentResolver
    protected override fun onLoad(lister: GroupLister<AudioItemHolder>) {
        val albumList: MutableMap<Int, AlbumHolder> = ArrayMap()
        val songCursor: Cursor = mResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null,
            MediaStore.Audio.Media.IS_MUSIC + "=?", arrayOf(1.toString()), null
        )
        val albumCursor: Cursor = mResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, null,
            null, null, null
        )
        if (albumCursor != null) {
            if (albumCursor.moveToFirst()) {
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
            albumCursor.close()
        }
        if (songCursor != null) {
            if (songCursor.moveToFirst()) {
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
            songCursor.close()
        }
    }

    protected override fun onGenerateRepresentative(text: String, merger: Merger<AudioItemHolder>?): AudioItemHolder {
        return AudioItemHolder(text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        if (viewType == GroupEditableListAdapter.Companion.VIEW_TYPE_REPRESENTATIVE) return GroupViewHolder(
            getInflater().inflate(R.layout.layout_list_title, parent, false),
            R.id.layout_list_title_text
        )
        val holder = GroupViewHolder(
            getInflater().inflate(
                R.layout.list_music, parent,
                false
            )
        )
        getFragment().registerLayoutViewClicks(holder)
        holder.itemView.findViewById<View>(R.id.visitView)
            .setOnClickListener(View.OnClickListener { v: View? -> getFragment().performLayoutClickOpen(holder) })
        holder.itemView.findViewById<View>(R.id.selector)
            .setOnClickListener(View.OnClickListener { v: View? -> getFragment().setItemSelected(holder, true) })
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val `object`: AudioItemHolder = getItem(position)
            val parentView: View = holder.itemView
            if (!holder.tryBinding(`object`)) {
                val image = parentView.findViewById<ImageView>(R.id.image)
                val text1: TextView = parentView.findViewById<TextView>(R.id.text)
                val text2: TextView = parentView.findViewById<TextView>(R.id.text2)
                val text3: TextView = parentView.findViewById<TextView>(R.id.text3)
                val textSeparator1: TextView = parentView.findViewById<TextView>(R.id.textSeparator1)
                text1.setText(`object`.song)
                if (getGroupBy() == MODE_GROUP_BY_ALBUM || getGroupBy() == MODE_GROUP_BY_ARTIST) {
                    text2.setText(if (getGroupBy() == MODE_GROUP_BY_ALBUM) `object`.artist else `object`.albumHolder!!.title)
                    text3.setVisibility(View.GONE)
                    textSeparator1.setVisibility(View.GONE)
                } else {
                    text2.setText(`object`.artist)
                    text3.setText(`object`.albumHolder!!.title)
                    text3.setVisibility(View.VISIBLE)
                    textSeparator1.setVisibility(View.VISIBLE)
                }
                parentView.isSelected = `object`.isSelectableSelected
                GlideApp.with(getContext())
                    .load(`object`.albumHolder!!.art)
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
        `object`: AudioItemHolder
    ): Boolean {
        if (mode == MODE_GROUP_BY_ALBUM) lister.offer(
            `object`, StringMerger<AudioItemHolder>(
                `object`.albumHolder!!.title
            )
        ) else if (mode == MODE_GROUP_BY_ARTIST) lister.offer(
            `object`,
            StringMerger<AudioItemHolder>(`object`.artist)
        ) else if (mode == MODE_GROUP_BY_FOLDER) lister.offer(
            `object`,
            StringMerger<AudioItemHolder>(`object`.folder)
        ) else return false
        return true
    }

    override fun createLister(loadedList: MutableList<AudioItemHolder>, groupBy: Int): GroupLister<AudioItemHolder> {
        return super.createLister(loadedList, groupBy)
            .setCustomLister(this)
    }

    fun extractFolderName(folder: String): String {
        var folder = folder
        if (folder.contains(File.separator)) {
            val split = folder.split(File.separator.toRegex()).toTypedArray()
            if (split.size >= 2) folder = split[split.size - 2]
        }
        return folder
    }

    override fun getSectionName(position: Int, `object`: AudioItemHolder): String {
        if (!`object`.isGroupRepresentative()) {
            if (getGroupBy() == MODE_GROUP_BY_ARTIST) return `object`.artist!! else if (getGroupBy() == MODE_GROUP_BY_FOLDER) return `object`.folder!! else if (getGroupBy() == MODE_GROUP_BY_ALBUM) return `object`.albumHolder!!.title
        }
        return super.getSectionName(position, `object`)
    }

    class AudioItemHolder : GroupShareable {
        var artist: String? = null
        var song: String? = null
        var folder: String? = null
        var albumId = 0
        var albumHolder: AlbumHolder? = null

        constructor(representativeText: String?) : super(
            GroupEditableListAdapter.Companion.VIEW_TYPE_REPRESENTATIVE,
            representativeText
        ) {
        }

        constructor(
            id: Long, displayName: String?, artist: String, song: String, folder: String?, mimeType: String?,
            albumId: Int, albumHolder: AlbumHolder?, date: Long, size: Long, uri: Uri?
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
            for (keyword in filteringKeywords) if (TextUtils.searchWord(folder, keyword) || TextUtils.searchWord(
                    artist,
                    keyword
                )
                || TextUtils.searchWord(song, keyword) || TextUtils.searchWord(
                    albumHolder!!.title, keyword
                )
            ) return true
            return false
        }

        override fun getComparableName(): String? {
            return song
        }
    }

    class AlbumHolder(var id: Int, var title: String, var art: String?) {
        override fun equals(obj: Any?): Boolean {
            return if (obj is AlbumHolder) obj.id == id else super.equals(obj)
        }
    }

    companion object {
        val MODE_GROUP_BY_ALBUM: Int = GroupEditableListAdapter.Companion.MODE_GROUP_BY_NOTHING + 1
        val MODE_GROUP_BY_ARTIST = MODE_GROUP_BY_ALBUM + 1
        val MODE_GROUP_BY_FOLDER = MODE_GROUP_BY_ARTIST + 1
    }

    init {
        mResolver = getContext().getContentResolver()
    }
}