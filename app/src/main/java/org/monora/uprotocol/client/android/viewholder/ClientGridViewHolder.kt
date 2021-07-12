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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.databinding.ListClientGridBinding
import org.monora.uprotocol.client.android.model.ClientRoute
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel.ClickType

class ClientGridViewHolder(
    private val binding: ListClientGridBinding,
    private val clickListener: (ClientRoute, ClickType) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(clientRoute: ClientRoute) {
        binding.viewModel = ClientContentViewModel(clientRoute.client)
        binding.clickListener = View.OnClickListener { clickListener(clientRoute, ClickType.Default) }
        binding.detailsClickListener = View.OnClickListener { clickListener(clientRoute, ClickType.Details) }
        binding.executePendingBindings()
    }
}