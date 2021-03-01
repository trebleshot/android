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

import android.content.Intent
import android.net.Uri
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
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.config.AppConfig
import org.monora.uprotocol.client.android.remote.model.Contributor
import org.monora.uprotocol.client.android.viewmodel.ContributorsDataViewModel

/**
 * created by: Veli
 * date: 16.03.2018 15:46
 */
@AndroidEntryPoint
class GitHubContributorsFragment : Fragment(R.layout.layout_contributors) {
    private val viewModel: ContributorsDataViewModel by viewModels()

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

    class ContributorsAdapter : ListAdapter<Contributor, ViewHolder>(ContributorItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.list_contributors, parent, false)
            )
            holder.itemView.findViewById<View>(R.id.visitView).setOnClickListener { v: View ->
                val contributorObject = getItem(holder.adapterPosition)
                v.context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(String.format(AppConfig.URI_GITHUB_PROFILE, contributorObject.name)))
                )
            }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contributorObject = getItem(position)
            val textView = holder.itemView.findViewById<TextView>(R.id.text)
            val imageView = holder.itemView.findViewById<ImageView>(R.id.image)
            textView.text = contributorObject.name
            GlideApp.with(holder.itemView.context)
                .load(contributorObject.urlAvatar)
                .override(90)
                .circleCrop()
                .into(imageView)
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