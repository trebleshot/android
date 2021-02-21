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

import android.content.ContentResolver
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IListingFragment
import com.genonbeta.TrebleShot.model.AudioMediaModel
import com.genonbeta.TrebleShot.widget.ListingAdapter
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder
import java.io.File

class AudioListAdapter(
    fragment: IListingFragment<AudioMediaModel, ViewHolder>,
) : ListingAdapter<AudioMediaModel, ViewHolder>(fragment) {
    private val resolver: ContentResolver = context.contentResolver

    override fun onLoad(): MutableList<AudioMediaModel> {
        TODO("Not yet implemented")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(layoutInflater.inflate(R.layout.list_music, parent, false)).also { holder ->
            fragment.registerLayoutViewClicks(holder)
            holder.itemView.findViewById<View>(R.id.visitView).setOnClickListener {
                fragment.performLayoutClickOpen(holder)
            }
            holder.itemView.findViewById<View>(R.id.selector).setOnClickListener {
                fragment.setItemSelected(holder, true)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemHolder = getItem(position)
        val parentView = holder.itemView
        val image = parentView.findViewById<ImageView>(R.id.image)
        val text1: TextView = parentView.findViewById(R.id.text)
        val text2: TextView = parentView.findViewById(R.id.text2)
        val text3: TextView = parentView.findViewById(R.id.text3)
        val textSeparator1: TextView = parentView.findViewById(R.id.textSeparator1)
        text1.text = itemHolder.song
        text2.text = itemHolder.artist
        text3.text = itemHolder.album
        text3.visibility = View.VISIBLE
        textSeparator1.visibility = View.VISIBLE
        parentView.isSelected = itemHolder.selected()
        // FIXME: 2/21/21 Album art loading
        /**GlideApp.with(context)
            .load(itemHolder.albumHolder.art)
            .placeholder(R.drawable.ic_music_note_white_24dp)
            .override(160)
            .centerCrop()
            .into(image)**/
    }

    fun extractFolderName(folder: String): String {
        if (folder.contains(File.separator)) {
            val split = folder.split(File.separator.toRegex()).toTypedArray()
            if (split.size >= 2) return split[split.size - 2]
        }
        return folder
    }
}