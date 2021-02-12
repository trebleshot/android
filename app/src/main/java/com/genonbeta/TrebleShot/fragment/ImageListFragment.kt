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
import com.genonbeta.TrebleShot.adapter.ImageListAdapter
import com.genonbeta.TrebleShot.adapter.ImageListAdapter.ImageHolder
import com.genonbeta.TrebleShot.app.GalleryGroupEditableListFragment
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder

/**
 * Created by gabm on 11/01/18.
 */
class ImageListFragment : GalleryGroupEditableListFragment<ImageHolder, GroupViewHolder, ImageListAdapter>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isFilteringSupported = true
        defaultOrderingCriteria = EditableListAdapter.MODE_SORT_ORDER_DESCENDING
        defaultSortingCriteria = EditableListAdapter.MODE_SORT_BY_DATE
        itemOffsetDecorationEnabled = true

        setDefaultViewingGridSize(3, 5)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ImageListAdapter(this)
        emptyListImageView.setImageResource(R.drawable.ic_photo_white_24dp)
        emptyListTextView.text = getString(R.string.text_listEmptyImage)
    }

    override fun onResume() {
        super.onResume()
        requireContext().contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, defaultContentObserver
        )
    }

    override fun onPause() {
        super.onPause()
        requireContext().contentResolver.unregisterContentObserver(defaultContentObserver)
    }

    override fun getDistinctiveTitle(context: Context): CharSequence {
        return context.getString(R.string.text_photo)
    }

    override fun performDefaultLayoutClick(holder: GroupViewHolder, target: ImageHolder): Boolean {
        return performLayoutClickOpen(holder, target)
    }
}