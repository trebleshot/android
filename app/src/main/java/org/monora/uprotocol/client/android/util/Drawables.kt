package org.monora.uprotocol.client.android.util

import android.content.Context
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.core.protocol.Client
import java.io.InputStream

object Drawables {
    fun clientPicturePath(client: Client) = "photo_" + client.clientUid.hashCode()

    fun openClientPictureBitmapSource(context: Context, client: Client): InputStream {
        return context.openFileInput(clientPicturePath(client))
    }
}
