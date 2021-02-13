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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.config.AppConfig
import com.genonbeta.TrebleShot.fragment.external.GitHubChangelogListFragment.VersionListAdapter
import com.genonbeta.TrebleShot.fragment.external.GitHubChangelogListFragment.VersionObject
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.android.framework.app.DynamicRecyclerViewFragment
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import com.genonbeta.android.updatewithgithub.RemoteServer
import org.json.JSONArray

/**
 * created by: veli
 * date: 9/12/18 5:51 PM
 */
class GitHubChangelogListFragment : DynamicRecyclerViewFragment<VersionObject, ViewHolder, VersionListAdapter>() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View? {
        return generateDefaultView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = VersionListAdapter(requireContext())
        emptyListImageView.setImageResource(R.drawable.ic_github_circle_white_24dp)
        emptyListTextView.text = getString(R.string.mesg_noInternetConnection)
        useEmptyListActionButton(getString(R.string.butn_refresh)) { v: View? -> refreshList() }
    }

    override fun onResume() {
        super.onResume()
        AppUtils.publishLatestChangelogSeen(requireActivity())
    }

    class VersionObject(var tag: String, var name: String, var changes: String)

    class VersionListAdapter(context: Context) : RecyclerViewAdapter<VersionObject, ViewHolder>(context) {
        private val list: MutableList<VersionObject> = ArrayList()

        private val currentVersion = AppUtils.getLocalDevice(context).versionName

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(layoutInflater.inflate(R.layout.list_changelog, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val versionObject = list[position]
            val imageCheck = holder.itemView.findViewById<ImageView>(R.id.image_check)
            val text1 = holder.itemView.findViewById<TextView>(R.id.text1)
            val text2 = holder.itemView.findViewById<TextView>(R.id.text2)
            text1.text = versionObject.name
            text2.text = versionObject.changes
            imageCheck.visibility = if (currentVersion == versionObject.tag) View.VISIBLE else View.GONE
        }

        override fun onLoad(): MutableList<VersionObject> {
            val versionObjects: MutableList<VersionObject> = ArrayList()
            val server = RemoteServer(AppConfig.URI_REPO_APP_UPDATE)
            try {
                val result = server.connect(null, null)
                val releases = JSONArray(result)
                if (releases.length() > 0) {
                    for (iterator in 0 until releases.length()) {
                        val currentObject = releases.getJSONObject(iterator)
                        versionObjects.add(
                            VersionObject(
                                currentObject.getString("tag_name"),
                                currentObject.getString("name"),
                                currentObject.getString("body")
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return versionObjects
        }

        override fun onUpdate(passedItem: MutableList<VersionObject>) {
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

        override fun getList(): MutableList<VersionObject> {
            return list
        }
    }
}