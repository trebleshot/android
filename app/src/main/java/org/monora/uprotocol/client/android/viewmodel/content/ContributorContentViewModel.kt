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