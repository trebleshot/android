/*
 * Copyright (C) 2019 Veli Tasalı
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
package org.monora.uprotocol.client.android.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.monora.uprotocol.client.android.activity.TextEditorActivity
import org.monora.uprotocol.client.android.database.model.SharedTextModel
import org.monora.uprotocol.client.android.databinding.ListSharedTextBinding
import org.monora.uprotocol.client.android.viewmodel.SharedTextViewModel

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
class SharedTextListAdapter : ListAdapter<SharedTextModel, ViewHolder>(SharedTextDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = SharedTextViewHolder(
        ListSharedTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        (holder as SharedTextViewHolder).bind(getItem(position))
    }

    class SharedTextViewHolder(private val binding: ListSharedTextBinding) : ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener { view ->
                binding.viewModel?.sharedText?.let {
                    view.context.startActivity(
                        Intent(view.context, TextEditorActivity::class.java)
                            .setAction(TextEditorActivity.ACTION_EDIT_TEXT)
                            .putExtra(TextEditorActivity.EXTRA_TEXT_MODEL, it)
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }

        fun bind(sharedTextModel: SharedTextModel) = with(binding) {
            viewModel = SharedTextViewModel(sharedTextModel)
            executePendingBindings()
        }
    }
}

private class SharedTextDiffCallback : DiffUtil.ItemCallback<SharedTextModel>() {
    override fun areItemsTheSame(oldItem: SharedTextModel, newItem: SharedTextModel): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: SharedTextModel, newItem: SharedTextModel): Boolean {
        return oldItem == newItem
    }
}
