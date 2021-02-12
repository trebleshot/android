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
package com.genonbeta.TrebleShot.fragment

import android.content.*
import com.genonbeta.TrebleShot.R
import android.os.Bundle
import android.view.View

class AudioListFragment : GroupEditableListFragment<AudioItemHolder?, GroupViewHolder?, AudioListAdapter?>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setFilteringSupported(true)
        setDefaultGroupingCriteria(AudioListAdapter.MODE_GROUP_BY_ALBUM)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListAdapter(AudioListAdapter(this))
        setEmptyListImage(R.drawable.ic_library_music_white_24dp)
        setEmptyListText(getString(R.string.text_listEmptyMusic))
    }

    override fun onResume() {
        super.onResume()
        requireContext().getContentResolver().registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true, getDefaultContentObserver()
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().getContentResolver().unregisterContentObserver(getDefaultContentObserver())
    }

    override fun onGroupingOptions(options: MutableMap<String?, Int?>) {
        super.onGroupingOptions(options)
        options[getString(R.string.text_groupByNothing)] = GroupEditableListAdapter.MODE_GROUP_BY_NOTHING
        options[getString(R.string.text_groupByDate)] = GroupEditableListAdapter.MODE_GROUP_BY_DATE
        options[getString(R.string.text_groupByAlbum)] = AudioListAdapter.MODE_GROUP_BY_ALBUM
        options[getString(R.string.text_groupByArtist)] = AudioListAdapter.MODE_GROUP_BY_ARTIST
        options[getString(R.string.text_groupByFolder)] = AudioListAdapter.MODE_GROUP_BY_FOLDER
    }

    override fun onGridSpanSize(viewType: Int, currentSpanSize: Int): Int {
        return if (viewType == GroupEditableListAdapter.VIEW_TYPE_REPRESENTATIVE) currentSpanSize else super.onGridSpanSize(
            viewType,
            currentSpanSize
        )
    }

    override fun performDefaultLayoutClick(
        holder: GroupViewHolder,
        item: AudioItemHolder
    ): Boolean {
        return performLayoutClickOpen(holder, item)
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_music)
    }
}