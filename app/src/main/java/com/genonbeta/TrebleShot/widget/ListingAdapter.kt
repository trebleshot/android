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
import com.genonbeta.TrebleShot.app.IListingFragment
import com.genonbeta.TrebleShot.util.TextUtils
import com.genonbeta.TrebleShot.widgetimport.ListingAdapterBase
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
abstract class ListingAdapter<T : ContentModel, V : RecyclerViewAdapter.ViewHolder>(
    var fragment: IListingFragment<T, V>,
) : RecyclerViewAdapter<T, V>(fragment.requireContext()), ListingAdapterBase<T>, SectionTitleProvider {
    private var collator: Collator? = null

    private val itemList: MutableList<T> = ArrayList()

    override fun onUpdate(passedItem: MutableList<T>) {
        synchronized(itemList) {
            itemList.clear()
            itemList.addAll(passedItem)
            syncSelectionList()
        }
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

    override fun getSectionTitle(position: Int): String {
        val name = getItem(position).name()
        return if (name.length > 1) name.substring(0, 1) else name
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
    }

    init {
        setHasStableIds(true)
    }
}