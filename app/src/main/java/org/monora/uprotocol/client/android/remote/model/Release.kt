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

package org.monora.uprotocol.client.android.remote.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Release(
    @field:SerializedName("tag_name") val tag: String,
    @field:SerializedName("name") val name: String,
    @field:SerializedName("body") val changelog: String,
    @field:SerializedName("prerelease") val prerelase: Boolean,
    @field:SerializedName("published_at") val publishDate: String,
    @field:SerializedName("html_url") val url: String,
    @field:SerializedName("assets") val assets: Array<Asset>?,
) : Parcelable

@Parcelize
data class Asset(
    @field:SerializedName("name") val name: String,
    @field:SerializedName("url") val url: String,
) : Parcelable