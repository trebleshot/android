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

import android.widget.ImageView
import androidx.databinding.BindingAdapter
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
            .circleCrop()
            .into(imageView)
    }
}

@BindingAdapter("iconOf")
fun loadIconOf(imageView: ImageView, transferItem: UTransferItem) {
    imageView.setImageResource(MimeIcons.loadMimeIcon(transferItem.mimeType))
}