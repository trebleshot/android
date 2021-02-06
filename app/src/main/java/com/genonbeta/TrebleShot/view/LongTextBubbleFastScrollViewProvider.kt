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
package com.genonbeta.TrebleShot.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.genonbeta.TrebleShot.R
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.Utils

/**
 * created by: veli
 * date: 10.04.2018 19:52
 */
class LongTextBubbleFastScrollViewProvider : ScrollerViewProvider() {
    private var mBubble: View? = null
    private var mHandle: View? = null
    override fun provideHandleView(container: ViewGroup): View {
        mHandle = View(getContext())
        val verticalInset = if (getScroller().isVertical()) 0 else getContext().getResources()
            .getDimensionPixelSize(com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_inset)
        val horizontalInset = if (!getScroller().isVertical()) 0 else getContext().getResources()
            .getDimensionPixelSize(com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_inset)
        val handleBg = InsetDrawable(
            ContextCompat.getDrawable(
                getContext(),
                com.genonbeta.android.framework.R.drawable.genfw_fastscroll_default_handle
            ), horizontalInset, verticalInset, horizontalInset, verticalInset
        )
        Utils.setBackground(mHandle, handleBg)
        val handleWidth: Int = getContext().getResources()
            .getDimensionPixelSize(if (getScroller().isVertical()) com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_clickable_width else com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_height)
        val handleHeight: Int = getContext().getResources()
            .getDimensionPixelSize(if (getScroller().isVertical()) com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_height else com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_clickable_width)
        val params = ViewGroup.LayoutParams(handleWidth, handleHeight)
        mHandle!!.layoutParams = params
        return mHandle
    }

    override fun provideBubbleView(container: ViewGroup): View {
        mBubble = LayoutInflater.from(getContext())
            .inflate(R.layout.abstract_layout_fast_scroll_long_text_bubble_text_view, container, false)
        return mBubble
    }

    override fun provideBubbleTextView(): TextView {
        return mBubble as TextView
    }

    val bubbleOffset: Int
        get() = (if (getScroller().isVertical()) mHandle!!.height.toFloat() / 2f - mBubble!!.height
            .toFloat() / 2f else mHandle!!.width.toFloat() / 2f - mBubble!!.width.toFloat() / 2).toInt()

    protected override fun provideHandleBehavior(): ViewBehavior? {
        return null
    }

    protected override fun provideBubbleBehavior(): ViewBehavior? {
        return DefaultBubbleBehavior(VisibilityAnimationManager.Builder(mBubble).withPivotX(1f).withPivotY(1f).build())
    }
}