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

package org.monora.uprotocol.client.android.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ArtifactId(
    @field:SerializedName("name") var name: String? = null,
    @field:SerializedName("group") var group: String? = null,
    @field:SerializedName("version") var version: String? = null,
): Parcelable

@Parcelize
data class LibraryLicense(
    @field:SerializedName("artifactId") var artifactId: ArtifactId,
    @field:SerializedName("license") var license: String? = null,
    @field:SerializedName("licenseUrl") var licenseUrl: String? = null,
    @field:SerializedName("normalizedLicense") var normalizedLicense: String? = null,
    @field:SerializedName("url") var url: String? = null,
    @field:SerializedName("libraryName") var libraryName: String? = null,
) : Parcelable