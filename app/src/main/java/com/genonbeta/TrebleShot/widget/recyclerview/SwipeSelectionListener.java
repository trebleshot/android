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

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.OnItemTouchListener;
import androidx.recyclerview.widget.RecyclerView.OnScrollListener;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.android.framework.widget.RecyclerViewAdapter.ViewHolder;

/**
 * created by: veli
 * date: 3/11/19 1:02 AM
 */
public class SwipeSelectionListener<T extends Editable> extends OnScrollListener implements OnItemTouchListener,
        Runnable
{
    public static final String TAG = SwipeSelectionListener.class.getSimpleName();

    private boolean mSelectionActivated, mActivationWaiting;
    private int mLastPosition, mStartPosition;
    private int mInitialX, mInitialY;
    private int mScrollingX, mScrollingY;

    private EditableListFragmentImpl<T> mListFragment;

    private long mScrollJobStartTime;

    public SwipeSelectionListener(EditableListFragmentImpl<T> fragment)
    {
        mListFragment = fragment;
        setInitials(false);
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

            if (mSelectionActivated)
                rv.addOnScrollListener(this);
        }

        return mSelectionActivated;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e)
    {
        if (MotionEvent.ACTION_UP == e.getAction()) {
            setInitials(true);
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mSelectionActivated) {
            boolean childFound = false;
            View view = mListFragment.getListView().findChildViewUnder(e.getX(), e.getY());

            if (view != null) {
                ViewHolder holder = (ViewHolder) mListFragment.getListView()
                        .findContainingViewHolder(view);

                if (holder != null) {
                    int currentPos = holder.getAdapterPosition();
                    childFound = currentPos >= 0;

                    if (childFound) {
                        if (mStartPosition < 0) {
                            mStartPosition = currentPos;
                            mLastPosition = currentPos;
                        }

                        if (currentPos != mLastPosition) {
                            synchronized (mListFragment.getAdapterImpl().getList()) {
                                int startPos = currentPos;
                                int endPos = mLastPosition;

                                if (currentPos > mLastPosition) {
                                    startPos = mLastPosition;
                                    endPos = currentPos;
                                }

                                try {
                                    for (int i = startPos; i < endPos + 1; i++) {
                                        boolean selected = currentPos > mLastPosition ? mStartPosition <= i
                                                : mStartPosition >= i;

                                        boolean selectionResult = mListFragment.getEngineConnection().setSelected(
                                                mListFragment.getAdapterImpl().getItem(i), selected);

                                        ViewHolder viewHolder = (ViewHolder) rv.findViewHolderForAdapterPosition(i);

                                        if (viewHolder != null && selectionResult)
                                            viewHolder.setSelected(selected);
                                    }
                                } catch (NotReadyException e1) {
                                    // do nothing
                                }
                            }

                            mLastPosition = currentPos;
                        }
                    }
                }
            }

            if (mStartPosition < 0 && !childFound)
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

                mScrollingX = scrollX;
                mScrollingY = scrollY;

            }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept)
    {
        if (disallowIntercept)
            setInitials(true);
    }

    @Override
    public void run()
    {
        FragmentActivity activity = mListFragment.getActivity();
        RecyclerView view = mListFragment.getListView();

        if (activity.isFinishing() || !view.isShown()) {
            Log.d(TAG, "scroll: Cancelled to scroll task either because the the activity was finishing or " +
                    "the RecyclerView was not visible.");
            return;
        }

        long frameLoss = System.nanoTime();
    }

    protected void scroll()
    {
        FragmentActivity activity = mListFragment.getActivity();

        if (activity.isFinishing() && (mScrollingX != 0 || mScrollingY != 0))
            activity.runOnUiThread(this);
    }

    public void setInitials(boolean removal)
    {
        mSelectionActivated = mActivationWaiting = false;
        mStartPosition = mLastPosition = -1;
        mInitialX = mInitialY = 0;
        mScrollingX = mScrollingY = 0;

        // FIXME: 2.03.2020
        // if (removal)
        //     mListFragment.getListView().removeOnScrollListener(this);
    }
}

