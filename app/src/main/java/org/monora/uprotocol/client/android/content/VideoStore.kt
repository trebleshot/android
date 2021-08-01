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
import android.provider.MediaStore.Video.Media
import androidx.lifecycle.liveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class VideoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getAll() = liveData<List<Video>>(Dispatchers.IO) {
        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                Media._ID,
                Media.TITLE,
                Media.SIZE,
                Media.DURATION,
                Media.MIME_TYPE,
                Media.DATE_MODIFIED,
            ),
            null,
            null,
            Media.DATE_MODIFIED
        )?.use {
            if (it.moveToFirst()) {
                val idIndex = it.getColumnIndex(Media._ID)
                val titleIndex = it.getColumnIndex(Media.TITLE)
                val sizeIndex = it.getColumnIndex(Media.SIZE)
                val durationIndex = it.getColumnIndex(Media.DURATION)
                val mimeTypeIndex = it.getColumnIndex(Media.MIME_TYPE)
                val dateModifiedIndex = it.getColumnIndex(Media.DATE_MODIFIED)

                val list = ArrayList<Video>(it.count)

                do {
                    val id = it.getLong(idIndex)

                    list.add(
                        Video(
                            id,
                            it.getString(titleIndex),
                            it.getLong(sizeIndex),
                            it.getInt(durationIndex),
                            it.getString(mimeTypeIndex),
                            it.getLong(dateModifiedIndex),
                            ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id)
                        )
                    )
                } while (it.moveToNext())

                emit(list)
            }
        }
    }
}

@Parcelize
data class Video(
    val id: Long,
    val title: String,
    val size: Long,
    val duration: Int,
    val mimeType: String,
    val dateModified: Long,
    val uri: Uri,
) : Parcelable {
    @IgnoredOnParcel
    var isSelected = false

    override fun equals(other: Any?): Boolean {
        return other is Video && uri == other.uri
    }
}