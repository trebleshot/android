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
package com.genonbeta.TrebleShot.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.PathResolverRecyclerAdapter.Holder
import java.util.*

/**
 * Created by: veli
 * Date: 5/29/17 4:29 PM
 */
abstract class PathResolverRecyclerAdapter<T>(val context: Context) : RecyclerView.Adapter<Holder<T>>() {
    var clickListener: OnClickListener<T>? = null

    val list: MutableList<Index<T>> = ArrayList()

    /*
     * To fix issues with the RecyclerView not appearing, the first item must be provided
     * when dealing with wrap_content height.
     */
    abstract fun onFirstItem(): Index<T>

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<T> {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.list_pathresolver, null))
    }

    override fun onBindViewHolder(holder: Holder<T>, position: Int) {
        holder.index = list[position]
        holder.text.text = holder.index.title
        holder.image.setImageResource(holder.index.imgRes)
        holder.image.visibility = if (position == 0) View.GONE else View.VISIBLE
        holder.text.isEnabled = itemCount - 1 != position
        holder.text.setOnClickListener { clickListener?.onClick(holder) }
    }

    fun initAdapter() {
        synchronized(list) {
            list.clear()
            list.add(onFirstItem())
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    interface OnClickListener<E> {
        fun onClick(holder: Holder<E>)
    }

    class Holder<E> constructor(var container: View, ) : RecyclerView.ViewHolder(container) {
        var image: ImageView = container.findViewById(R.id.list_pathresolver_image)
        var text: Button = container.findViewById(R.id.list_pathresolver_text)
        lateinit var index: Index<E>
    }

    class Index<D>(var title: String, var data: D, var imgRes: Int = R.drawable.ic_keyboard_arrow_right_white_24dp)

    init {
        initAdapter()
    }
}