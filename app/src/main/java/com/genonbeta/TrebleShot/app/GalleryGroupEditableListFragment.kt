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
package com.genonbeta.TrebleShot.app

import android.os.Bundle
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupShareable
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.TrebleShot.widgetimport.GalleryGroupEditableListAdapter

/**
 * created by: veli
 * date: 30.03.2018 18:15
 */
abstract class GalleryGroupEditableListFragment<T : GroupShareable, V : GroupViewHolder, E : GroupEditableListAdapter<T, V>> :
    GroupEditableListFragment<T, V, E>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defaultGroupingCriteria = GroupEditableListAdapter.MODE_GROUP_BY_DATE
    }

    override fun onGroupingOptions(options: MutableMap<String, Int>) {
        super.onGroupingOptions(options)
        options[getString(R.string.text_groupByNothing)] = GroupEditableListAdapter.MODE_GROUP_BY_NOTHING
        options[getString(R.string.text_groupByDate)] = GroupEditableListAdapter.MODE_GROUP_BY_DATE
        options[getString(R.string.text_groupByAlbum)] = GalleryGroupEditableListAdapter.MODE_GROUP_BY_ALBUM
    }

    override var isGridSupported: Boolean = true
}