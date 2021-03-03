package org.monora.uprotocol.client.android.itemcallback

import androidx.recyclerview.widget.DiffUtil
import org.monora.uprotocol.client.android.database.model.UClient

class UClientItemCallback : DiffUtil.ItemCallback<UClient>() {
    override fun areItemsTheSame(oldItem: UClient, newItem: UClient): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: UClient, newItem: UClient): Boolean {
        return oldItem == newItem
    }
}