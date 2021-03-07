package org.monora.uprotocol.client.android.binding

import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.preference.PreferenceManager
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.util.Drawables
import org.monora.uprotocol.client.android.util.Graphics
import org.monora.uprotocol.client.android.viewmodel.UserProfileViewModel
import org.monora.uprotocol.core.protocol.Client

@BindingAdapter("listenNicknameChanges")
fun listenNicknameChanges(editText: EditText, viewModel: UserProfileViewModel) {
    editText.addTextChangedListener { editable ->
        val nickname = editable.toString().also { if (it.isEmpty()) return@addTextChangedListener }

        PreferenceManager.getDefaultSharedPreferences(editText.context).edit()
            .putString("client_nickname", nickname)
            .apply()
    }
}

@BindingAdapter("pictureOf")
fun loadPictureOfClient(imageView: ImageView, client: Client) {
    try {
        val default = Graphics.createIconBuilder(imageView.context).buildRound(client.clientNickname)
        val picture = imageView.context.getFileStreamPath(Drawables.clientPicturePath(client))

        if (picture.exists() && picture.length() > 0) {
            GlideApp.with(imageView)
                .load(picture)
                .circleCrop()
                .error(default)
                .into(imageView)
        } else {
            imageView.setImageDrawable(default)
        }
    } catch (ignored: Exception) {
    }
}