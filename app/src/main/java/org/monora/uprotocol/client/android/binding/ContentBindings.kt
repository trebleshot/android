package org.monora.uprotocol.client.android.binding

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.util.MimeIcons
import org.monora.uprotocol.core.transfer.TransferItem

@BindingAdapter("thumbnailOf")
fun loadThumbnailOf(imageView: ImageView, transferItem: UTransferItem) {
    if (transferItem.type == TransferItem.Type.Outgoing) {
        GlideApp.with(imageView)
            .load(transferItem.location)
            .circleCrop()
            .into(imageView)
    }
}

@BindingAdapter("iconOf")
fun loadIconOf(imageView: ImageView, transferItem: UTransferItem) {
    imageView.setImageResource(MimeIcons.loadMimeIcon(transferItem.mimeType))
}