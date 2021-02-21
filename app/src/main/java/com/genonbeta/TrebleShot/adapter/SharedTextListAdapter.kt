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

import android.text.format.DateUtils
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IListingFragment
import org.monora.uprotocol.client.android.database.model.SharedTextModel
import com.genonbeta.TrebleShot.widget.ListingAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
class SharedTextListAdapter(
    fragment: IListingFragment<SharedTextModel, ViewHolder>,
) : ListingAdapter<SharedTextModel, ViewHolder>(fragment) {
    override fun onLoad(): MutableList<SharedTextModel> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val holder = ViewHolder(layoutInflater.inflate(R.layout.list_text_stream, parent, false))
        fragment.registerLayoutViewClicks(holder)
        return holder
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val parentView: View = holder.itemView
        val text: String = item.text.replace("\n", " ").trim { it <= ' ' }
        val text1: TextView = parentView.findViewById(R.id.text)
        val text2: TextView = parentView.findViewById(R.id.text2)
        val text3: TextView = parentView.findViewById(R.id.text3)
        parentView.isSelected = item.selected()
        text1.text = text
        text2.text = DateUtils.formatDateTime(context, item.modified, DateUtils.FORMAT_SHOW_TIME)
    }
}