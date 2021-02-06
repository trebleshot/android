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

import androidx.recyclerview.widget.RecyclerView
import com.genonbeta.TrebleShot.app.EditableListFragmentBase
import com.genonbeta.TrebleShot.dataobject.Editable
import com.genonbeta.TrebleShot.widget.recyclerview.SwipeSelectionListener
import com.genonbeta.android.framework.widget.RecyclerViewAdapter

/**
 * created by: veli
 * date: 3/11/19 1:02 AM
 */
class SwipeSelectionListener<T : Editable?>(private val mListFragment: EditableListFragmentBase<T>) :
    OnItemTouchListener {
    private var mSelectionActivated = false
    private var mActivationWaiting = false
    private var mLastPosition = 0
    private var mStartPosition = 0
    private var mInitialX = 0
    private var mInitialY = 0
    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (MotionEvent.ACTION_DOWN == e.getAction()) {
            mActivationWaiting = mListFragment.performerEngine != null
            mInitialX = e.getX()
            mInitialY = e.getY()
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mActivationWaiting
            && (mInitialX != e.getX() as Int || mInitialY != e.getY() as Int)
        ) {
            mSelectionActivated = e.getEventTime() - e.getDownTime() > ViewConfiguration.getLongPressTimeout()
            mActivationWaiting = false
        }
        return mSelectionActivated
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (MotionEvent.ACTION_UP == e.getAction() || MotionEvent.ACTION_CANCEL == e.getAction()) {
            setInitials()
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mSelectionActivated) {
            var currentPos = RecyclerView.NO_POSITION
            val view = mListFragment.listView.findChildViewUnder(e.getX(), e.getY())
            if (view != null) {
                val holder = mListFragment.listView
                    .findContainingViewHolder(view) as RecyclerViewAdapter.ViewHolder?
                if (holder != null) {
                    currentPos = holder.adapterPosition
                    if (currentPos >= 0) {
                        if (mStartPosition < 0) {
                            mStartPosition = currentPos
                            mLastPosition = currentPos
                        }
                        if (currentPos != mLastPosition) {
                            synchronized(mListFragment.adapterImpl.list) {

                                // The idea is that we start with some arbitrary position to select, so, for instance,
                                // when the starting position is 8 and user goes to select 7, 6, 5, we declare these
                                // as selected, however, when the user goes with 6, 7, 8 after 5, we decide that those
                                // numbers are now unselected. This goes on until the user releases the touch event.
                                val startPos = Math.min(currentPos, mLastPosition)
                                val endPos = Math.max(currentPos, mLastPosition)
                                for (i in startPos until endPos + 1) {
                                    val selected =
                                        if (currentPos > mLastPosition) mStartPosition <= i else mStartPosition >= i
                                    mListFragment.engineConnection.setSelected(
                                        mListFragment.adapterImpl.getItem(i), selected
                                    )
                                }
                            }
                            mLastPosition = currentPos
                        }
                    }
                }
            }
            if (mStartPosition < 0 && currentPos < 0) mSelectionActivated = false
            run {
                var scrollY = 0
                var scrollX = 0
                {
                    val viewHeight = rv.height.toFloat()
                    val viewPinPoint = viewHeight / 3
                    val touchPointBelowStart = viewHeight - viewPinPoint
                    if (touchPointBelowStart < e.getY()) {
                        scrollY =
                            (30 * ((Math.min(e.getY(), viewHeight) - touchPointBelowStart) / viewPinPoint)).toInt()
                    } else if (viewPinPoint > e.getY()) {
                        scrollY = (-30 * ((viewPinPoint - Math.max(e.getY(), 0f)) / viewPinPoint)).toInt()
                    }
                }
                {
                    val viewWidth = rv.width.toFloat()
                    val viewPinPoint = viewWidth / 3
                    val touchPointBelowStart = viewWidth - viewPinPoint
                    if (viewWidth - viewPinPoint < e.getX()) {
                        scrollX = (30 * ((Math.min(e.getX(), viewWidth) - touchPointBelowStart) / viewPinPoint)).toInt()
                    } else if (viewPinPoint > e.getX()) {
                        scrollX = (-30 / ((viewPinPoint - Math.max(e.getX(), 0f)) / viewPinPoint)).toInt()
                    }
                }

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
                rv.scrollBy(scrollX, scrollY)
            }
        }
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        if (disallowIntercept) setInitials()
    }

    fun setInitials() {
        mActivationWaiting = false
        mSelectionActivated = mActivationWaiting
        mLastPosition = -1
        mStartPosition = mLastPosition
        mInitialY = 0
        mInitialX = mInitialY
    }

    companion object {
        val TAG = SwipeSelectionListener::class.java.simpleName
    }

    init {
        setInitials()
    }
}