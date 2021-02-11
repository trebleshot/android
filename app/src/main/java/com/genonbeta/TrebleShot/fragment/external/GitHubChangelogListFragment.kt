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

/**
 * created by: veli
 * date: 9/12/18 5:51 PM
 */
class GitHubChangelogListFragment :
    DynamicRecyclerViewFragment<VersionObject?, RecyclerViewAdapter.ViewHolder?, VersionListAdapter?>() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return generateDefaultView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listAdapter = VersionListAdapter(context)
        setEmptyListImage(R.drawable.ic_github_circle_white_24dp)
        setEmptyListText(getString(R.string.mesg_noInternetConnection))
        useEmptyListActionButton(getString(R.string.butn_refresh)) { v: View? -> refreshList() }
    }

    override fun onResume() {
        super.onResume()
        AppUtils.publishLatestChangelogSeen(activity)
    }

    class VersionObject(var tag: String, var name: String, var changes: String)
    class VersionListAdapter(context: Context?) :
        RecyclerViewAdapter<VersionObject, RecyclerViewAdapter.ViewHolder>(context) {
        private val mList: MutableList<VersionObject> = ArrayList()
        private val mCurrentVersion: String?
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(inflater.inflate(R.layout.list_changelog, parent, false))
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val versionObject = list[position]
            val imageCheck = holder.itemView.findViewById<ImageView>(R.id.image_check)
            val text1: TextView = holder.itemView.findViewById<TextView>(R.id.text1)
            val text2: TextView = holder.itemView.findViewById<TextView>(R.id.text2)
            text1.setText(versionObject.name)
            text2.setText(versionObject.changes)
            imageCheck.visibility = if (mCurrentVersion == versionObject.tag) View.VISIBLE else View.GONE
        }

        override fun onLoad(): List<VersionObject> {
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

        override fun onUpdate(passedItem: List<VersionObject>) {
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
            return mList
        }

        init {
            mCurrentVersion = AppUtils.getLocalDevice(context).versionName
        }
    }
}