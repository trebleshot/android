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

import android.text.format.DateUtils
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.app.IEditableListFragment
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.TrebleShot.widgetimport.EditableListAdapterBase
import com.genonbeta.android.framework.util.Files
import com.genonbeta.android.framework.util.MathUtils
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.SectionTitleProvider
import org.monora.uprotocol.client.android.model.ContentModel
import java.text.Collator
import java.util.*

/**
 * created by: Veli
 * date: 12.01.2018 16:55
 */
abstract class EditableListAdapter<T : ContentModel, V : RecyclerViewAdapter.ViewHolder>(
    var fragment: IEditableListFragment<T, V>,
) : RecyclerViewAdapter<T, V>(fragment.requireContext()), EditableListAdapterBase<T>, SectionTitleProvider {
    private var collator: Collator? = null

    private val itemList: MutableList<T> = ArrayList()

    var sortingCriteria = MODE_SORT_BY_NAME

    var sortingOrder = MODE_SORT_ORDER_ASCENDING

    private var gridLayoutRequested = false

    override fun onUpdate(passedItem: MutableList<T>) {
        synchronized(itemList) {
            itemList.clear()
            itemList.addAll(passedItem)
            syncSelectionList()
        }
    }

    override fun compare(compare: T, compareTo: T): Int {
        val sortingAscending = getSortingOrder(compare, compareTo) == MODE_SORT_ORDER_ASCENDING
        return compareItems(
            getSortingCriteria(compare, compareTo),
            if (sortingAscending) compare else compareTo,
            if (sortingAscending) compareTo else compare
        )
    }

    fun compareItems(sortingCriteria: Int, obj1: T, obj2: T): Int {
        when (sortingCriteria) {
            MODE_SORT_BY_DATE -> return MathUtils.compare(obj1.dateModified(), obj2.dateModified())
            MODE_SORT_BY_SIZE -> return MathUtils.compare(obj1.length(), obj2.length())
            MODE_SORT_BY_NAME -> return getDefaultCollator().compare(obj1.name(), obj2.name())
        }
        throw IllegalStateException("Asked for $sortingCriteria which isn't known.")
    }

    fun isGridLayoutRequested(): Boolean {
        return gridLayoutRequested
    }

    fun getDefaultCollator(): Collator = collator ?: Collator.getInstance().also {
        it.strength = Collator.TERTIARY
        collator = it
    }

    override fun getItemCount(): Int {
        return getList().size
    }

    override fun getItem(position: Int): T {
        synchronized(itemList) { return itemList[position] }
    }

    fun getItem(holder: V): T {
        val position = holder.adapterPosition
        check(position != RecyclerView.NO_POSITION)
        return getItem(position)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).id()
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_DEFAULT
    }

    override fun getList(): MutableList<T> {
        return itemList
    }

    override fun getAvailableList(): MutableList<T> {
        return getList()
    }

    open fun getSectionName(position: Int, item: T): String {
        when (sortingCriteria) {
            MODE_SORT_BY_NAME -> return getSectionNameTrimmedText(item.name())
            MODE_SORT_BY_DATE -> return getSectionNameDate(item.dateModified())
            MODE_SORT_BY_SIZE -> return Files.formatLength(item.length(), false)
        }
        return position.toString()
    }

    fun getSectionNameDate(date: Long): String {
        return DateUtils.formatDateTime(context, date, DateUtils.FORMAT_SHOW_DATE).toString()
    }

    fun getSectionNameTrimmedText(text: String): String {
        return TextUtils.trimText(text, 1).toUpperCase(Locale.getDefault())
    }

    override fun getSectionTitle(position: Int): String {
        return getSectionName(position, getItem(position))
    }

    open fun getSortingCriteria(objectOne: T, objectTwo: T): Int {
        return sortingCriteria
    }

    open fun getSortingOrder(objectOne: T, objectTwo: T): Int {
        return sortingCriteria
    }

    fun notifyGridSizeUpdate(gridSize: Int, isScreenLarge: Boolean) {
        gridLayoutRequested = !isScreenLarge && gridSize > 1 || gridSize > 2
    }

    fun setSortingCriteria(sortingCriteria: Int, sortingOrder: Int) {
        this.sortingCriteria = sortingCriteria
        this.sortingOrder = sortingOrder
    }

    override fun syncAndNotify(adapterPosition: Int) {
        syncSelection(adapterPosition)
        notifyItemChanged(adapterPosition)
    }

    override fun syncAllAndNotify() {
        syncSelectionList()
        notifyDataSetChanged()
    }

    @Synchronized
    fun syncSelection(adapterPosition: Int) {
        val item = getItem(adapterPosition)
        item.select(fragment.engineConnection.isSelectedOnHost(item))
    }

    @Synchronized
    fun syncSelectionList() {
        val itemList: List<T> = ArrayList(getList())
        for (item in itemList) item.select(fragment.engineConnection.isSelectedOnHost(item))
    }

    companion object {
        const val VIEW_TYPE_DEFAULT = 0
        const val MODE_SORT_BY_NAME = 100
        const val MODE_SORT_BY_DATE = 110
        const val MODE_SORT_BY_SIZE = 120
        const val MODE_SORT_ORDER_ASCENDING = 100
        const val MODE_SORT_ORDER_DESCENDING = 110
    }

    init {
        setHasStableIds(true)
    }
}