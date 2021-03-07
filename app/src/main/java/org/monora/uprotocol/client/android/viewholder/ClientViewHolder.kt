package org.monora.uprotocol.client.android.viewholder

import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.databinding.ListClientBinding
import org.monora.uprotocol.client.android.viewmodel.content.ClientContentViewModel

class ClientViewHolder(val binding: ListClientBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(client: UClient) {
        binding.viewModel = ClientContentViewModel(client)
        binding.executePendingBindings()
    }
}