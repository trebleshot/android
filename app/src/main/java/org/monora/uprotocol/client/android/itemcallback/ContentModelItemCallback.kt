package org.monora.uprotocol.client.android.itemcallback

import androidx.recyclerview.widget.DiffUtil
import org.monora.uprotocol.client.android.model.ContentModel

class ContentModelItemCallback : DiffUtil.ItemCallback<ContentModel>() {
    override fun areItemsTheSame(oldItem: ContentModel, newItem: ContentModel): Boolean {
        return oldItem.id() == newItem.id()
    }

    override fun areContentsTheSame(oldItem: ContentModel, newItem: ContentModel): Boolean {
        return oldItem.hashCode() == newItem.hashCode()
    }
}