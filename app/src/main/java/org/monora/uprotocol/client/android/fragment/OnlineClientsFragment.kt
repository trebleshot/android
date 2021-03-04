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
package org.monora.uprotocol.client.android.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.monora.uprotocol.client.android.GlideApp
import org.monora.uprotocol.client.android.R
import org.monora.uprotocol.client.android.database.AppDatabase
import org.monora.uprotocol.client.android.database.model.UClient
import org.monora.uprotocol.client.android.drawable.TextDrawable.IShapeBuilder
import org.monora.uprotocol.client.android.itemcallback.UClientItemCallback
import org.monora.uprotocol.client.android.util.Graphics
import javax.inject.Inject

/**
 * created by: veli
 * date: 3/11/19 7:43 PM
 */
@AndroidEntryPoint
class OnlineClientsFragment : Fragment(R.layout.layout_online_client) {
    @Inject
    lateinit var appDatabase: AppDatabase

    private val imageBuilder by lazy {
        Graphics.getDefaultIconBuilder(requireContext())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        val adapter = Adapter(imageBuilder)

        recyclerView.adapter = adapter
        recyclerView.layoutManager?.let {
            if (it is GridLayoutManager) {
                it.orientation = GridLayoutManager.HORIZONTAL
            }
        }

        lifecycleScope.launch(Dispatchers.IO) {
            adapter.submitList(appDatabase.clientDao().getAll())
        }
    }

    class Adapter(private val imageBuilder: IShapeBuilder) : ListAdapter<UClient, ViewHolder>(UClientItemCallback()) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            return ViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.list_client_grid, parent, false)
            )
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val model = getItem(position)
            val textView1: TextView = holder.itemView.findViewById(R.id.text1)
            val textView2: TextView = holder.itemView.findViewById(R.id.text2)
            val imageView: ImageView = holder.itemView.findViewById(R.id.image)

            textView1.text = model.nickname
            textView2.text = model.clientType.name
            imageView.setImageDrawable(imageBuilder.buildRound(model.nickname))
        }

        override fun getItemId(position: Int): Long {
            return getItem(position).uid.hashCode().toLong()
        }
    }
}

