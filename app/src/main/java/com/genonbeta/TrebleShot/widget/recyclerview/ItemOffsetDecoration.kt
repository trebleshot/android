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
package com.genonbeta.TrebleShot.widget.recyclerview

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
class ItemOffsetDecoration(padding: Int, edgeSpace: Boolean, horizontalView: Boolean) : ItemDecoration() {
    private val mPadding: Int
    private val mEdgeSpace: Boolean
    private val mHorizontalView: Boolean
    private val mTmpRect = Rect()
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        if (parent.adapter == null) return
        val size = parent.adapter!!.itemCount
        val position = parent.getChildAdapterPosition(view)
        if (position < 0) return
        var spanIndex = 0
        var spanCount = 1
        if (parent.layoutManager is GridLayoutManager) {
            val layoutManager = parent.layoutManager as GridLayoutManager?
            val layoutSpanCount = layoutManager!!.spanCount
            spanIndex = layoutManager.spanSizeLookup.getSpanIndex(position, layoutSpanCount)
            spanCount = layoutSpanCount - layoutManager.spanSizeLookup.getSpanSize(position)
        }
        mTmpRect.set(outRect)
        mTmpRect.left = if (mEdgeSpace || spanIndex != 0) mPadding else 0
        mTmpRect.right = if (mEdgeSpace || spanIndex != spanCount) mPadding else 0
        mTmpRect.top = if (mEdgeSpace || position != 0) mPadding else 0
        mTmpRect.bottom = if (mEdgeSpace || position + 1 != size) mPadding else 0
        outRect.left = if (mHorizontalView) mTmpRect.top else mTmpRect.left
        outRect.right = if (mHorizontalView) mTmpRect.bottom else mTmpRect.right
        outRect.top = if (mHorizontalView) mTmpRect.left else mTmpRect.top
        outRect.bottom = if (mHorizontalView) mTmpRect.right else mTmpRect.bottom
    }

    fun prepare(parent: RecyclerView) {
        if (mEdgeSpace) {
            parent.setPadding(mPadding, mPadding, mPadding, mPadding)
            parent.clipToPadding = false
        }
    }

    init {
        mPadding = if (padding > 1) padding / 2 else padding
        mEdgeSpace = edgeSpace
        mHorizontalView = horizontalView
    }
}