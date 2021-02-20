package com.genonbeta.TrebleShot.model

import kotlinx.parcelize.Parcelize
import org.monora.uprotocol.client.android.model.ContentModel

@Parcelize
data class AudioMediaModel(
    val artist: String,
    var song: String,
    var folder: String,

) : ContentModel {
}