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