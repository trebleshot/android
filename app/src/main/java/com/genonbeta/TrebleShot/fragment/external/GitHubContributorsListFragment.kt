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
package com.genonbeta.TrebleShot.fragment.external

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.GlideApp
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.fragment.external.GitHubContributorsListFragment.ContributorListAdapter
import com.genonbeta.TrebleShot.fragment.external.GitHubContributorsListFragment.Contributor
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.*
import com.genonbeta.android.updatewithgithub.RemoteServer
import org.json.JSONArray

/**
 * created by: Veli
 * date: 16.03.2018 15:46
 */
class GitHubContributorsListFragment : DynamicRecyclerViewFragment<Contributor, ViewHolder, ContributorListAdapter>() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        return generateDefaultView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ContributorListAdapter(requireContext())
        emptyListImageView.setImageResource(R.drawable.ic_github_circle_white_24dp)
        emptyListTextView.text = getString(R.string.mesg_noInternetConnection)
        useEmptyListActionButton(getString(R.string.butn_refresh)) { v: View? -> refreshList() }
        listView.isNestedScrollingEnabled = true
    }

    override fun getLayoutManager(): RecyclerView.LayoutManager {
        return GridLayoutManager(context, 1)
    }

    data class Contributor(var name: String, var url: String, var urlAvatar: String)

    class ContributorListAdapter(context: Context) : RecyclerViewAdapter<Contributor, ViewHolder>(context) {
        private val list: MutableList<Contributor> = ArrayList()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(
                layoutInflater.inflate(R.layout.list_contributors, parent, false)
            )
            holder.itemView.findViewById<View>(R.id.visitView).setOnClickListener { v: View? ->
                val contributorObject = list[holder.adapterPosition]
                context.startActivity(
                    Intent(Intent.ACTION_VIEW)
                        .setData(Uri.parse(String.format(AppConfig.URI_GITHUB_PROFILE, contributorObject.name)))
                )
            }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val contributorObject = list[position]
            val textView = holder.itemView.findViewById<TextView>(R.id.text)
            val imageView = holder.itemView.findViewById<ImageView>(R.id.image)
            textView.text = contributorObject.name
            GlideApp.with(context)
                .load(contributorObject.urlAvatar)
                .override(90)
                .circleCrop()
                .into(imageView)
        }

        override fun onLoad(): MutableList<Contributor> {
            val contributors: MutableList<Contributor> = ArrayList()
            val server = RemoteServer(AppConfig.URI_REPO_APP_CONTRIBUTORS)
            try {
                val result = server.connect(null, null)
                val releases = JSONArray(result)
                if (releases.length() > 0) {
                    for (iterator in 0 until releases.length()) {
                        val currentObject = releases.getJSONObject(iterator)
                        contributors.add(
                            Contributor(
                                currentObject.getString("login"),
                                currentObject.getString("url"),
                                currentObject.getString("avatar_url")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return contributors
        }

        override fun onUpdate(passedItem: MutableList<Contributor>) {
            synchronized(list) {
                list.clear()
                list.addAll(passedItem)
            }
        }

        override fun getItemId(i: Int): Long {
            return 0
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun getList(): MutableList<Contributor> {
            return list
        }
    }
}