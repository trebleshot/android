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
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.*
import com.google.gson.Gson
import com.google.gson.stream.JsonReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.model.LibraryLicense
import java.io.InputStreamReader

/**
 * created by: veli
 * date: 7/20/18 8:56 PM
 */
class LicensesFragment : Fragment(R.layout.layout_licenses) {
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerView)
        val adapter = LicensesAdapter()

        adapter.setHasStableIds(true)
        recyclerView.adapter = adapter
        recyclerView.isNestedScrollingEnabled = true

        lifecycleScope.launch(Dispatchers.IO) {
            requireContext().assets.open("licenses.json").use { inputStream ->
                val jsonReader = JsonReader(InputStreamReader(inputStream))
                val gson = Gson()
                val list = mutableListOf<LibraryLicense>()

                try {
                    // skip to the "libraries" index
                    jsonReader.beginObject()

                    if (!jsonReader.hasNext()) {
                        return@use
                    } else if (jsonReader.nextName() != "libraries") {
                        Log.d(LicensesFragment::class.simpleName, "onViewCreated: 'libraries' does not exist")
                        return@use
                    }

                    jsonReader.beginArray()

                    while (jsonReader.hasNext()) {
                        list.add(gson.fromJson(jsonReader, LibraryLicense::class.java))
                    }

                    list.sortBy { it.artifactId.group }

                    jsonReader.endArray()
                    jsonReader.endObject()

                    withContext(Dispatchers.Main) {
                        adapter.submitList(list)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    list.forEach {
                        println(it)
                    }
                }
            }
        }
    }

    class LicensesAdapter : ListAdapter<LibraryLicense, ViewHolder>(LibraryLicenseItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val holder = ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.list_third_party_library, parent, false)
            )
            holder.itemView.findViewById<View>(R.id.menu).setOnClickListener { v: View ->
                val moduleItem = getItem(holder.adapterPosition)
                val popupMenu = PopupMenu(v.context, v)
                popupMenu.menuInflater.inflate(R.menu.popup_third_party_library_item, popupMenu.menu)
                popupMenu.menu.findItem(R.id.popup_visitWebPage).isEnabled = moduleItem.url != null
                popupMenu.menu.findItem(R.id.popup_goToLicenceURL).isEnabled = moduleItem.licenseUrl != null
                popupMenu.setOnMenuItemClickListener { item: MenuItem ->
                    when (item.itemId) {
                        R.id.popup_goToLicenceURL -> v.context.startActivity(
                            Intent(Intent.ACTION_VIEW).setData(Uri.parse(moduleItem.licenseUrl))
                        )
                        R.id.popup_visitWebPage -> v.context.startActivity(
                            Intent(Intent.ACTION_VIEW).setData(Uri.parse(moduleItem.url))
                        )
                        else -> return@setOnMenuItemClickListener false
                    }
                    true
                }
                popupMenu.show()
            }
            return holder
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = getItem(position)
            val text1 = holder.itemView.findViewById<TextView>(R.id.text)
            val text2 = holder.itemView.findViewById<TextView>(R.id.text2)
            text1.text = "${item.artifactId.group} / ${item.libraryName}"
            text2.text = item.license
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).hashCode().toLong()
        }
    }
}

class LibraryLicenseItemCallback : DiffUtil.ItemCallback<LibraryLicense>() {
    override fun areItemsTheSame(oldItem: LibraryLicense, newItem: LibraryLicense): Boolean {
        return oldItem === newItem
    }

    override fun areContentsTheSame(oldItem: LibraryLicense, newItem: LibraryLicense): Boolean {
        return oldItem == newItem
    }
}