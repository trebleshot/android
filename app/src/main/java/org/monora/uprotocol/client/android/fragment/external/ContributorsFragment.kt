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
package org.monora.uprotocol.client.android.fragment.external

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.databinding.ListContributorsBinding
import org.monora.uprotocol.client.android.remote.model.Contributor
import org.monora.uprotocol.client.android.viewholder.ContributorViewHolder
import org.monora.uprotocol.client.android.viewmodel.ContributorsViewModel

/**
 * created by: Veli
 * date: 16.03.2018 15:46
 */
@AndroidEntryPoint
class ContributorsFragment : Fragment(R.layout.layout_contributors) {
    private val viewModel: ContributorsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = ContributorsAdapter()

        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true

        viewModel.contributors.observe(viewLifecycleOwner) { result ->
            adapter.submitList(result)
        }
    }

    class ContributorsAdapter : ListAdapter<Contributor, ContributorViewHolder>(ContributorItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContributorViewHolder {
            return ContributorViewHolder(
                ListContributorsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: ContributorViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).hashCode().toLong()
        }
    }
}

class ContributorItemCallback : DiffUtil.ItemCallback<Contributor>() {
    override fun areItemsTheSame(oldItem: Contributor, newItem: Contributor): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Contributor, newItem: Contributor): Boolean {
        return oldItem.name == newItem.name && oldItem.urlAvatar == newItem.urlAvatar && oldItem.url == newItem.url
    }
}