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

import android.content.Context
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.adapter.VideoListAdapter
import com.genonbeta.TrebleShot.adapter.VideoListAdapter.VideoHolder
import com.genonbeta.TrebleShot.app.GalleryGroupEditableListFragment
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder

class VideoListFragment : GalleryGroupEditableListFragment<VideoHolder, GroupViewHolder, VideoListAdapter>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isFilteringSupported = true
        defaultOrderingCriteria = EditableListAdapter.MODE_SORT_ORDER_DESCENDING
        defaultSortingCriteria = EditableListAdapter.MODE_SORT_BY_DATE
        defaultViewingGridSize = 3
        defaultViewingGridSizeLandscape = 5
        itemOffsetDecorationEnabled = true
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = VideoListAdapter(this)
        emptyListImageView.setImageResource(R.drawable.ic_video_library_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmptyVideo)
    }

    override fun onResume() {
        super.onResume()
        requireContext().contentResolver.registerContentObserver(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            true, defaultContentObserver
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(defaultContentObserver)
    }

    override fun onGridSpanSize(viewType: Int, currentSpanSize: Int): Int {
        return if (viewType == VideoListAdapter.VIEW_TYPE_TITLE) currentSpanSize else super.onGridSpanSize(
            viewType,
            currentSpanSize
        )
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_video)
    }

    override fun performDefaultLayoutClick(holder: GroupViewHolder, target: VideoHolder): Boolean {
        return performLayoutClickOpen(holder, target)
    }
}