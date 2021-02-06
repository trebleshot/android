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
import android.widget.TextView
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.RecyclerViewScrollListener
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.SectionTitleProvider
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.Utils
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.ScrollerViewProvider

class FastScroller constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) :
    LinearLayout(context, attrs, defStyle) {
    private val mScrollListener: RecyclerViewScrollListener = RecyclerViewScrollListener(this)
    private var mRecyclerView: RecyclerView? = null
    private var mBubble: View? = null
    private var mHandle: View? = null
    private var mBubbleTextView: TextView? = null
    private var mBubbleOffset = 0
    private var mHandleColor = 0
    private var mBubbleColor = 0
    private var mBubbleTextAppearance = 0
    private var mScrollerOrientation = 0
    private var mMaxVisibility: Int
    private var mManuallyChangingPosition = false
    private var mViewProvider: ScrollerViewProvider? = null
    private var mTitleProvider: SectionTitleProvider? = null

    /**
     * Attach the [FastScroller] to [RecyclerView]. Should mHandle.setMinimumHeight((int) computedExtent);
     * be used after the adapter is set to the [RecyclerView]. If the adapter implements SectionTitleProvider,
     * the FastScroller will show a bubble with title.
     *
     * @param recyclerView A [RecyclerView] to attach the [FastScroller] to.
     */
    fun setRecyclerView(recyclerView: RecyclerView) {
        mRecyclerView = recyclerView
        if (recyclerView.adapter is SectionTitleProvider)
            mTitleProvider = recyclerView.adapter
        recyclerView.addOnScrollListener(mScrollListener)
        invalidateVisibility()
        recyclerView.setOnHierarchyChangeListener(object : OnHierarchyChangeListener {
            override fun onChildViewAdded(parent: View?, child: View?) {
                invalidateVisibility()
            }

            override fun onChildViewRemoved(parent: View?, child: View?) {
                invalidateVisibility()
            }
        })
    }

    /**
     * Set the orientation of the [FastScroller]. The orientation of the [FastScroller]
     * should generally match the orientation of connected  [RecyclerView] for good UX but it's not enforced.
     * Note: This method is overridden from [LinearLayout.setOrientation] but for [FastScroller]
     * it has a totally different meaning.
     *
     * @param orientation of the [FastScroller]. [.VERTICAL] or [.HORIZONTAL]
     */
    override fun setOrientation(orientation: Int) {
        mScrollerOrientation = orientation
        //switching orientation, because orientation in linear layout
        //is something different than orientation of fast scroller
        super.setOrientation(if (orientation == HORIZONTAL) VERTICAL else HORIZONTAL)
    }

    /**
     * Set the background color of the bubble.
     *
     * @param color Color in hex notation with alpha channel, e.g. 0xFFFFFFFF
     */
    fun setBubbleColor(color: Int) {
        mBubbleColor = color
        invalidate()
    }

    /**
     * Set the background color of the handle.
     *
     * @param color Color in hex notation with alpha channel, e.g. 0xFFFFFFFF
     */
    fun setHandleColor(color: Int) {
        mHandleColor = color
        invalidate()
    }

    /**
     * Sets the text appearance of the bubble.
     *
     * @param textAppearanceResourceId The id of the resource to be used as text appearance of the bubble.
     */
    fun setBubbleTextAppearance(textAppearanceResourceId: Int) {
        mBubbleTextAppearance = textAppearanceResourceId
        invalidate()
    }

    /**
     * Add a [RecyclerViewScrollListener.ScrollerListener]
     * to be notified of user scrolling
     *
     * @param listener
     */
    fun addScrollerListener(listener: RecyclerViewScrollListener.ScrollerListener) {
        mScrollListener.addScrollerListener(listener)
    }

    protected override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        initHandleMovement()
        mBubbleOffset = mViewProvider.getBubbleOffset()
        applyStyling() //TODO this doesn't belong here, even if it works
        if (!isInEditMode) {
            //sometimes recycler starts with a defined scroll (e.g. when coming from saved state)

            // Disabled since this was triggering another change
            // on layout every time it was scrolled (esp. manual handle change).
            //mScrollListener.updateHandlePosition(mRecyclerView);
        }
    }

    private fun applyStyling() {
        if (mBubbleColor != STYLE_NONE)
            setBackgroundTint(mBubbleTextView, mBubbleColor)

        if (mHandleColor != STYLE_NONE)
            setBackgroundTint(mHandle, mHandleColor)

        if (mBubbleTextAppearance != STYLE_NONE)
            TextViewCompat.setTextAppearance(mBubbleTextView, mBubbleTextAppearance)
    }

    private fun setBackgroundTint(view: View?, color: Int) {
        val background = DrawableCompat.wrap(view.getBackground())
        DrawableCompat.setTint(background.mutate(), color)
        Utils.setBackground(view, background)
    }

    private fun initHandleMovement() {
      mHandle.setOnTouchListener { v, event ->
          requestDisallowInterceptTouchEvent(true)
          if (event.getAction() == MotionEvent.ACTION_DOWN || event.getAction() == MotionEvent.ACTION_MOVE) {
              if (mTitleProvider != null && event.getAction() == MotionEvent.ACTION_DOWN) mViewProvider.onHandleGrabbed()
              mManuallyChangingPosition = true
              val relativePos = getRelativeTouchPosition(event)
              setScrollerPosition(relativePos)
              setRecyclerViewPosition(relativePos)
              return true
          } else if (event.getAction() == MotionEvent.ACTION_UP) {
              mManuallyChangingPosition = false
              mRecyclerView.scrollBy(0, 0)
              if (mTitleProvider != null) mViewProvider.onHandleReleased()
              return true
          }
          return false
      }
    }

    private fun getRelativeTouchPosition(event: MotionEvent?): Float {
        if (isVertical()) {
            val yInParent: Float = event.getRawY() - Utils.getViewRawY(mHandle)
            return yInParent / (getHeight() - mHandle.getHeight())
        }
        val xInParent: Float = event.getRawX() - Utils.getViewRawX(mHandle)
        return xInParent / (getWidth() - mHandle.getWidth())
    }

    override fun setVisibility(visibility: Int) {
        mMaxVisibility = visibility
        invalidateVisibility()
    }

    private fun invalidateVisibility() {
        if (mRecyclerView.getAdapter() == null || mRecyclerView.getAdapter()
                .getItemCount() == 0 || mRecyclerView.getChildAt(0) == null ||
            isRecyclerViewNotScrollable() || mMaxVisibility != View.VISIBLE
        ) {
            super.setVisibility(View.INVISIBLE)
        } else {
            super.setVisibility(View.VISIBLE)
        }
    }

    private fun isRecyclerViewNotScrollable(): Boolean {
        return if (isVertical()) mRecyclerView.getChildAt(0).getHeight() * mRecyclerView.getAdapter()
            .getItemCount() <= mRecyclerView.getHeight() else mRecyclerView.getChildAt(0)
            .getWidth() * mRecyclerView.getAdapter().getItemCount() <= mRecyclerView.getWidth()
    }

    private fun setRecyclerViewPosition(relativePos: Float) {
        if (mRecyclerView == null || mRecyclerView.getAdapter() == null) return
        val offset: Int
        val extent: Int
        val range: Int
        if (isVertical()) {
            offset = mRecyclerView.computeVerticalScrollOffset()
            extent = mRecyclerView.computeVerticalScrollExtent()
            range = mRecyclerView.computeVerticalScrollRange()
        } else {
            offset = mRecyclerView.computeHorizontalScrollOffset()
            extent = mRecyclerView.computeHorizontalScrollExtent()
            range = mRecyclerView.computeHorizontalScrollRange()
        }
        run {
            val viewUnder: View = if (isVertical()) mRecyclerView.findChildViewUnder(
                0f,
                mHandle.getY()
            ) else mRecyclerView.findChildViewUnder(mHandle.getX(), 0f)
            val viewHolder: RecyclerView.ViewHolder? =
                if (viewUnder == null) null else mRecyclerView.findContainingViewHolder(viewUnder)
            val itemCount: Int = mRecyclerView.getAdapter().getItemCount()
            var targetPos = if (viewHolder == null) -1 else viewHolder.getAdapterPosition()
            if (targetPos < 0 || targetPos >= itemCount) // This is an old fallback method.
            // The problem is it assumes each child has the same length.
            // Let's hope that the method above always locates what is under.
                targetPos = Utils.getValueInRange(
                    0f,
                    (itemCount - 1).toFloat(),
                    (relativePos * itemCount as Float) as Int.toFloat
                    ()
                ) as Int
            if (mTitleProvider != null && mBubbleTextView != null) mBubbleTextView.setText(
                mTitleProvider.getSectionTitle(
                    targetPos
                )
            )
        }

        //float computedExtent = (float) extent * (offset / (float) (range - extent));
        //float currentOffset = offset + computedExtent;
        //  For: Change-a
        val expectedPosition = range * relativePos

        // Change-a
        // We don't want to scroll the view when it still covers all the content that is requested.
        if (expectedPosition <= extent && offset == 0) return
        val difference = (expectedPosition - (offset + extent)) as Int
        if (isVertical()) mRecyclerView.scrollBy(0, difference) else mRecyclerView.scrollBy(difference, 0)
    }

    fun setScrollerPosition(relativePos: Float) {
        if (isVertical()) {
            mBubble.setY(
                Utils.getValueInRange(
                    0f, (
                            getHeight() - mBubble.getHeight()).toFloat(),
                    relativePos * (getHeight() - mHandle.getHeight()) + mBubbleOffset
                )
            )
            mHandle.setY(
                Utils.getValueInRange(
                    0f, (
                            getHeight() - mHandle.getHeight()).toFloat(),
                    relativePos * (getHeight() - mHandle.getHeight())
                )
            )
        } else {
            mBubble.setX(
                Utils.getValueInRange(
                    0f, (
                            getWidth() - mBubble.getWidth()).toFloat(),
                    relativePos * (getWidth() - mHandle.getWidth()) + mBubbleOffset
                )
            )
            mHandle.setX(
                Utils.getValueInRange(
                    0f, (
                            getWidth() - mHandle.getWidth()).toFloat(),
                    relativePos * (getWidth() - mHandle.getWidth())
                )
            )
        }
    }

    fun isVertical(): Boolean {
        return mScrollerOrientation == VERTICAL
    }

    fun shouldUpdateHandlePosition(): Boolean {
        return mHandle != null && !mManuallyChangingPosition && mRecyclerView.getChildCount() > 0
    }

    fun getViewProvider(): ScrollerViewProvider {
        return mViewProvider
    }

    /**
     * Enables custom layout for [FastScroller].
     *
     * @param viewProvider A [ScrollerViewProvider] for the [FastScroller] to use when building layout.
     */
    fun setViewProvider(viewProvider: ScrollerViewProvider?) {
        removeAllViews()
        mViewProvider = viewProvider
        viewProvider.setFastScroller(this)
        mBubble = viewProvider.provideBubbleView(this)
        mHandle = viewProvider.provideHandleView(this)
        mBubbleTextView = viewProvider.provideBubbleTextView()
        addView(mBubble)
        addView(mHandle)
    }

    companion object {
        private const val STYLE_NONE = -1
    }

    init {
        setClipChildren(false)
        val style: TypedArray? = context.obtainStyledAttributes(
            attrs, R.styleable.GenfwFastScroller,
            R.attr.genfw_fastScrollStyle, 0
        )
        try {
            mBubbleColor = style.getColor(R.styleable.GenfwFastScroller_genfw_fastScrollBubbleColor, STYLE_NONE)
            mHandleColor = style.getColor(R.styleable.GenfwFastScroller_genfw_fastScrollHandleColor, STYLE_NONE)
            mBubbleTextAppearance = style.getResourceId(
                R.styleable.GenfwFastScroller_genfw_fastScrollBubbleTextAppearance, STYLE_NONE
            )
        } finally {
            style.recycle()
        }
        mMaxVisibility = getVisibility()
        setViewProvider(DefaultScrollerViewProvider())
    }
}