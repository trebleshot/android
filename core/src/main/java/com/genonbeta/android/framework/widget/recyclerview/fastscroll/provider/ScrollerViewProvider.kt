package com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider

import androidx.test.runner.AndroidJUnit4
import android.content.ContentResolver
import android.content.Context
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
import android.webkit.MimeTypeMap
import com.google.android.material.snackbar.Snackbar
import com.genonbeta.android.framework.util.actionperformer.PerformerCallback
import com.genonbeta.android.framework.util.actionperformer.PerformerListener
import android.view.MenuInflater
import android.view.View
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine
import com.genonbeta.android.framework.util.actionperformer.IBaseEngineConnection
import com.genonbeta.android.framework.``object`
import com.genonbeta.android.framework.widget.recyclerview.FastScroller

/**
 * Created by Michal on 05/08/16.
 * Provides [View]s and their behaviors for the handle and bubble of the fastscroller.
 */
abstract class ScrollerViewProvider {
    private var mScroller: FastScroller? = null
    private var mHandleBehavior: ViewBehavior? = null
    private var mBubbleBehavior: ViewBehavior? = null
    fun setFastScroller(scroller: FastScroller?) {
        mScroller = scroller
    }

    protected fun getContext(): Context? {
        return mScroller.getContext()
    }

    protected fun getScroller(): FastScroller? {
        return mScroller
    }

    /**
     * @param container The container [FastScroller] for the view to inflate properly.
     * @return A view which will be by the [FastScroller] used as a handle.
     */
    abstract fun provideHandleView(container: ViewGroup?): View?

    /**
     * @param container The container [FastScroller] for the view to inflate properly.
     * @return A view which will be by the [FastScroller] used as a bubble.
     */
    abstract fun provideBubbleView(container: ViewGroup?): View?

    /**
     * Bubble view has to provide a [TextView] that will show the index title.
     *
     * @return A [TextView] that will hold the index title.
     */
    abstract fun provideBubbleTextView(): TextView?

    /**
     * To offset the position of the bubble relative to the handle. E.g. in [DefaultScrollerViewProvider]
     * the sharp corner of the bubble is aligned with the center of the handle.
     *
     * @return the position of the bubble in relation to the handle (according to the orientation).
     */
    abstract fun getBubbleOffset(): Int
    protected abstract fun provideHandleBehavior(): ViewBehavior?
    protected abstract fun provideBubbleBehavior(): ViewBehavior?
    protected fun getHandleBehavior(): ViewBehavior? {
        if (mHandleBehavior == null) mHandleBehavior = provideHandleBehavior()
        return mHandleBehavior
    }

    protected fun getBubbleBehavior(): ViewBehavior? {
        if (mBubbleBehavior == null) mBubbleBehavior = provideBubbleBehavior()
        return mBubbleBehavior
    }

    fun onHandleGrabbed() {
        if (getHandleBehavior() != null) getHandleBehavior().onHandleGrabbed()
        if (getBubbleBehavior() != null) getBubbleBehavior().onHandleGrabbed()
    }

    fun onHandleReleased() {
        if (getHandleBehavior() != null) getHandleBehavior().onHandleReleased()
        if (getBubbleBehavior() != null) getBubbleBehavior().onHandleReleased()
    }

    fun onScrollStarted() {
        if (getHandleBehavior() != null) getHandleBehavior().onScrollStarted()
        if (getBubbleBehavior() != null) getBubbleBehavior().onScrollStarted()
    }

    fun onScrollFinished() {
        if (getHandleBehavior() != null) getHandleBehavior().onScrollFinished()
        if (getBubbleBehavior() != null) getBubbleBehavior().onScrollFinished()
    }
}