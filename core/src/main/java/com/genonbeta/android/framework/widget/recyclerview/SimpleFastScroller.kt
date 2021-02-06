package com.genonbeta.android.framework.widget.recyclerview

import android.R
import android.animation.Animator
import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import kotlin.Throws
import com.genonbeta.android.framework.io.StreamInfo.FolderStateException
import android.provider.OpenableColumns
import com.genonbeta.android.framework.io.StreamInfo
import com.genonbeta.android.framework.io.LocalDocumentFile
import com.genonbeta.android.framework.io.StreamDocumentFile
import androidx.annotation.RequiresApi
import android.provider.DocumentsContract
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.StateListDrawable
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

class SimpleFastScroller(
    recyclerView: RecyclerView?, // Final values for the vertical scroll bar
    /* synthetic access */
    val mVerticalThumbDrawable: StateListDrawable?,
    verticalTrackDrawable: Drawable?, horizontalThumbDrawable: StateListDrawable?,
    horizontalTrackDrawable: Drawable?, defaultWidth: Int, scrollbarMinimumRange: Int,
    margin: Int
) : ItemDecoration(), OnItemTouchListener {
    /* synthetic access */ val mVerticalTrackDrawable: Drawable?
    /* synthetic access */ val mShowHideAnimator: ValueAnimator? = ValueAnimator.ofFloat(0f, 1f)
    private val mScrollbarMinimumRange: Int
    private val mMargin: Int
    private val mVerticalThumbWidth: Int
    private val mVerticalTrackWidth: Int

    // Final values for the horizontal scroll bar
    private val mHorizontalThumbDrawable: StateListDrawable?
    private val mHorizontalTrackDrawable: Drawable?
    private val mHorizontalThumbHeight: Int
    private val mHorizontalTrackHeight: Int
    private val mVerticalRange: IntArray? = IntArray(2)
    private val mHorizontalRange: IntArray? = IntArray(2)

    // Dynamic values for the vertical scroll bar
    private var mVerticalThumbHeight = 0
    private var mVerticalThumbCenterY = 0
    private var mVerticalDragY = 0f

    // Dynamic values for the horizontal scroll bar
    private var mHorizontalThumbWidth = 0
    private var mHorizontalThumbCenterX = 0
    private var mHorizontalDragX = 0f

    /* synthetic access */@AnimationState
    var mAnimationState = ANIMATION_STATE_OUT
    private val mHideRunnable: Runnable? = Runnable { hide(HIDE_DURATION_MS) }
    private var mRecyclerViewWidth = 0
    private var mRecyclerViewHeight = 0
    private var mRecyclerView: RecyclerView? = null

    /**
     * Whether the document is long/wide enough to require scrolling. If not, we don't show the
     * relevant scroller.
     */
    private var mNeedVerticalScrollbar = false
    private var mNeedHorizontalScrollbar = false

    @State
    private var mState = STATE_HIDDEN
    private val mOnScrollListener: RecyclerView.OnScrollListener? = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
            updateScrollPosition(
                recyclerView.computeHorizontalScrollOffset(),
                recyclerView.computeVerticalScrollOffset()
            )
        }
    }

    @DragState
    private var mDragState = DRAG_NONE
    fun attachToRecyclerView(recyclerView: RecyclerView?) {
        if (mRecyclerView === recyclerView) {
            return  // nothing to do
        }
        if (mRecyclerView != null) {
            destroyCallbacks()
        }
        mRecyclerView = recyclerView
        if (mRecyclerView != null) {
            setupCallbacks()
        }
    }

    private fun setupCallbacks() {
        mRecyclerView.addItemDecoration(this)
        mRecyclerView.addOnItemTouchListener(this)
        mRecyclerView.addOnScrollListener(mOnScrollListener)
    }

    private fun destroyCallbacks() {
        mRecyclerView.removeItemDecoration(this)
        mRecyclerView.removeOnItemTouchListener(this)
        mRecyclerView.removeOnScrollListener(mOnScrollListener)
        cancelHide()
    }

    fun  /* synthetic access */requestRedraw() {
        mRecyclerView.invalidate()
    }

    fun setState(@State state: Int) {
        if (state == STATE_DRAGGING && mState != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(PRESSED_STATE_SET)
            cancelHide()
        }
        if (state == STATE_HIDDEN) {
            requestRedraw()
        } else {
            show()
        }
        if (mState == STATE_DRAGGING && state != STATE_DRAGGING) {
            mVerticalThumbDrawable.setState(EMPTY_STATE_SET)
            resetHideDelay(HIDE_DELAY_AFTER_DRAGGING_MS)
        } else if (state == STATE_VISIBLE) {
            resetHideDelay(HIDE_DELAY_AFTER_VISIBLE_MS)
        }
        mState = state
    }

    private fun isLayoutRTL(): Boolean {
        return ViewCompat.getLayoutDirection(mRecyclerView) == ViewCompat.LAYOUT_DIRECTION_RTL
    }

    fun isDragging(): Boolean {
        return mState == STATE_DRAGGING
    }

    fun isVisible(): Boolean {
        return mState == STATE_VISIBLE
    }

    fun isHidden(): Boolean {
        return mState == STATE_HIDDEN
    }

    fun show() {
        when (mAnimationState) {
            ANIMATION_STATE_FADING_OUT -> {
                mShowHideAnimator.cancel()
                mAnimationState = ANIMATION_STATE_FADING_IN
                mShowHideAnimator.setFloatValues(mShowHideAnimator.getAnimatedValue() as Float, 1f)
                mShowHideAnimator.setDuration(SHOW_DURATION_MS.toLong())
                mShowHideAnimator.setStartDelay(0)
                mShowHideAnimator.start()
            }
            ANIMATION_STATE_OUT -> {
                mAnimationState = ANIMATION_STATE_FADING_IN
                mShowHideAnimator.setFloatValues(mShowHideAnimator.getAnimatedValue() as Float, 1f)
                mShowHideAnimator.setDuration(SHOW_DURATION_MS.toLong())
                mShowHideAnimator.setStartDelay(0)
                mShowHideAnimator.start()
            }
        }
    }

    @JvmOverloads
    fun hide(duration: Int = 0) {
        when (mAnimationState) {
            ANIMATION_STATE_FADING_IN -> {
                mShowHideAnimator.cancel()
                mAnimationState = ANIMATION_STATE_FADING_OUT
                mShowHideAnimator.setFloatValues(mShowHideAnimator.getAnimatedValue() as Float, 0f)
                mShowHideAnimator.setDuration(duration.toLong())
                mShowHideAnimator.start()
            }
            ANIMATION_STATE_IN -> {
                mAnimationState = ANIMATION_STATE_FADING_OUT
                mShowHideAnimator.setFloatValues(mShowHideAnimator.getAnimatedValue() as Float, 0f)
                mShowHideAnimator.setDuration(duration.toLong())
                mShowHideAnimator.start()
            }
        }
    }

    private fun cancelHide() {
        mRecyclerView.removeCallbacks(mHideRunnable)
    }

    private fun resetHideDelay(delay: Int) {
        cancelHide()
        mRecyclerView.postDelayed(mHideRunnable, delay.toLong())
    }

    override fun onDrawOver(canvas: Canvas?, parent: RecyclerView?, state: RecyclerView.State?) {
        if (mRecyclerViewWidth != mRecyclerView.getWidth()
            || mRecyclerViewHeight != mRecyclerView.getHeight()
        ) {
            mRecyclerViewWidth = mRecyclerView.getWidth()
            mRecyclerViewHeight = mRecyclerView.getHeight()
            // This is due to the different events ordering when keyboard is opened or
            // retracted vs rotate. Hence to avoid corner cases we just disable the
            // scroller when size changed, and wait until the scroll position is recomputed
            // before showing it back.
            setState(STATE_HIDDEN)
            return
        }
        if (mAnimationState != ANIMATION_STATE_OUT) {
            if (mNeedVerticalScrollbar) {
                drawVerticalScrollbar(canvas)
            }
            if (mNeedHorizontalScrollbar) {
                drawHorizontalScrollbar(canvas)
            }
        }
    }

    private fun drawVerticalScrollbar(canvas: Canvas?) {
        val viewWidth = mRecyclerViewWidth
        val left = viewWidth - mVerticalThumbWidth
        val top = mVerticalThumbCenterY - mVerticalThumbHeight / 2
        mVerticalThumbDrawable.setBounds(0, 0, mVerticalThumbWidth, mVerticalThumbHeight)
        mVerticalTrackDrawable
            .setBounds(0, 0, mVerticalTrackWidth, mRecyclerViewHeight)
        if (isLayoutRTL()) {
            mVerticalTrackDrawable.draw(canvas)
            canvas.translate(mVerticalThumbWidth.toFloat(), top.toFloat())
            canvas.scale(-1f, 1f)
            mVerticalThumbDrawable.draw(canvas)
            canvas.scale(1f, 1f)
            canvas.translate(-mVerticalThumbWidth.toFloat(), -top.toFloat())
        } else {
            canvas.translate(left.toFloat(), 0f)
            mVerticalTrackDrawable.draw(canvas)
            canvas.translate(0f, top.toFloat())
            mVerticalThumbDrawable.draw(canvas)
            canvas.translate(-left.toFloat(), -top.toFloat())
        }
    }

    private fun drawHorizontalScrollbar(canvas: Canvas?) {
        val viewHeight = mRecyclerViewHeight
        val top = viewHeight - mHorizontalThumbHeight
        val left = mHorizontalThumbCenterX - mHorizontalThumbWidth / 2
        mHorizontalThumbDrawable.setBounds(0, 0, mHorizontalThumbWidth, mHorizontalThumbHeight)
        mHorizontalTrackDrawable
            .setBounds(0, 0, mRecyclerViewWidth, mHorizontalTrackHeight)
        canvas.translate(0f, top.toFloat())
        mHorizontalTrackDrawable.draw(canvas)
        canvas.translate(left.toFloat(), 0f)
        mHorizontalThumbDrawable.draw(canvas)
        canvas.translate(-left.toFloat(), -top.toFloat())
    }

    /**
     * Notify the scroller of external change of the scroll, e.g. through dragging or flinging on
     * the view itself.
     *
     * @param offsetX The new scroll X offset.
     * @param offsetY The new scroll Y offset.
     */
    fun updateScrollPosition(offsetX: Int, offsetY: Int) {
        val verticalContentLength: Int = mRecyclerView.computeVerticalScrollRange()
        val verticalVisibleLength = mRecyclerViewHeight
        mNeedVerticalScrollbar = (verticalContentLength - verticalVisibleLength > 0
                && mRecyclerViewHeight >= mScrollbarMinimumRange)
        val horizontalContentLength: Int = mRecyclerView.computeHorizontalScrollRange()
        val horizontalVisibleLength = mRecyclerViewWidth
        mNeedHorizontalScrollbar = (horizontalContentLength - horizontalVisibleLength > 0
                && mRecyclerViewWidth >= mScrollbarMinimumRange)
        if (!mNeedVerticalScrollbar && !mNeedHorizontalScrollbar) {
            if (mState != STATE_HIDDEN) {
                setState(STATE_HIDDEN)
            }
            return
        }
        if (mNeedVerticalScrollbar) {
            val middleScreenPos = offsetY + verticalVisibleLength / 2.0f
            mVerticalThumbCenterY = (verticalVisibleLength * middleScreenPos / verticalContentLength) as Int
            mVerticalThumbHeight = Math.min(
                verticalVisibleLength,
                verticalVisibleLength * verticalVisibleLength / verticalContentLength
            )
        }
        if (mNeedHorizontalScrollbar) {
            val middleScreenPos = offsetX + horizontalVisibleLength / 2.0f
            mHorizontalThumbCenterX = (horizontalVisibleLength * middleScreenPos / horizontalContentLength) as Int
            mHorizontalThumbWidth = Math.min(
                horizontalVisibleLength,
                horizontalVisibleLength * horizontalVisibleLength / horizontalContentLength
            )
        }
        if (mState == STATE_HIDDEN || mState == STATE_VISIBLE) {
            setState(STATE_VISIBLE)
        }
    }

    override fun onInterceptTouchEvent(
        recyclerView: RecyclerView,
        ev: MotionEvent
    ): Boolean {
        val handled: Boolean
        if (mState == STATE_VISIBLE) {
            val insideVerticalThumb = isPointInsideVerticalThumb(ev.getX(), ev.getY())
            val insideHorizontalThumb = isPointInsideHorizontalThumb(ev.getX(), ev.getY())
            if (ev.getAction() == MotionEvent.ACTION_DOWN
                && (insideVerticalThumb || insideHorizontalThumb)
            ) {
                if (insideHorizontalThumb) {
                    mDragState = DRAG_X
                    mHorizontalDragX = ev.getX() as Int.toFloat()
                } else if (insideVerticalThumb) {
                    mDragState = DRAG_Y
                    mVerticalDragY = ev.getY() as Int.toFloat()
                }
                setState(STATE_DRAGGING)
                handled = true
            } else {
                handled = false
            }
        } else if (mState == STATE_DRAGGING) {
            handled = true
        } else {
            handled = false
        }
        return handled
    }

    override fun onTouchEvent(recyclerView: RecyclerView, me: MotionEvent) {
        if (mState == STATE_HIDDEN) {
            return
        }
        if (me.getAction() == MotionEvent.ACTION_DOWN) {
            val insideVerticalThumb = isPointInsideVerticalThumb(me.getX(), me.getY())
            val insideHorizontalThumb = isPointInsideHorizontalThumb(me.getX(), me.getY())
            if (insideVerticalThumb || insideHorizontalThumb) {
                if (insideHorizontalThumb) {
                    mDragState = DRAG_X
                    mHorizontalDragX = me.getX() as Int.toFloat()
                } else if (insideVerticalThumb) {
                    mDragState = DRAG_Y
                    mVerticalDragY = me.getY() as Int.toFloat()
                }
                setState(STATE_DRAGGING)
            }
        } else if (me.getAction() == MotionEvent.ACTION_UP && mState == STATE_DRAGGING) {
            mVerticalDragY = 0f
            mHorizontalDragX = 0f
            setState(STATE_VISIBLE)
            mDragState = DRAG_NONE
        } else if (me.getAction() == MotionEvent.ACTION_MOVE && mState == STATE_DRAGGING) {
            show()
            if (mDragState == DRAG_X) {
                horizontalScrollTo(me.getX())
            }
            if (mDragState == DRAG_Y) {
                verticalScrollTo(me.getY())
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {}
    private fun verticalScrollTo(y: Float) {
        var y = y
        val scrollbarRange = getVerticalRange()
        y = Math.max(scrollbarRange.get(0), Math.min(scrollbarRange.get(1), y))
        if (Math.abs(mVerticalThumbCenterY - y) < 2) {
            return
        }
        val scrollingBy = scrollTo(
            mVerticalDragY, y, scrollbarRange,
            mRecyclerView.computeVerticalScrollRange(),
            mRecyclerView.computeVerticalScrollOffset(), mRecyclerViewHeight
        )
        if (scrollingBy != 0) {
            mRecyclerView.scrollBy(0, scrollingBy)
        }
        mVerticalDragY = y
    }

    private fun horizontalScrollTo(x: Float) {
        var x = x
        val scrollbarRange = getHorizontalRange()
        x = Math.max(scrollbarRange.get(0), Math.min(scrollbarRange.get(1), x))
        if (Math.abs(mHorizontalThumbCenterX - x) < 2) {
            return
        }
        val scrollingBy = scrollTo(
            mHorizontalDragX, x, scrollbarRange,
            mRecyclerView.computeHorizontalScrollRange(),
            mRecyclerView.computeHorizontalScrollOffset(), mRecyclerViewWidth
        )
        if (scrollingBy != 0) {
            mRecyclerView.scrollBy(scrollingBy, 0)
        }
        mHorizontalDragX = x
    }

    private fun scrollTo(
        oldDragPos: Float, newDragPos: Float, scrollbarRange: IntArray?, scrollRange: Int,
        scrollOffset: Int, viewLength: Int
    ): Int {
        val scrollbarLength = scrollbarRange.get(1) - scrollbarRange.get(0)
        if (scrollbarLength == 0) {
            return 0
        }
        val percentage = (newDragPos - oldDragPos) / scrollbarLength as Float
        val totalPossibleOffset = scrollRange - viewLength
        val scrollingBy = (percentage * totalPossibleOffset) as Int
        val absoluteOffset = scrollOffset + scrollingBy
        return if (absoluteOffset < totalPossibleOffset && absoluteOffset >= 0) {
            scrollingBy
        } else {
            0
        }
    }

    fun isPointInsideVerticalThumb(x: Float, y: Float): Boolean {
        return ((if (isLayoutRTL()) x <= mVerticalThumbWidth / 2 else x >= mRecyclerViewWidth - mVerticalThumbWidth)
                && y >= mVerticalThumbCenterY - mVerticalThumbHeight / 2 && y <= mVerticalThumbCenterY + mVerticalThumbHeight / 2)
    }

    fun isPointInsideHorizontalThumb(x: Float, y: Float): Boolean {
        return (y >= mRecyclerViewHeight - mHorizontalThumbHeight
                && x >= mHorizontalThumbCenterX - mHorizontalThumbWidth / 2 && x <= mHorizontalThumbCenterX + mHorizontalThumbWidth / 2)
    }

    fun getHorizontalTrackDrawable(): Drawable? {
        return mHorizontalTrackDrawable
    }

    fun getHorizontalThumbDrawable(): Drawable? {
        return mHorizontalThumbDrawable
    }

    fun getVerticalTrackDrawable(): Drawable? {
        return mVerticalTrackDrawable
    }

    fun getVerticalThumbDrawable(): Drawable? {
        return mVerticalThumbDrawable
    }

    /**
     * Gets the (min, max) vertical positions of the vertical scroll bar.
     */
    private fun getVerticalRange(): IntArray? {
        mVerticalRange.get(0) = mMargin
        mVerticalRange.get(1) = mRecyclerViewHeight - mMargin
        return mVerticalRange
    }

    /**
     * Gets the (min, max) horizontal positions of the horizontal scroll bar.
     */
    private fun getHorizontalRange(): IntArray? {
        mHorizontalRange.get(0) = mMargin
        mHorizontalRange.get(1) = mRecyclerViewWidth - mMargin
        return mHorizontalRange
    }

    @IntDef(STATE_HIDDEN, STATE_VISIBLE, STATE_DRAGGING)
    @Retention(RetentionPolicy.SOURCE)
    private inner annotation class State

    @IntDef(DRAG_X, DRAG_Y, DRAG_NONE)
    @Retention(RetentionPolicy.SOURCE)
    private inner annotation class DragState

    @IntDef(ANIMATION_STATE_OUT, ANIMATION_STATE_FADING_IN, ANIMATION_STATE_IN, ANIMATION_STATE_FADING_OUT)
    @Retention(
        RetentionPolicy.SOURCE
    )
    private inner annotation class AnimationState
    private inner class AnimatorListener internal constructor() : AnimatorListenerAdapter() {
        private var mCanceled = false
        override fun onAnimationEnd(animation: Animator?) {
            // Cancel is always followed by a new directive, so don't update state.
            if (mCanceled) {
                mCanceled = false
                return
            }
            if (mShowHideAnimator.getAnimatedValue() as Float == 0f) {
                mAnimationState = ANIMATION_STATE_OUT
                setState(STATE_HIDDEN)
            } else {
                mAnimationState = ANIMATION_STATE_IN
                requestRedraw()
            }
        }

        override fun onAnimationCancel(animation: Animator?) {
            mCanceled = true
        }
    }

    private inner class AnimatorUpdater internal constructor() : AnimatorUpdateListener {
        override fun onAnimationUpdate(valueAnimator: ValueAnimator?) {
            val alpha = (SCROLLBAR_FULL_OPAQUE * valueAnimator.getAnimatedValue() as Float) as Int
            mVerticalThumbDrawable.setAlpha(alpha)
            mVerticalTrackDrawable.setAlpha(alpha)
            requestRedraw()
        }
    }

    companion object {
        // Scroll thumb not showing
        private const val STATE_HIDDEN = 0

        // Scroll thumb visible and moving along with the scrollbar
        private const val STATE_VISIBLE = 1

        // Scroll thumb being dragged by user
        private const val STATE_DRAGGING = 2
        private const val DRAG_NONE = 0
        private const val DRAG_X = 1
        private const val DRAG_Y = 2
        private const val ANIMATION_STATE_OUT = 0
        private const val ANIMATION_STATE_FADING_IN = 1
        private const val ANIMATION_STATE_IN = 2
        private const val ANIMATION_STATE_FADING_OUT = 3
        private const val SHOW_DURATION_MS = 500
        private const val HIDE_DELAY_AFTER_VISIBLE_MS = 1500
        private const val HIDE_DELAY_AFTER_DRAGGING_MS = 1200
        private const val HIDE_DURATION_MS = 500
        private const val SCROLLBAR_FULL_OPAQUE = 255
        private val PRESSED_STATE_SET: IntArray? = intArrayOf(R.attr.state_pressed)
        private val EMPTY_STATE_SET: IntArray? = intArrayOf()
    }

    init {
        mVerticalTrackDrawable = verticalTrackDrawable
        mHorizontalThumbDrawable = horizontalThumbDrawable
        mHorizontalTrackDrawable = horizontalTrackDrawable
        mVerticalThumbWidth = Math.max(defaultWidth, mVerticalThumbDrawable.getIntrinsicWidth())
        mVerticalTrackWidth = Math.max(defaultWidth, verticalTrackDrawable.getIntrinsicWidth())
        mHorizontalThumbHeight = Math
            .max(defaultWidth, horizontalThumbDrawable.getIntrinsicWidth())
        mHorizontalTrackHeight = Math
            .max(defaultWidth, horizontalTrackDrawable.getIntrinsicWidth())
        mScrollbarMinimumRange = scrollbarMinimumRange
        mMargin = margin
        mVerticalThumbDrawable.setAlpha(SCROLLBAR_FULL_OPAQUE)
        mVerticalTrackDrawable.setAlpha(SCROLLBAR_FULL_OPAQUE)
        mShowHideAnimator.addListener(AnimatorListener())
        mShowHideAnimator.addUpdateListener(AnimatorUpdater())
        attachToRecyclerView(recyclerView)
    }
}