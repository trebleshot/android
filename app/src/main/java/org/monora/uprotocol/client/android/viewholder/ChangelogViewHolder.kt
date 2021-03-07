package org.monora.uprotocol.client.android.viewholder

import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.databinding.ListChangelogBinding
import org.monora.uprotocol.client.android.remote.model.Release
import org.monora.uprotocol.client.android.viewmodel.content.ChangelogContentViewModel

class ChangelogViewHolder(private val binding: ListChangelogBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(release: Release) {
        binding.viewModel = ChangelogContentViewModel(release)
        binding.executePendingBindings()
    }
}