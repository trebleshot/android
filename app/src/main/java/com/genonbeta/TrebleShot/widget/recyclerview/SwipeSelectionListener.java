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

package com.genonbeta.TrebleShot.widget.recyclerview;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import com.genonbeta.TrebleShot.app.EditableListFragmentBase;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder;

/**
 * created by: veli
 * date: 3/11/19 1:02 AM
 */
public class SwipeSelectionListener<T extends Editable> implements OnItemTouchListener
{
    public static final String TAG = SwipeSelectionListener.class.getSimpleName();

    private boolean mSelectionActivated, mActivationWaiting;
    private int mLastPosition, mStartPosition;
    private int mInitialX, mInitialY;
    private EditableListFragmentBase<T> mListFragment;

    public SwipeSelectionListener(EditableListFragmentBase<T> fragment)
    {
        mListFragment = fragment;
        setInitials();
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e)
    {
        if (MotionEvent.ACTION_DOWN == e.getAction()) {
            mActivationWaiting = mListFragment.getPerformerEngine() != null;
            mInitialX = (int) e.getX();
            mInitialY = (int) e.getY();
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mActivationWaiting
                && (mInitialX != (int) (e.getX()) || mInitialY != (int) (e.getY()))) {
            mSelectionActivated = e.getEventTime() - e.getDownTime() > ViewConfiguration.getLongPressTimeout();
            mActivationWaiting = false;
        }

        return mSelectionActivated;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e)
    {
        if (MotionEvent.ACTION_UP == e.getAction() || MotionEvent.ACTION_CANCEL == e.getAction()) {
            setInitials();
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mSelectionActivated) {
            int currentPos = RecyclerView.NO_POSITION;
            View view = mListFragment.getListView().findChildViewUnder(e.getX(), e.getY());

            if (view != null) {
                ViewHolder holder = (ViewHolder) mListFragment.getListView()
                        .findContainingViewHolder(view);

                if (holder != null) {
                    currentPos = holder.getAdapterPosition();

                    if (currentPos >= 0) {
                        if (mStartPosition < 0) {
                            mStartPosition = currentPos;
                            mLastPosition = currentPos;
                        }

                        if (currentPos != mLastPosition) {
                            synchronized (mListFragment.getAdapterImpl().getList()) {
                                // The idea is that we start with some arbitrary position to select, so, for instance,
                                // when the starting position is 8 and user goes to select 7, 6, 5, we declare these
                                // as selected, however, when the user goes with 6, 7, 8 after 5, we decide that those
                                // numbers are now unselected. This goes on until the user releases the touch event.
                                int startPos = Math.min(currentPos, mLastPosition);
                                int endPos = Math.max(currentPos, mLastPosition);

                                for (int i = startPos; i < endPos + 1; i++) {
                                    boolean selected = currentPos > mLastPosition ? mStartPosition <= i
                                            : mStartPosition >= i;

                                    boolean selectionResult = mListFragment.getEngineConnection().setSelected(
                                            mListFragment.getAdapterImpl().getItem(i), selected);

                                    // TODO: 28.03.2020 Remove this unneeded code
                                    ViewHolder viewHolder = (ViewHolder) rv.findViewHolderForAdapterPosition(i);

                                    if (viewHolder != null && selectionResult)
                                        viewHolder.setSelected(selected);
                                }
                            }

                            mLastPosition = currentPos;
                        }
                    }
                }
            }

            if (mStartPosition < 0 && currentPos < 0)
                mSelectionActivated = false;

            {
                int scrollY = 0;
                int scrollX = 0;

                {
                    float viewHeight = rv.getHeight();
                    float viewPinPoint = viewHeight / 3;
                    float touchPointBelowStart = viewHeight - viewPinPoint;

                    if (touchPointBelowStart < e.getY()) {
                        scrollY = (int) (30 * ((Math.min(e.getY(), viewHeight) - touchPointBelowStart) / viewPinPoint));
                    } else if (viewPinPoint > e.getY()) {
                        scrollY = (int) (-30 * ((viewPinPoint - Math.max(e.getY(), 0f)) / viewPinPoint));
                    }
                }

                {
                    float viewWidth = rv.getWidth();
                    float viewPinPoint = viewWidth / 3;
                    float touchPointBelowStart = viewWidth - viewPinPoint;

                    if (viewWidth - viewPinPoint < e.getX()) {
                        scrollX = (int) (30 * ((Math.min(e.getX(), viewWidth) - touchPointBelowStart) / viewPinPoint));
                    } else if (viewPinPoint > e.getX()) {
                        scrollX = (int) (-30 / ((viewPinPoint - Math.max(e.getX(), 0f)) / viewPinPoint));
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
                rv.scrollBy(scrollX, scrollY);
            }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept)
    {
        if (disallowIntercept)
            setInitials();
    }

    public void setInitials()
    {
        mSelectionActivated = mActivationWaiting = false;
        mStartPosition = mLastPosition = -1;
        mInitialX = mInitialY = 0;
    }
}

