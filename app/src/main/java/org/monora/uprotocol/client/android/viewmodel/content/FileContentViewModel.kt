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

package org.monora.uprotocol.client.android.viewmodel.content

import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.genonbeta.android.framework.util.Files
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.util.MimeIcons
import org.monora.uprotocol.core.transfer.TransferItem

class FileContentViewModel(fileModel: FileModel) {
    val name = fileModel.file.getName()

    val count = fileModel.indexCount

    val isDirectory = fileModel.file.isDirectory()

    val mimeType = fileModel.file.getType()

    val icon = if (isDirectory) R.drawable.ic_folder_white_24dp else MimeIcons.loadMimeIcon(mimeType)

    val indexCount = fileModel.indexCount

    val sizeText by lazy {
        Files.formatLength(fileModel.file.getLength(), false)
    }

    val uri = fileModel.file.getUri()
}
