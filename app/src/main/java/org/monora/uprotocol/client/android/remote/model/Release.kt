package org.monora.uprotocol.client.android.remote.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class Release(
    @field:SerializedName("tag_name") val tag: String,
    @field:SerializedName("name") val name: String,
    @field:SerializedName("body") val changelog: String,
) : Parcelable