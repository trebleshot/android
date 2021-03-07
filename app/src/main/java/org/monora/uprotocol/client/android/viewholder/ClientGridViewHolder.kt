package org.monora.uprotocol.client.android.viewholder

import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.ListClientGridBinding
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel

class ClientGridViewHolder(private val binding: ListClientGridBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(client: UClient) {
        binding.viewModel = ClientContentViewModel(client)
        binding.executePendingBindings()
    }
}