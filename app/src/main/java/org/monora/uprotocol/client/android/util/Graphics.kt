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

package org.monora.uprotocol.client.android.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.drawable.TextDrawable
import org.monora.uprotocol.client.android.util.Resources.attrToRes
import org.monora.uprotocol.client.android.util.Resources.resToColor
import org.monora.uprotocol.core.protocol.Client
import java.util.*

object Graphics {
    fun createIconBuilder(context: Context) = TextDrawable.createBuilder().apply {
        textFirstLetters = true
        textMaxLength = 2
        textBold = true
        textColor = R.attr.colorControlNormal.attrToRes(context).resToColor(context)
        shapeColor = R.attr.colorPassive.attrToRes(context).resToColor(context)
    }

    fun saveClientPicture(context: Context, client: Client, data: ByteArray?) {
        if (client !is UClient) throw UnsupportedOperationException()

        if (data == null) {
            context.deleteFile(client.picturePath)
        } else {
            GlideApp.with(context)
                .load(data)
                .centerCrop()
                .override(200, 200)
                .into(PictureTarget(context, client))
        }
    }
}

private class PictureTarget(private val context: Context, private val client: UClient) : CustomTarget<Drawable>() {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        if (resource !is BitmapDrawable) throw IllegalStateException()

        try {
            context.openFileOutput(client.picturePath, Context.MODE_PRIVATE).use { outputStream ->
                resource.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {}
}

val Client.picturePath: String
    get() = "picture_${clientUid.hashCode()}.png"
