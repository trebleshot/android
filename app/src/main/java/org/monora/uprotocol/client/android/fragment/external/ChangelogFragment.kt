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
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import org.monora.uprotocol.client.android.BuildConfig
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.remote.model.Release
import org.monora.uprotocol.client.android.util.Updates
import org.monora.uprotocol.client.android.viewmodel.ReleasesDataViewModel

/**
 * created by: veli
 * date: 9/12/18 5:51 PM
 */
@AndroidEntryPoint
class ChangelogFragment : Fragment(R.layout.layout_changelog) {
    private val viewModel: ReleasesDataViewModel by viewModels()

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
        Updates.declareLatestChangelogAsShown(requireActivity())
    }

    class ReleasesAdapter : ListAdapter<Release, ViewHolder>(ReleaseItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.list_changelog, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val versionObject = getItem(position)
            val imageCheck = holder.itemView.findViewById<ImageView>(R.id.image_check)
            val text1 = holder.itemView.findViewById<TextView>(R.id.text1)
            val text2 = holder.itemView.findViewById<TextView>(R.id.text2)
            text1.text = versionObject.name
            text2.text = versionObject.changelog.trim()
            imageCheck.visibility = if (BuildConfig.VERSION_NAME == versionObject.tag) View.VISIBLE else View.GONE
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