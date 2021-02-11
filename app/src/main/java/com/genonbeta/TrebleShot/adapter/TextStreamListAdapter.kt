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
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.dataobject.TextStreamObject
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.android.database.SQLQuery
import com.genonbeta.android.framework.util.listing.Merger

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
class TextStreamListAdapter(fragment: IEditableListFragment<TextStreamObject, GroupViewHolder>) :
    GroupEditableListAdapter<TextStreamObject, GroupViewHolder>(
        fragment,
        MODE_GROUP_BY_DATE
    ) {
    protected override fun onLoad(lister: GroupLister<TextStreamObject>) {
        for (item in AppUtils.getKuick(context).castQuery(
            SQLQuery.Select(Kuick.TABLE_CLIPBOARD), TextStreamObject::class.java
        )) lister.offerObliged(this, item)
    }

    override fun onGenerateRepresentative(text: String, merger: Merger<TextStreamObject>?): TextStreamObject {
        return TextStreamObject(text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == EditableListAdapter.VIEW_TYPE_DEFAULT) GroupViewHolder(
            layoutInflater.inflate(
                R.layout.list_text_stream, parent, false
            )
        ) else createDefaultViews(parent, viewType, false)
        if (!holder.isRepresentative()) fragment.registerLayoutViewClicks(holder)
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val item: TextStreamObject = getItem(position)
            if (!holder.tryBinding(item)) {
                val parentView: View = holder.itemView
                val text: String = item.text.replace("\n", " ").trim { it <= ' ' }
                val text1: TextView = parentView.findViewById(R.id.text)
                val text2: TextView = parentView.findViewById(R.id.text2)
                val text3: TextView = parentView.findViewById(R.id.text3)
                parentView.isSelected = item.isSelectableSelected()
                text1.text = text
                text2.text = DateUtils.formatDateTime(context, item.getComparableDate(), DateUtils.FORMAT_SHOW_TIME)
                text3.visibility = if (getGroupBy() != MODE_GROUP_BY_DATE) View.VISIBLE else View.GONE
                if (getGroupBy() != MODE_GROUP_BY_DATE) text3.text = getSectionNameDate(item.getComparableDate())
            }
        } catch (ignored: Exception) {
        }
    }
}