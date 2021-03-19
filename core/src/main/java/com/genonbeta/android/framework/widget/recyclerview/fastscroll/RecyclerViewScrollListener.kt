/*
 * Copyright (C) 2021 Veli Tasalı
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

package com.genonbeta.android.framework.widget.recyclerview.fastscroll

import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.widget.recyclerview.FastScroller
import java.util.*

/**
 * Created by Michal on 04/08/16.
 * Responsible for updating the handle / bubble position when user scrolls the [RecyclerView].
 */
class RecyclerViewScrollListener(private val scroller: FastScroller) : RecyclerView.OnScrollListener() {
    private val listeners: MutableList<ScrollerListener> = ArrayList()
    private var oldScrollState: Int = RecyclerView.SCROLL_STATE_IDLE

    fun addScrollerListener(listener: ScrollerListener) {
        listeners.add(listener)
    }

    override fun onScrollStateChanged(recyclerView: RecyclerView, newScrollState: Int) {
        super.onScrollStateChanged(recyclerView, newScrollState)
        if (newScrollState == RecyclerView.SCROLL_STATE_IDLE && oldScrollState != RecyclerView.SCROLL_STATE_IDLE)
            scroller.viewProvider?.onScrollFinished()
        else if (newScrollState != RecyclerView.SCROLL_STATE_IDLE && oldScrollState == RecyclerView.SCROLL_STATE_IDLE)
            scroller.viewProvider?.onScrollStarted()
        oldScrollState = newScrollState
    }

    override fun onScrolled(rv: RecyclerView, dx: Int, dy: Int) {
        if (scroller.shouldUpdateHandlePosition())
            updateHandlePosition(rv)
    }

    private fun updateHandlePosition(rv: RecyclerView) {
        val offset: Int
        val extent: Int
        val range: Int

        if (scroller.horizontalLayout) {
            offset = rv.computeHorizontalScrollOffset()
            extent = rv.computeHorizontalScrollExtent()
            range = rv.computeHorizontalScrollRange()
        } else {
            offset = rv.computeVerticalScrollOffset()
            extent = rv.computeVerticalScrollExtent()
            range = rv.computeVerticalScrollRange()
        }

        //float relativePos = offset / (float) (range - extent);
        // Subtracting extent introduces opposite direction numbers
        // even though the direction of scrolling is the same.
        // To overcome this, while preserving the highest and lowest possible
        // location for the bubble, we slowly add the extent number.

        // Another attempt to sync the positions for scrolling the view versus the handle
        //float computedExtent = (float) extent * (offset / (float) (range - extent));
        //float relativePos = (offset + computedExtent) / (float) range;
        val relativePos = if (offset <= extent)
            if (offset <= 0) 0f
            else (offset.toFloat() + extent.toFloat() * (offset / (range - extent).toFloat())) / range
        else (offset + extent) / range.toFloat()
        scroller.setScrollerPosition(relativePos)
        notifyListeners(relativePos)
    }

    private fun notifyListeners(relativePos: Float) {
        for (listener in listeners)
            listener.onScroll(relativePos)
    }

    interface ScrollerListener {
        fun onScroll(relativePos: Float)
    }
}