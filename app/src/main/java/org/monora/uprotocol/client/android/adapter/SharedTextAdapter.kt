/*
 * Copyright (C) 2019 Veli Tasalı
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
package org.monora.uprotocol.client.android.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.monora.uprotocol.client.android.database.model.SharedText
import org.monora.uprotocol.client.android.databinding.ListSectionDateBinding
import org.monora.uprotocol.client.android.databinding.ListSharedTextBinding
import org.monora.uprotocol.client.android.itemcallback.ContentModelItemCallback
import org.monora.uprotocol.client.android.model.ContentModel
import org.monora.uprotocol.client.android.model.DateSectionContentModel
import org.monora.uprotocol.client.android.viewholder.DateSectionViewHolder
import org.monora.uprotocol.client.android.viewholder.SharedTextViewHolder

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
class SharedTextAdapter : ListAdapter<ContentModel, ViewHolder>(ContentModelItemCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = when (viewType) {
        VIEW_TYPE_SHARED_TEXT -> SharedTextViewHolder(
            ListSharedTextBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        VIEW_TYPE_SECTION -> DateSectionViewHolder(
            ListSectionDateBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
        else -> throw UnsupportedOperationException()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is SharedText -> if (holder is SharedTextViewHolder) holder.bind(item)
            is DateSectionContentModel -> if (holder is DateSectionViewHolder) holder.bind(item)
        }
    }

    override fun getItemViewType(position: Int) = when (getItem(position)) {
        is SharedText -> VIEW_TYPE_SHARED_TEXT
        is DateSectionContentModel -> VIEW_TYPE_SECTION
        else -> throw UnsupportedOperationException()
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id()
    }

    companion object {
        const val VIEW_TYPE_SECTION = 0

        const val VIEW_TYPE_SHARED_TEXT = 1
    }
}