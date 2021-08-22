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

import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.adapter.FileAdapter
import org.monora.uprotocol.client.android.databinding.ListFileNouveauBinding
import org.monora.uprotocol.client.android.model.FileModel
import org.monora.uprotocol.client.android.viewmodel.content.FileContentViewModel

class FileViewHolder(
    private val binding: ListFileNouveauBinding,
    private val clickListener: (FileModel, FileAdapter.ClickType) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(fileModel: FileModel) {
        binding.viewModel = FileContentViewModel(fileModel)
        binding.root.setOnClickListener {
            clickListener(fileModel, FileAdapter.ClickType.Default)
        }
        binding.selection.setOnClickListener {
            fileModel.isSelected = !fileModel.isSelected
            it.isSelected = fileModel.isSelected
            clickListener(fileModel, FileAdapter.ClickType.ToggleSelect)
        }
        binding.selection.isSelected = fileModel.isSelected
        binding.executePendingBindings()
    }
}
