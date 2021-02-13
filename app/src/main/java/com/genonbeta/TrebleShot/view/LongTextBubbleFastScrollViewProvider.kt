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

import android.graphics.drawable.InsetDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.genonbeta.TrebleShot.R
import com.genonbeta.android.framework.R.dimen
import com.genonbeta.android.framework.R.drawable
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.Utils
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.DefaultBubbleBehavior
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.ScrollViewProvider
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.ViewBehavior
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.VisibilityAnimationManager

/**
 * created by: veli
 * date: 10.04.2018 19:52
 */
class LongTextBubbleFastScrollViewProvider : ScrollViewProvider() {
    override lateinit var bubbleTextView: TextView

    override lateinit var bubbleView: View

    override lateinit var handleView: View

    override fun getBubbleOffset(): Int = if (scroller.horizontalLayout) {
        (handleView.width.toFloat() / 2f - bubbleView.width.toFloat() / 2).toInt()
    } else {
        (handleView.height.toFloat() / 2f - bubbleView.height.toFloat() / 2f).toInt()
    }

    override fun recreateViews(container: ViewGroup?) {
        bubbleTextView = LayoutInflater.from(context).inflate(
            R.layout.abstract_layout_fast_scroll_long_text_bubble_text_view, container, false
        ) as TextView
        bubbleView = bubbleTextView
        handleView = View(context).also {
            val inset = context.resources.getDimensionPixelSize(dimen.genfw_fastscroll_handle_inset)
            val verticalInset = if (scroller.horizontalLayout) inset else 0
            val horizontalInset = if (scroller.horizontalLayout) 0 else inset
            val handleBackground = InsetDrawable(
                ContextCompat.getDrawable(
                    context,
                    drawable.genfw_fastscroll_default_handle
                ), horizontalInset, verticalInset, horizontalInset, verticalInset
            )

            Utils.setBackground(handleView, handleBackground)

            val handleWidth = context.resources.getDimensionPixelSize(
                if (scroller.horizontalLayout) {
                    dimen.genfw_fastscroll_handle_height
                } else {
                    dimen.genfw_fastscroll_handle_clickable_width
                }
            )
            val handleHeight = context.resources.getDimensionPixelSize(
                if (scroller.horizontalLayout) {
                    dimen.genfw_fastscroll_handle_clickable_width
                } else {
                    dimen.genfw_fastscroll_handle_height
                }
            )

            it.layoutParams = ViewGroup.LayoutParams(handleWidth, handleHeight)
        }

        bubbleBehavior = DefaultBubbleBehavior(VisibilityAnimationManager.Builder(bubbleView)
            .withPivotX(1f)
            .withPivotY(1f)
            .build())
    }
}