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
import android.net.Uri
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.data.ClientRepository
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

    fun deleteLocalClientPicture(context: Context) {
        context.deleteFile(UserDataRepository.FILE_CLIENT_PICTURE)
        changeLocalClientPictureChecksum(context, 0)
    }

    fun saveClientPictureLocal(context: Context, uri: Uri) {
        GlideApp.with(context).load(uri)
            .centerCrop()
            .override(200, 200)
            .into(LocalPictureTarget(context))
    }

    suspend fun saveClientPicture(
        context: Context,
        clientRepository: ClientRepository,
        client: Client,
        data: ByteArray?,
        checksum: Int,
    ) {
        if (client !is UClient) throw UnsupportedOperationException()

        if (data == null) {
            clientRepository.update(
                client.also {
                    it.pictureFile?.delete()
                    it.pictureFile = null
                    it.checksum = checksum
                }
            )
        }

        val path = UUID.randomUUID().toString()

        GlideApp.with(context)
            .load(data)
            .centerCrop()
            .override(200, 200)
            .into(PictureTarget(context, clientRepository, client, checksum, path))
    }
}

private fun changeLocalClientPictureChecksum(context: Context, checksum: Int) {
    PreferenceManager.getDefaultSharedPreferences(context).edit {
        putInt(UserDataRepository.KEY_PICTURE_CHECKSUM, checksum)
    }
}

private fun processNewPicture(context: Context, path: String, resource: Drawable): Boolean {
    if (resource !is BitmapDrawable) throw IllegalStateException()

    try {
        context.openFileOutput(path, Context.MODE_PRIVATE).use { outputStream ->
            resource.bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        }

        return true
    } catch (e: Exception) {
        e.printStackTrace()
    }

    return false
}

private class PictureTarget(
    private val context: Context,
    private val clientRepository: ClientRepository,
    private val client: UClient,
    private val checksum: Int,
    private val path: String,
) : CustomTarget<Drawable>() {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        if (processNewPicture(context, path, resource)) {
            GlobalScope.launch(Dispatchers.IO) {
                clientRepository.update(
                    client.also {
                        it.pictureFile = context.getFileStreamPath(path)
                        it.checksum = checksum
                    }
                )
            }
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {}
}

private class LocalPictureTarget(private val context: Context) : CustomTarget<Drawable>() {
    override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
        if (processNewPicture(context, UserDataRepository.FILE_CLIENT_PICTURE, resource)) {
            GlobalScope.launch(Dispatchers.IO) {
                context.openFileInput(UserDataRepository.FILE_CLIENT_PICTURE).use {
                    changeLocalClientPictureChecksum(context, it.readBytes().contentHashCode())
                }
            }
        }
    }

    override fun onLoadCleared(placeholder: Drawable?) {}
}