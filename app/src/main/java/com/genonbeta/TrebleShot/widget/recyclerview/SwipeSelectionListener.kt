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
package com.genonbeta.TrebleShot.widget.recyclerview

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.android.framework.widget.RecyclerViewAdapter
import kotlin.math.max
import kotlin.math.min

/**
 * created by: veli
 * date: 3/11/19 1:02 AM
 */
class SwipeSelectionListener<T : Editable>(private val listFragment: EditableListFragmentBase<T>) :
    RecyclerView.OnItemTouchListener {
    private var selectionActivated = false
    private var activationWaiting = false
    private var lastPos = 0
    private var startPos = 0
    private var initialX = 0
    private var initialY = 0

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (MotionEvent.ACTION_DOWN == e.action) {
            activationWaiting = listFragment.getPerformerEngine() != null
            initialX = e.x.toInt()
            initialY = e.y.toInt()
        } else if (MotionEvent.ACTION_MOVE == e.action && activationWaiting
            && (initialX != e.x.toInt() || initialY != e.y.toInt())
        ) {
            selectionActivated = e.eventTime - e.downTime > ViewConfiguration.getLongPressTimeout()
            activationWaiting = false
        }
        return selectionActivated
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (MotionEvent.ACTION_UP == e.action || MotionEvent.ACTION_CANCEL == e.action) {
            initialize()
        } else if (MotionEvent.ACTION_MOVE == e.action && selectionActivated) {
            var currentPos = RecyclerView.NO_POSITION
            val view = listFragment.listView.findChildViewUnder(e.x, e.y)
            val adapter = listFragment.adapterImpl ?: return

            if (view != null) {
                val holder = listFragment.listView.findContainingViewHolder(view) as RecyclerViewAdapter.ViewHolder?

                if (holder != null) {
                    currentPos = holder.adapterPosition
                    if (currentPos >= 0) {
                        if (startPos < 0) {
                            startPos = currentPos
                            lastPos = currentPos
                        }

                        if (currentPos != lastPos) {
                            // The idea is that we start with some arbitrary position to select, so, for instance,
                            // when the starting position is 8 and user goes to select 7, 6, 5, we declare these
                            // as selected, however, when the user goes with 6, 7, 8 after 5, we decide that those
                            // numbers are now unselected. This goes on until the user releases the touch event.
                            val startPos = min(currentPos, lastPos)
                            val endPos = max(currentPos, lastPos)
                            for (i in startPos until endPos + 1) {
                                val selected = if (currentPos > lastPos) this.startPos <= i else this.startPos >= i
                                listFragment.engineConnection.setSelected(adapter.getItem(i), selected)
                            }
                            lastPos = currentPos
                        }
                    }
                }
            }

            if (startPos < 0 && currentPos < 0)
                selectionActivated = false

            // Sadly a previous attempt to make this scroll continuous failed due to the limitations of the
            // smoothScrollBy method of RecyclerView. The problem is that inside the touch events, calling it has
            // no effect. And also, using scrollBy with touch listener has not benefits as it doesn't invoke
            // onScrollStateChanged method which, if it did, could be used to repeat the scrolling process
            // when the state is SETTLING. If it went according to the plan, as long as the mSelectionActivated
            // is true, we could keep scrolling it. Another good solution could be to use SmoothScroller class
            // with the layout manager, however, it was expensive use to because, firstly, it didn't scroll by
            // pixels, but by pointing out the position of a child and secondly, even though it could work, it
            // wasn't the best solution out there, because the next problem would be to guess where the user is
            // pointing his or her hand.
            rv.scrollBy(calculateX(rv, e), calculateY(rv, e))
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) initialize()
    }

    private fun calculateX(rv: RecyclerView, e: MotionEvent): Int {
        val viewWidth = rv.width.toFloat()
        val viewPinPoint = viewWidth / 3
        val touchPointBelowStart = viewWidth - viewPinPoint
        return when {
            viewWidth - viewPinPoint < e.x -> (30 * (min(e.x, viewWidth) - touchPointBelowStart) / viewPinPoint).toInt()
            viewPinPoint > e.x -> (-30 / ((viewPinPoint - max(e.x, 0f)) / viewPinPoint)).toInt()
            else -> 0
        }
    }

    private fun calculateY(rv: RecyclerView, e: MotionEvent): Int {
        val viewHeight = rv.height.toFloat()
        val viewPinPoint = viewHeight / 3
        val touchPointBelowStart = viewHeight - viewPinPoint
        return when {
            touchPointBelowStart < e.y -> (30 * (min(e.y, viewHeight) - touchPointBelowStart) / viewPinPoint).toInt()
            viewPinPoint > e.y -> (-30 * (viewPinPoint - max(e.y, 0f)) / viewPinPoint).toInt()
            else -> 0
        }
    }

    private fun initialize() {
        activationWaiting = false
        selectionActivated = activationWaiting
        lastPos = -1
        startPos = lastPos
        initialY = 0
        initialX = initialY
    }

    companion object {
        val TAG = SwipeSelectionListener::class.java.simpleName
    }

    init {
        initialize()
    }
}