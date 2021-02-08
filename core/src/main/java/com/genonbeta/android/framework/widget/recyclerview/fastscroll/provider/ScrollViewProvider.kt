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
abstract class ScrollViewProvider {
    protected val context: Context
        get() = scroller.context

    open var bubbleBehavior: ViewBehavior? = null

    abstract var bubbleTextView: TextView

    abstract var bubbleView: View

    open var handleBehavior: ViewBehavior? = null

    abstract var handleView: View

    abstract fun getBubbleOffset(): Int

    abstract fun recreateViews(container: ViewGroup?)

    open lateinit var scroller: FastScroller

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