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