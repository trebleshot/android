package org.monora.uprotocol.client.android.viewholder

import androidx.recyclerview.widget.RecyclerView
import org.monora.uprotocol.client.android.databinding.ListSectionDateBinding
import org.monora.uprotocol.client.android.model.DateSectionContentModel

class DateSectionViewHolder(val binding: ListSectionDateBinding) : RecyclerView.ViewHolder(binding.root) {
    fun bind(contentModel: DateSectionContentModel) = with(binding) {
        model = contentModel
        executePendingBindings()
    }
}