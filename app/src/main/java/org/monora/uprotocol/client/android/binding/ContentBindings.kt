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

package org.monora.uprotocol.client.android.binding

import android.net.Uri
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.database.model.UTransferItem
import org.monora.uprotocol.client.android.util.MimeIcons
import org.monora.uprotocol.core.transfer.TransferItem.State.Done
import org.monora.uprotocol.core.transfer.TransferItem.Type.Outgoing

@BindingAdapter("thumbnailOf")
fun loadThumbnailOf(imageView: ImageView, item: UTransferItem) {
    if (item.mimeType.startsWith("image/") || item.mimeType.startsWith("video/")
        && (item.type == Outgoing || item.state == Done)
    ) {
        GlideApp.with(imageView)
            .load(item.location)
            .override(300)
            .circleCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .into(imageView)
    }
}

@BindingAdapter("thumbnailOf")
fun loadThumbnailOf(imageView: ImageView, uri: Uri) {
    GlideApp.with(imageView)
        .load(uri)
        .override(300)
        .apply(RequestOptions.centerCropTransform())
        .transition(DrawableTransitionOptions.withCrossFade())
        .into(imageView)
}

@BindingAdapter("iconOf")
fun loadIconOf(imageView: ImageView, mimeType: String) {
    imageView.setImageResource(MimeIcons.loadMimeIcon(mimeType))
}