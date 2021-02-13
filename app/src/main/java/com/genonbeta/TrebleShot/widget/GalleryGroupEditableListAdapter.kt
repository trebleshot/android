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
package com.genonbeta.TrebleShot.widgetimport

import android.net.Uri
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.*
import com.genonbeta.TrebleShot.widgetimport.GalleryGroupEditableListAdapter.*
import com.genonbeta.android.framework.util.listing.merger.StringMerger

/**
 * created by: Veli
 * date: 30.03.2018 14:58
 */
abstract class GalleryGroupEditableListAdapter<T : GalleryGroupShareable, V : GroupViewHolder>(
    fragment: IEditableListFragment<T, V>,
    groupBy: Int
) : GroupEditableListAdapter<T, V>(fragment, groupBy), GroupLister.CustomGroupLister<T> {
    override fun onCustomGroupListing(lister: GroupLister<T>, mode: Int, holder: T): Boolean {
        if (mode == MODE_GROUP_BY_ALBUM) {
            lister.offer(holder, StringMerger(holder.albumName))
            return true
        }
        return false
    }

    override fun createLister(loadedList: MutableList<T>, groupBy: Int): GroupLister<T> {
        return super.createLister(loadedList, groupBy).also { it.customLister = this }
    }

    override fun getSectionName(position: Int, item: T): String {
        if (!item.isGroupRepresentative()) if (getGroupBy() == MODE_GROUP_BY_ALBUM) return item.albumName
        return super.getSectionName(position, item)
    }

    open class GalleryGroupShareable : GroupShareable {
        lateinit var albumName: String

        constructor(viewType: Int, representativeText: String) : super(viewType, representativeText)

        constructor(
            id: Long, friendlyName: String, fileName: String, albumName: String, mimeType: String,
            date: Long, size: Long, uri: Uri
        ) {
            initialize(id, friendlyName, fileName, mimeType, date, size, uri)
            this.albumName = albumName
        }
    }

    companion object {
        val MODE_GROUP_BY_ALBUM: Int = MODE_GROUP_BY_DATE + 1
    }
}