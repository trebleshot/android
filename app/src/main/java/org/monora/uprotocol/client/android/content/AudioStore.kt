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

package org.monora.uprotocol.client.android.content

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore.Audio.Media
import androidx.lifecycle.liveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class AudioStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getAll() = liveData<List<Song>>(Dispatchers.IO) {
        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            null,
            "${Media.IS_MUSIC} = ?", arrayOf("1"),
            "${Media.TITLE} ASC"
        )?.use {
            if (it.moveToFirst()) {
                val idIndex: Int = it.getColumnIndex(Media._ID)
                val artistIndex: Int = it.getColumnIndex(Media.ARTIST)
                val albumIndex: Int = it.getColumnIndex(Media.ALBUM)
                val titleIndex: Int = it.getColumnIndex(Media.TITLE)
                val displayNameIndex: Int = it.getColumnIndex(Media.DISPLAY_NAME)
                val mimeTypeIndex: Int = it.getColumnIndex(Media.MIME_TYPE)
                val sizeIndex: Int = it.getColumnIndex(Media.SIZE)
                val dateModifiedIndex: Int = it.getColumnIndex(Media.DATE_MODIFIED)

                val result = ArrayList<Song>(it.count)

                do {
                    val id = it.getLong(idIndex)

                    result.add(
                        Song(
                            id,
                            it.getString(artistIndex),
                            it.getString(albumIndex),
                            it.getString(titleIndex),
                            it.getString(displayNameIndex),
                            it.getString(mimeTypeIndex),
                            it.getLong(sizeIndex),
                            it.getLong(dateModifiedIndex),
                            ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id)
                        )
                    )
                } while (it.moveToNext());

                emit(result)
            }
        }
    }
}

@Parcelize
data class Song(
    val id: Long,
    val artist: String,
    val album: String,
    val title: String,
    val displayName: String,
    val mimeType: String,
    val size: Long,
    val dateModified: Long,
    val uri: Uri,
) : Parcelable