package com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.genonbeta.android.framework.widget.recyclerview.FastScroller

/**
 * Created by Michal on 05/08/16.
 * Provides [View]s and their behaviors for the handle and bubble of the fastscroller.
 */
abstract class ScrollerViewProvider {
    protected val context: Context
        get() = scroller.context

    // TODO: 2/7/21 Use the providers?
    var bubbleBehavior: ViewBehavior? = null
    private set

    var handleBehavior: ViewBehavior? = null
        private set

    abstract var scroller: FastScroller

    /**
     * To offset the position of the bubble relative to the handle. E.g. in [DefaultScrollerViewProvider]
     * the sharp corner of the bubble is aligned with the center of the handle.
     *
     * @return the position of the bubble in relation to the handle (according to the orientation).
     */
    abstract fun getBubbleOffset(): Int

    /**
     * Bubble view has to provide a [TextView] that will show the index title.
     *
     * @return A [TextView] that will hold the index title.
     */
    abstract fun provideBubbleTextView(): TextView?

    /**
     * @param container The container [FastScroller] for the view to inflate properly.
     * @return A view which will be by the [FastScroller] used as a bubble.
     */
    abstract fun provideBubbleView(container: ViewGroup?): View?

    protected abstract fun provideHandleBehavior(): ViewBehavior?

    protected abstract fun provideBubbleBehavior(): ViewBehavior?

    /**
     * @param container The container [FastScroller] for the view to inflate properly.
     * @return A view which will be by the [FastScroller] used as a handle.
     */
    abstract fun provideHandleView(container: ViewGroup?): View?

    fun onHandleGrabbed() {
        handleBehavior?.onHandleGrabbed()
        bubbleBehavior?.onHandleGrabbed()
    }

    fun onHandleReleased() {
        handleBehavior?.onHandleReleased()
        bubbleBehavior?.onHandleReleased()
    }

    fun onScrollStarted() {
        handleBehavior?.onScrollStarted()
        bubbleBehavior?.onScrollStarted()
    }

    fun onScrollFinished() {
        handleBehavior?.onScrollFinished()
        bubbleBehavior?.onScrollFinished()
    }
}