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
import android.database.Cursor
import android.net.Uri
import android.os.Parcelable
import android.provider.MediaStore
import android.provider.MediaStore.Audio.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

class AudioStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun getAlbums(): List<Album> {
        return try {
            loadAlbums(
                context.contentResolver.query(
                    Albums.EXTERNAL_CONTENT_URI,
                    arrayOf(
                        Albums._ID,
                        Albums.ARTIST,
                        Albums.LAST_YEAR,
                        Albums.ALBUM,
                    ),
                    null,
                    null,
                    Albums.ALBUM
                )
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getAlbums(artist: Artist): List<Album> {
        return try {
            loadAlbums(
                context.contentResolver.query(
                    Artists.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL, artist.id),
                    arrayOf(
                        Albums._ID,
                        Albums.ARTIST,
                        Albums.LAST_YEAR,
                        Albums.ALBUM,
                    ),
                    null,
                    null,
                    Albums.ALBUM
                )
            )
        } catch (e: Throwable) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getArtists(): List<Artist> {
        try {
            context.contentResolver.query(
                Artists.EXTERNAL_CONTENT_URI,
                arrayOf(
                    Artists._ID,
                    Artists.ARTIST,
                    Artists.NUMBER_OF_ALBUMS,
                ),
                null,
                null,
                Albums.ARTIST
            )?.use {
                if (it.moveToFirst()) {
                    val idIndex: Int = it.getColumnIndex(Artists._ID)
                    val artistIndex: Int = it.getColumnIndex(Artists.ARTIST)
                    val numberOfAlbumsIndex: Int = it.getColumnIndex(Artists.NUMBER_OF_ALBUMS)

                    val result = ArrayList<Artist>(it.count)

                    do {
                        try {
                            val id = it.getLong(idIndex)

                            result.add(
                                Artist(
                                    id,
                                    it.getString(artistIndex),
                                    it.getInt(numberOfAlbumsIndex),
                                    ContentUris.withAppendedId(Artists.EXTERNAL_CONTENT_URI, id),
                                )
                            )
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    } while (it.moveToNext())

                    return result
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return emptyList()
    }

    fun getSongs(selection: String, selectionArgs: Array<String>): List<Song> {
        try {
            context.contentResolver.query(
                Media.EXTERNAL_CONTENT_URI,
                arrayOf(
                    Media._ID,
                    Media.ARTIST,
                    Media.ALBUM,
                    Media.ALBUM_ID,
                    Media.TITLE,
                    Media.DISPLAY_NAME,
                    Media.MIME_TYPE,
                    Media.SIZE,
                    Media.DATE_MODIFIED
                ),
                selection,
                selectionArgs,
                Media.TITLE
            )?.use {
                if (it.moveToFirst()) {
                    val idIndex: Int = it.getColumnIndex(Media._ID)
                    val artistIndex: Int = it.getColumnIndex(Media.ARTIST)
                    val albumIndex: Int = it.getColumnIndex(Media.ALBUM)
                    val albumIdIndex = it.getColumnIndex(Media.ALBUM_ID)
                    val titleIndex: Int = it.getColumnIndex(Media.TITLE)
                    val displayNameIndex: Int = it.getColumnIndex(Media.DISPLAY_NAME)
                    val mimeTypeIndex: Int = it.getColumnIndex(Media.MIME_TYPE)
                    val sizeIndex: Int = it.getColumnIndex(Media.SIZE)
                    val dateModifiedIndex: Int = it.getColumnIndex(Media.DATE_MODIFIED)

                    val result = ArrayList<Song>(it.count)

                    do {
                        try {
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
                                    ContentUris.withAppendedId(Media.EXTERNAL_CONTENT_URI, id),
                                    ContentUris.withAppendedId(Albums.EXTERNAL_CONTENT_URI, it.getLong(albumIdIndex))
                                )
                            )
                        } catch (e: Throwable) {
                            e.printStackTrace()
                        }
                    } while (it.moveToNext())

                    return result
                }
            }
        } catch (e: Throwable) {
            e.printStackTrace()
        }

        return emptyList()
    }

    private fun loadAlbums(cursor: Cursor?): List<Album> {
        if (cursor != null && cursor.moveToFirst()) {
            val idIndex: Int = cursor.getColumnIndex(Albums._ID)
            val artistIndex: Int = cursor.getColumnIndex(Albums.ARTIST)
            val albumIndex: Int = cursor.getColumnIndex(Albums.ALBUM)
            val lastYearIndex: Int = cursor.getColumnIndex(Albums.LAST_YEAR)

            val result = ArrayList<Album>(cursor.count)

            do {
                try {
                    val id = cursor.getLong(idIndex)

                    result.add(
                        Album(
                            id,
                            cursor.getString(artistIndex),
                            cursor.getString(albumIndex),
                            cursor.getInt(lastYearIndex),
                            ContentUris.withAppendedId(Albums.EXTERNAL_CONTENT_URI, id),
                        )
                    )
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            } while (cursor.moveToNext())

            return result
        }

        return emptyList()
    }
}

@Parcelize
data class Album(
    val id: Long,
    val artist: String,
    val title: String,
    val year: Int,
    val uri: Uri,
) : Parcelable

@Parcelize
data class Artist(
    val id: Long,
    val name: String,
    val numberOfAlbums: Int,
    val uri: Uri,
) : Parcelable

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
    val albumUri: Uri,
) : Parcelable {
    @IgnoredOnParcel
    var isSelected = false

    override fun equals(other: Any?): Boolean {
        return other is Song && uri == other.uri
    }
}
