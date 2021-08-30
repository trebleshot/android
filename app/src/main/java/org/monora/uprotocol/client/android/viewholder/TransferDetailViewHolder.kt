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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.database.model.TransferDetail
import org.monora.uprotocol.client.android.databinding.ListTransferBinding
import org.monora.uprotocol.client.android.viewmodel.content.TransferDetailContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferStateContentViewModel
import org.monora.uprotocol.client.android.viewmodel.content.TransferStateFeederViewModel

class TransferDetailViewHolder(
    private val gibSubscriberListener: (detail: TransferDetail) -> LiveData<TransferStateContentViewModel>,
    private val clickListener: (TransferDetail, ClickType) -> Unit,
    private val binding: ListTransferBinding,
) : RecyclerView.ViewHolder(binding.root), LifecycleOwner {
    // FIXME: 7/28/21 ViewHolder lifecycle isn't called when user leaves the activity
    private val lifecycleRegistry = LifecycleRegistry(this).apply {
        currentState = Lifecycle.State.INITIALIZED
    }

    fun onAppear() {
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
    }

    fun onDisappear() {
        lifecycleRegistry.currentState = Lifecycle.State.STARTED
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
    }

    fun onDestroy() {
        // FIXME: 7/28/21 Recycled views are still being used for some reason and destroyed state is not reusable
        //lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
    }

    fun bind(transferDetail: TransferDetail) {
        binding.viewModel = TransferDetailContentViewModel(transferDetail)
        binding.feederModel = TransferStateFeederViewModel(gibSubscriberListener(transferDetail))
        binding.lifecycleOwner = this
        binding.container.setOnClickListener {
            clickListener(transferDetail, ClickType.Default)
        }
        binding.rejectButton.setOnClickListener {
            clickListener(transferDetail, ClickType.Reject)
        }

        val toggleListener = View.OnClickListener {
            clickListener(transferDetail, ClickType.ToggleTask)
        }
        binding.acceptButton.setOnClickListener(toggleListener)
        binding.toggleButton.setOnClickListener(toggleListener)

        binding.executePendingBindings()
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

    enum class ClickType {
        Default,
        ToggleTask,
        Reject,
    }
}
