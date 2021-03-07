package org.monora.uprotocol.client.android.viewholder

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.databinding.ListContributorsBinding
import org.monora.uprotocol.client.android.remote.model.Contributor
import org.monora.uprotocol.client.android.viewmodel.content.ContributorContentViewModel

class ContributorViewHolder(private val binding: ListContributorsBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(contributor: Contributor) {
        binding.viewModel = ContributorContentViewModel(contributor)
        binding.executePendingBindings()
    }
}