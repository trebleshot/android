package org.monora.uprotocol.client.android.binding

import android.widget.EditText
import android.widget.ImageView
import androidx.core.widget.addTextChangedListener
import androidx.databinding.BindingAdapter
import androidx.preference.PreferenceManager
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.data.UserDataRepository
import org.monora.uprotocol.client.android.util.Graphics
import org.monora.uprotocol.client.android.viewmodel.UserProfileViewModel
import org.monora.uprotocol.core.protocol.Client

@BindingAdapter("listenNicknameChanges")
fun listenNicknameChanges(editText: EditText, viewModel: UserProfileViewModel) {
    editText.addTextChangedListener { editable ->
        val nickname = editable.toString().also { if (it.isEmpty()) return@addTextChangedListener }

        PreferenceManager.getDefaultSharedPreferences(editText.context).edit()
            .putString(UserDataRepository.KEY_NICKNAME, nickname)
            .apply()
    }
}

@BindingAdapter("pictureOf")
fun loadPictureOfClient(imageView: ImageView, client: Client?) {
    if (client == null) return

    try {
        val default = Graphics.createIconBuilder(imageView.context).buildRound(client.clientNickname)

        if (client.hasPicture()) {
            GlideApp.with(imageView)
                .load(client.clientPictureData)
                .circleCrop()
                .error(default)
                .into(imageView)
        } else {
            imageView.setImageDrawable(default)
        }
    } catch (ignored: Exception) {
    }
}