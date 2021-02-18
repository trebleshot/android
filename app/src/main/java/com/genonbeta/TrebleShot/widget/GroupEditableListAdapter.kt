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
package com.genonbeta.TrebleShot.widget

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.genonbeta.TrebleShot.R
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.dataobject.Shareable
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupEditable
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter.GroupViewHolder
import com.genonbeta.TrebleShot.widgetimport.EditableListAdapterBase
import com.genonbeta.android.framework.util.date.DateMerger
import com.genonbeta.android.framework.util.listing.ComparableMerger
import com.genonbeta.android.framework.util.listing.Lister
import com.genonbeta.android.framework.util.listing.Merger
import com.genonbeta.android.framework.util.listing.merger.StringMerger
import java.util.*

/**
 * created by: Veli
 * date: 29.03.2018 08:00
 */
abstract class GroupEditableListAdapter<T : GroupEditable, V : GroupViewHolder>(
    fragment: IEditableListFragment<T, V>, open var groupBy: Int,
) : EditableListAdapter<T, V>(fragment) {
    protected abstract fun onLoad(lister: GroupLister<T>)

    protected abstract fun onGenerateRepresentative(text: String, merger: Merger<T>?): T

    override fun onLoad(): MutableList<T> {
        val loadedList: MutableList<T> = ArrayList()
        val groupLister = createLister(loadedList, groupBy)
        onLoad(groupLister)
        if (groupLister.mergers.isNotEmpty()) {
            groupLister.mergers.sortWith { o1: ComparableMerger<T>, o2: ComparableMerger<T> -> o2.compareTo(o1) }

            for (thisMerger in groupLister.mergers) {
                Collections.sort(thisMerger.belongings, this)
                val generated: T = onGenerateRepresentative(getRepresentativeText(thisMerger), thisMerger)
                val firstEditable: T = thisMerger.belongings[0]
                loadedList.add(generated)
                generated.size = thisMerger.belongings.size.toLong()
                generated.date = firstEditable.getComparableDate()
                generated.id = generated.getRepresentativeText().hashCode().inv().toLong()
                loadedList.addAll(thisMerger.belongings)
            }
        } else Collections.sort(loadedList, this)
        return loadedList
    }

    open fun createLister(loadedList: MutableList<T>, groupBy: Int): GroupLister<T> {
        return GroupLister(loadedList, groupBy)
    }

    protected fun createDefaultViews(parent: ViewGroup?, viewType: Int, noPadding: Boolean): GroupViewHolder {
        if (viewType == VIEW_TYPE_REPRESENTATIVE) return GroupViewHolder(
            layoutInflater.inflate(if (noPadding) R.layout.layout_list_title_no_padding else R.layout.layout_list_title,
                parent, false), R.id.layout_list_title_text
        ) else if (viewType == VIEW_TYPE_ACTION_BUTTON) return GroupViewHolder(
            layoutInflater.inflate(R.layout.layout_list_action_button, parent, false), R.id.text
        )
        throw IllegalArgumentException("$viewType is not defined in defaults")
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).getViewType()
    }

    open fun getRepresentativeText(merger: Merger<out T>): String {
        if (merger is DateMerger<*>)
            return getSectionNameDate(merger.time)
        else if (merger is StringMerger<*>)
            return merger.text
        return merger.toString()
    }

    override fun getSectionName(position: Int, item: T): String {
        return when {
            item.isGroupRepresentative() -> item.getRepresentativeText()
            groupBy == MODE_GROUP_BY_DATE -> getSectionNameDate(item.getComparableDate())
            else -> super.getSectionName(position, item)
        }
    }

    interface GroupEditable : Editable {
        var requestCode: Int

        var date: Long

        var size: Long

        fun getViewType(): Int

        fun getRepresentativeText(): String

        fun setRepresentativeText(text: CharSequence)

        fun isGroupRepresentative(): Boolean
    }

    abstract class GroupShareable : Shareable, GroupEditable {
        private var viewType: Int = VIEW_TYPE_DEFAULT

        override var requestCode: Int = 0

        override var date: Long = 0

        override var size: Long = 0

        constructor() : super()

        constructor(viewType: Int, representativeText: String) {
            this.viewType = viewType
            friendlyName = representativeText
        }

        override fun getViewType(): Int {
            return viewType
        }

        override fun getRepresentativeText(): String {
            return friendlyName
        }

        override fun setRepresentativeText(text: CharSequence) {
            friendlyName = text.toString()
        }

        override fun isGroupRepresentative(): Boolean {
            return viewType == VIEW_TYPE_REPRESENTATIVE || viewType == VIEW_TYPE_ACTION_BUTTON
        }

        override fun setSelectableSelected(selected: Boolean): Boolean {
            return !isGroupRepresentative() && super.setSelectableSelected(selected)
        }
    }

    class GroupViewHolder : ViewHolder {
        private var representativeTextView: TextView? = null

        var requestCode = 0

        constructor(itemView: View, textView: TextView) : super(itemView) {
            representativeTextView = textView
        }

        constructor(itemView: View, resRepresentativeText: Int) : this(
            itemView, itemView.findViewById<TextView>(resRepresentativeText)
        )

        constructor(itemView: View) : super(itemView)

        fun getRepresentativeTextView(): TextView? {
            return representativeTextView
        }

        fun isRepresentative(): Boolean {
            return representativeTextView != null
        }

        fun tryBinding(editable: GroupEditable): Boolean {
            if (getRepresentativeTextView() == null) return false
            getRepresentativeTextView()?.text = editable.getRepresentativeText()
            requestCode = editable.requestCode
            return true
        }
    }

    class GroupLister<T : GroupEditable>(
        private val noGroupingList: MutableList<T>, private val mode: Int,
        var customLister: CustomGroupLister<T>? = null,
    ) : Lister<T, ComparableMerger<T>>() {
        fun offerObliged(adapter: EditableListAdapterBase<T>, item: T) {
            if (adapter.filterItem(item)) offer(item)
        }

        fun offer(item: T) {
            val customLister = customLister

            if (customLister == null || !customLister.onCustomGroupListing(this, mode, item)) {
                if (mode == MODE_GROUP_BY_DATE) {
                    offer(item, DateMerger(item.getComparableDate()))
                } else noGroupingList.add(item)
            }
        }

        interface CustomGroupLister<T : GroupEditable> {
            fun onCustomGroupListing(lister: GroupLister<T>, mode: Int, holder: T): Boolean
        }
    }

    companion object {
        const val VIEW_TYPE_REPRESENTATIVE = 100

        const val VIEW_TYPE_ACTION_BUTTON = 110

        const val MODE_GROUP_BY_NOTHING = 100

        const val MODE_GROUP_BY_DATE = 110
    }
}