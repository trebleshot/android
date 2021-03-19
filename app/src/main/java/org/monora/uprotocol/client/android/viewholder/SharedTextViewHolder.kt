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

package org.monora.uprotocol.client.android.viewholder

import android.content.Intent
import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.databinding.ListSharedTextBinding
import org.monora.uprotocol.client.android.viewmodel.content.SharedTextContentViewModel

class SharedTextViewHolder(private val binding: ListSharedTextBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(sharedText: SharedText) {
        binding.viewModel = SharedTextContentViewModel(sharedText) { view ->
            view.context.startActivity(
                Intent(view.context, TextEditorActivity::class.java)
                    .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                    .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, sharedText)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        binding.executePendingBindings()
    }
}