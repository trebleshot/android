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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class VideoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getBuckets(): List<VideoBucket> {
        try {
            context.contentResolver.query(
                Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    Media.BUCKET_ID,
                    Media.BUCKET_DISPLAY_NAME,
                    Media._ID,
                    Media.DATE_MODIFIED,
                ),
                null,
                null,
                "${Media.DATE_MODIFIED} DESC"
            )?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(Media._ID)
                    val bucketIdIndex = it.getColumnIndex(Media.BUCKET_ID)
                    val bucketDisplayNameIndex = it.getColumnIndex(Media.BUCKET_DISPLAY_NAME)
                    val dateModifiedIndex = it.getColumnIndex(Media.DATE_MODIFIED)

                    val buckets = mutableMapOf<Long, VideoBucket>()

                    do {
                        try {
                            val bucketId = it.getLong(bucketIdIndex)
                            if (buckets.containsKey(bucketId)) continue

                            buckets[bucketId] = VideoBucket(
                                it.getLong(bucketIdIndex),
                                it.getString(bucketDisplayNameIndex),
                                it.getLong(dateModifiedIndex),
                                ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, it.getLong(idIndex))
                            )
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    } while (it.moveToNext())

                    val result = buckets.values.toMutableList()
                    result.sortBy { bucket -> bucket.name }

                    return result
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return emptyList()
    }

    fun getVideos(bucket: VideoBucket): List<Video> {
        try {
            context.contentResolver.query(
                Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    Media._ID,
                    Media.TITLE,
                    Media.DISPLAY_NAME,
                    Media.SIZE,
                    Media.DURATION,
                    Media.MIME_TYPE,
                    Media.DATE_MODIFIED,
                ),
                "${Media.BUCKET_ID} = ?",
                arrayOf(bucket.id.toString()),
                "${Media.DATE_MODIFIED} DESC"
            )?.use {
                if (it.moveToFirst()) {
                    val idIndex = it.getColumnIndex(Media._ID)
                    val titleIndex = it.getColumnIndex(Media.TITLE)
                    val displayNameIndex = it.getColumnIndex(Media.DISPLAY_NAME)
                    val sizeIndex = it.getColumnIndex(Media.SIZE)
                    val durationIndex = it.getColumnIndex(Media.DURATION)
                    val mimeTypeIndex = it.getColumnIndex(Media.MIME_TYPE)
                    val dateModifiedIndex = it.getColumnIndex(Media.DATE_MODIFIED)

                    val list = ArrayList<Video>(it.count)

                    do {
                        try {
                            val id = it.getLong(idIndex)
                            val title = it.getString(titleIndex)
                            val displayName = it.getString(displayNameIndex) ?: title

                            list.add(
                                Video(
                                    id,
                                    title,
                                    displayName,
                                    it.getLong(sizeIndex),
                                    it.getInt(durationIndex),
                                    it.getString(mimeTypeIndex),
                                    it.getLong(dateModifiedIndex),
                                    ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id)
                                )
                            )
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    } while (it.moveToNext())

                    return list
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return emptyList()
    }
}

@Parcelize
data class VideoBucket(
    val id: Long,
    val name: String,
    val dateModified: Long,
    val thumbnailUri: Uri,
) : Parcelable

@Parcelize
data class Video(
    val id: Long,
    val title: String,
    val displayName: String,
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
