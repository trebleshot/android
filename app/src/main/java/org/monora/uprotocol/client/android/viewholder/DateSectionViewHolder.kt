package org.monora.uprotocol.client.android.viewholder

import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.databinding.ListSectionDateBinding
import org.monora.uprotocol.client.android.model.DateSectionContentModel
import org.monora.uprotocol.client.android.viewmodel.content.DateSectionContentViewModel

class DateSectionViewHolder(val binding: ListSectionDateBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(contentModel: DateSectionContentModel) {
        binding.viewModel = DateSectionContentViewModel(contentModel)
        binding.executePendingBindings()
    }
}