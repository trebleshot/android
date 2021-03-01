package org.monora.uprotocol.client.android.remote.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.io.Serializable

@Parcelize
class Contributor(
    @field:SerializedName("login") var name: String,
    @field:SerializedName("url") var url: String,
    @field:SerializedName("avatar_url") var urlAvatar: String,
) : Parcelable, Serializable