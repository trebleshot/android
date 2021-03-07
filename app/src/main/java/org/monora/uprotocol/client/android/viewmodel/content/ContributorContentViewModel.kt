package org.monora.uprotocol.client.android.viewmodel.content

import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.remote.model.Contributor

class ContributorContentViewModel(contributor: Contributor) {
    val avatarUrl = contributor.urlAvatar

    val name = contributor.name

    val visitProfileListener = View.OnClickListener {
        it.context.startActivity(
            Intent(Intent.ACTION_VIEW).setData(
                Uri.parse(String.format(AppConfig.URI_GITHUB_PROFILE, contributor.name))
            )
        )
    }
}

@BindingAdapter("loadHttpImage")
fun loadHttpImage(imageView: ImageView, url: String) {
    GlideApp.with(imageView.context)
        .load(url)
        .override(90)
        .circleCrop()
        .into(imageView)
}