/*
 * Copyright (C) 2020 Veli Tasalı
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
package com.genonbeta.android.framework.widget.recyclerview

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.R
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.RecyclerViewScrollListener
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.SectionTitleProvider
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.Utils
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.DefaultScrollViewProvider
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.ScrollViewProvider

class FastScroller @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0,
) : LinearLayout(context, attrs, defStyle) {
    var bubbleColor = 0
        set(value) {
            field = value
            invalidate()
        }

    var bubbleOffset = 0

    var bubbleTextAppearance = 0
        set(value) {
            field = value
            invalidate()
        }

    var handleColor = 0
        set(value) {
            field = value
            invalidate()
        }

    var horizontalLayout = false

    var manuallyChangingPosition = false

    var maxVisibility: Int

    var recyclerView: RecyclerView? = null
        set(value) {
            field = value

            value?.let {
                val adapter = it.adapter

                if (adapter is SectionTitleProvider)
                    titleProvider = adapter

                it.addOnScrollListener(scrollListener)
                invalidateVisibility()
                it.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
                    override fun onChildViewAdded(parent: View?, child: View?) {
                        invalidateVisibility()
                    }

                    override fun onChildViewRemoved(parent: View?, child: View?) {
                        invalidateVisibility()
                    }
                })
            }
        }

    val scrollListener: RecyclerViewScrollListener = RecyclerViewScrollListener(this)

    var titleProvider: SectionTitleProvider? = null

    var viewProvider: ScrollViewProvider? = null
        set(value) {
            removeAllViews()
            field = value

            value?.let {
                it.scroller = this
                it.recreateViews(this)

                addView(it.bubbleView)
                addView(it.handleView)
            }
        }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)

        viewProvider?.let {
            bubbleOffset = it.getBubbleOffset()

            initHandleMovement(it)
            applyStyling(it)
        }
    }

    fun addScrollerListener(listener: RecyclerViewScrollListener.ScrollerListener) {
        scrollListener.addScrollerListener(listener)
    }

    private fun applyStyling(viewProvider: ScrollViewProvider) {
        if (bubbleColor != STYLE_NONE)
            setBackgroundTint(viewProvider.bubbleTextView, bubbleColor)

        if (handleColor != STYLE_NONE)
            setBackgroundTint(viewProvider.handleView, handleColor)

        if (bubbleTextAppearance != STYLE_NONE)
            TextViewCompat.setTextAppearance(viewProvider.bubbleTextView, bubbleTextAppearance)
    }

    private fun setBackgroundTint(view: View, color: Int) {
        val background = DrawableCompat.wrap(view.background)

        DrawableCompat.setTint(background.mutate(), color)
        Utils.setBackground(view, background)
    }

    private fun initHandleMovement(viewProvider: ScrollViewProvider) {
        viewProvider.handleView.setOnTouchListener { v, event ->
            requestDisallowInterceptTouchEvent(true)

            if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_MOVE) {
                if (titleProvider != null && event.action == MotionEvent.ACTION_DOWN)
                    viewProvider.onHandleGrabbed()

                manuallyChangingPosition = true

                getRelativeTouchPosition(event).also {
                    setScrollerPosition(it)
                    setRecyclerViewPosition(it)
                }
            } else if (event.action == MotionEvent.ACTION_UP) {
                manuallyChangingPosition = false

                recyclerView?.scrollBy(0, 0)
                if (titleProvider != null)
                    viewProvider.onHandleReleased()
            } else {
                // TODO: 2/8/21 I don't like having this here.
                v.performClick()
                return@setOnTouchListener false
            }

            return@setOnTouchListener true
        }
    }

    private fun getRelativeTouchPosition(event: MotionEvent): Float {
        viewProvider?.let {
            if (horizontalLayout) {
                val xInParent: Float = event.rawX - Utils.getViewRawX(it.handleView)
                return xInParent / (width - it.handleView.width)
            }

            val yInParent: Float = event.rawY - Utils.getViewRawY(it.handleView)
            return yInParent / (height - it.handleView.height)
        }

        return 0f
    }

    override fun setVisibility(visibility: Int) {
        maxVisibility = visibility
        invalidateVisibility()
    }

    private fun invalidateVisibility() = recyclerView?.let {
        if (it.adapter == null || it.adapter?.itemCount == 0 || it.getChildAt(0) == null
            || isRecyclerViewInvalid() || maxVisibility != View.VISIBLE
        ) {
            super.setVisibility(View.INVISIBLE)
        } else {
            super.setVisibility(View.VISIBLE)
        }
    }

    private fun isRecyclerViewInvalid(): Boolean = recyclerView?.let {
        return it.adapter?.let { adapter ->
            if (horizontalLayout)
                it.getChildAt(0).width * adapter.itemCount <= it.width
            else
                it.getChildAt(0).height * adapter.itemCount <= it.height
        } ?: true
    } ?: true

    private fun setRecyclerViewPosition(relativePos: Float) = recyclerView?.let {
        val provider = viewProvider
        val adapter = it.adapter

        if (adapter == null || provider == null)
            return Unit

        val offset: Int
        val extent: Int
        val range: Int

        if (horizontalLayout) {
            offset = it.computeHorizontalScrollOffset()
            extent = it.computeHorizontalScrollExtent()
            range = it.computeHorizontalScrollRange()
        } else {
            offset = it.computeVerticalScrollOffset()
            extent = it.computeVerticalScrollExtent()
            range = it.computeVerticalScrollRange()
        }

        val viewUnder: View? = if (horizontalLayout)
            it.findChildViewUnder(provider.handleView.x, 0f)
        else
            it.findChildViewUnder(0f, provider.handleView.y)

        val viewHolder: RecyclerView.ViewHolder? = if (viewUnder == null)
            null
        else
            it.findContainingViewHolder(viewUnder)

        val itemCount = adapter.itemCount
        var targetPos = viewHolder?.adapterPosition ?: -1

        // The problem is it assumes each child has the same length.
        // Let's hope that the method above always locates what is under.
        if (targetPos < 0 || targetPos >= itemCount)
            targetPos = Utils.getValueInRange(
                0f,
                (itemCount - 1).toFloat(),
                relativePos * itemCount.toFloat()
            ).toInt()

        titleProvider?.let {
            provider.bubbleTextView.text = it.getSectionTitle(targetPos)
        }

        val expectedPosition = range * relativePos

        if (expectedPosition <= extent && offset == 0)
            return Unit

        val difference = (expectedPosition - (offset + extent)).toInt()

        if (horizontalLayout)
            it.scrollBy(difference, 0)
        else
            it.scrollBy(0, difference)
    }

    fun setScrollerPosition(relativePos: Float) = viewProvider?.let {
        if (horizontalLayout) {
            it.bubbleView.x = Utils.getValueInRange(
                0f, (width - it.bubbleView.width).toFloat(),
                relativePos * (width - it.handleView.width) + bubbleOffset
            )
            it.handleView.x = Utils.getValueInRange(
                0f, (width - it.handleView.width).toFloat(),
                relativePos * (width - it.handleView.width)
            )
        } else {
            it.bubbleView.y = Utils.getValueInRange(
                0f, (height - it.bubbleView.height).toFloat(),
                relativePos * (height - it.handleView.height) + bubbleOffset
            )
            it.handleView.y = Utils.getValueInRange(
                0f, (height - it.handleView.height).toFloat(),
                relativePos * (height - it.handleView.height)
            )
        }
    }

    fun shouldUpdateHandlePosition(): Boolean {
        return viewProvider != null && !manuallyChangingPosition && recyclerView?.let { it.childCount > 0 } ?: false
    }

    companion object {
        private const val STYLE_NONE = -1
    }

    init {
        clipChildren = false

        val style = context.obtainStyledAttributes(
            attrs, R.styleable.GenfwFastScroller,
            R.attr.genfw_fastScrollStyle, 0
        )
        try {
            bubbleColor = style.getColor(R.styleable.GenfwFastScroller_genfw_fastScrollBubbleColor, STYLE_NONE)
            handleColor = style.getColor(R.styleable.GenfwFastScroller_genfw_fastScrollHandleColor, STYLE_NONE)
            bubbleTextAppearance = style.getResourceId(
                R.styleable.GenfwFastScroller_genfw_fastScrollBubbleTextAppearance, STYLE_NONE
            )
        } finally {
            style.recycle()
        }

        maxVisibility = visibility
        viewProvider = DefaultScrollViewProvider()
    }
}