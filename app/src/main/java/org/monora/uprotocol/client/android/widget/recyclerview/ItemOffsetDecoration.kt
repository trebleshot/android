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
package org.monora.uprotocol.client.android.widget.recyclerview

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * created by: Veli
 * date: 10.11.2018 00:15
 * This header was missing and I copied from TransferGroupListFragment and accidentally the dates were nearly identical
 * This header was missing and I copied from TransferListFragment and accidentally the dates were nearly identical
 */
class ItemOffsetDecoration(
    padding: Int, val edgeSpace: Boolean, val horizontalView: Boolean,
) : RecyclerView.ItemDecoration() {
    private val padding: Int = if (padding > 1) padding / 2 else padding

    private val tmpRect = Rect()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val adapter = parent.adapter ?: return
        val layoutManager = parent.layoutManager ?: return
        val size = adapter.itemCount
        val position = parent.getChildAdapterPosition(view)

        if (position < 0)
            return

        var spanIndex = 0
        var spanCount = 1

        if (layoutManager is GridLayoutManager) {
            val layoutSpanCount = layoutManager.spanCount
            spanIndex = layoutManager.spanSizeLookup.getSpanIndex(position, layoutSpanCount)
            spanCount = layoutSpanCount - layoutManager.spanSizeLookup.getSpanSize(position)
        }

        tmpRect.set(outRect)
        tmpRect.left = if (edgeSpace || spanIndex != 0) padding else 0
        tmpRect.right = if (edgeSpace || spanIndex != spanCount) padding else 0
        tmpRect.top = if (edgeSpace || position != 0) padding else 0
        tmpRect.bottom = if (edgeSpace || position + 1 != size) padding else 0
        outRect.left = if (edgeSpace) tmpRect.top else tmpRect.left
        outRect.right = if (edgeSpace) tmpRect.bottom else tmpRect.right
        outRect.top = if (edgeSpace) tmpRect.left else tmpRect.top
        outRect.bottom = if (edgeSpace) tmpRect.right else tmpRect.bottom
    }

    fun prepare(parent: RecyclerView) {
        if (edgeSpace) {
            parent.setPadding(padding, padding, padding, padding)
            parent.clipToPadding = false
        }
    }
}