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
import org.monora.uprotocol.client.android.databinding.ListChangelogBinding
import org.monora.uprotocol.client.android.remote.model.Release
import org.monora.uprotocol.client.android.util.Updater
import org.monora.uprotocol.client.android.viewholder.ChangelogViewHolder
import org.monora.uprotocol.client.android.viewmodel.ReleasesViewModel
import javax.inject.Inject

/**
 * created by: veli
 * date: 9/12/18 5:51 PM
 */
@AndroidEntryPoint
class ChangelogFragment : Fragment(R.layout.layout_changelog) {
    @Inject
    lateinit var updater: Updater

    private val viewModel: ReleasesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = ReleasesAdapter()

        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true

        viewModel.releases.observe(viewLifecycleOwner) { result ->
            adapter.submitList(result)
        }
    }

    override fun onResume() {
        super.onResume()
        updater.declareLatestChangelogAsShown()
    }

    class ReleasesAdapter : ListAdapter<Release, ChangelogViewHolder>(ReleaseItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChangelogViewHolder {
            return ChangelogViewHolder(
                ListChangelogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            )
        }

        override fun onBindViewHolder(holder: ChangelogViewHolder, position: Int) {
            holder.bind(getItem(position))
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).tag.hashCode().toLong()
        }
    }
}

class ReleaseItemCallback : DiffUtil.ItemCallback<Release>() {
    override fun areItemsTheSame(oldItem: Release, newItem: Release): Boolean {
        return oldItem == newItem
    }

    override fun areContentsTheSame(oldItem: Release, newItem: Release): Boolean {
        return oldItem.name == newItem.name && oldItem.tag == newItem.tag && oldItem.changelog == newItem.changelog
    }
}