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