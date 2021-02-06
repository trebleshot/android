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

import android.view.View
import android.view.ViewGroup
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.database.Kuick
import com.genonbeta.TrebleShot.util.AppUtils
import com.genonbeta.TrebleShot.widget.EditableListAdapter
import com.genonbeta.android.framework.util.listing.Merger

/**
 * created by: Veli
 * date: 30.12.2017 13:25
 */
class TextStreamListAdapter(fragment: IEditableListFragment<TextStreamObject?, GroupViewHolder?>?) :
    GroupEditableListAdapter<TextStreamObject?, GroupViewHolder?>(
        fragment,
        GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE
    ) {
    protected override fun onLoad(lister: GroupLister<TextStreamObject>) {
        for (`object` in AppUtils.getKuick(getContext()).castQuery<Any, TextStreamObject>(
            SQLQuery.Select(Kuick.Companion.TABLE_CLIPBOARD), TextStreamObject::class.java
        )) lister.offerObliged(this, `object`)
    }

    protected override fun onGenerateRepresentative(text: String, merger: Merger<TextStreamObject>?): TextStreamObject {
        return TextStreamObject(text)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val holder: GroupViewHolder = if (viewType == EditableListAdapter.Companion.VIEW_TYPE_DEFAULT) GroupViewHolder(
            getInflater().inflate(
                R.layout.list_text_stream, parent, false
            )
        ) else createDefaultViews(parent, viewType, false)
        if (!holder.isRepresentative()) getFragment().registerLayoutViewClicks(holder)
        return holder
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        try {
            val `object`: TextStreamObject = getItem(position)
            if (!holder.tryBinding(`object`)) {
                val parentView: View = holder.itemView
                val text: String = `object`.text.replace("\n", " ").trim { it <= ' ' }
                val text1: TextView = parentView.findViewById<TextView>(R.id.text)
                val text2: TextView = parentView.findViewById<TextView>(R.id.text2)
                val text3: TextView = parentView.findViewById<TextView>(R.id.text3)
                parentView.isSelected = `object`.isSelectableSelected()
                text1.setText(text)
                text2.setText(
                    DateUtils.formatDateTime(
                        getContext(),
                        `object`.comparableDate,
                        DateUtils.FORMAT_SHOW_TIME
                    )
                )
                text3.setVisibility(if (getGroupBy() != GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE) View.VISIBLE else View.GONE)
                if (getGroupBy() != GroupEditableListAdapter.Companion.MODE_GROUP_BY_DATE) text3.setText(
                    getSectionNameDate(`object`.comparableDate)
                )
            }
        } catch (ignored: Exception) {
        }
    }
}