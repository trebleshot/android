package com.genonbeta.TrebleShot.widget.recyclerview;

import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.exception.NotReadyException;
import com.genonbeta.TrebleShot.object.Editable;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;

/**
 * created by: veli
 * date: 3/11/19 1:02 AM
 */
public class SwipeTouchSelectionListener<T extends Editable>
        implements RecyclerView.OnItemTouchListener
{
    private boolean mSelectionActivated = false;
    private boolean mActivationWaiting = true;
    private int mLastPosition = -1;
    private int mStartPosition = -1;
    private int mConsistentX = 0;
    private int mConsistentY = 0;
    private EditableListFragmentImpl<T> mListFragment;

    public SwipeTouchSelectionListener(EditableListFragmentImpl<T> fragment)
    {
        mListFragment = fragment;
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e)
    {
        if (MotionEvent.ACTION_DOWN == e.getAction()) {
            mActivationWaiting = mListFragment.getSelectionConnection() != null;
            mConsistentX = (int) (e.getX() / 10);
            mConsistentY = (int) (e.getY() / 10);
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mActivationWaiting
                && (mConsistentX != (int) (e.getX() / 10) || mConsistentY != (int) (e.getY() / 10))) {
            mSelectionActivated = e.getEventTime() - e.getDownTime() > ViewConfiguration.getLongPressTimeout();
            mActivationWaiting = false;
        }

        return mSelectionActivated;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e)
    {
        if (MotionEvent.ACTION_UP == e.getAction()) {
            rv.requestDisallowInterceptTouchEvent(true);
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mSelectionActivated) {
            boolean childFound = false;
            View view = mListFragment.getListView().findChildViewUnder(e.getX(), e.getY());

            if (view != null) {
                EditableListAdapter.EditableViewHolder holder
                        = (EditableListAdapter.EditableViewHolder) mListFragment.getListView()
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
                                        boolean selected = currentPos > mLastPosition
                                                ? mStartPosition <= i
                                                : mStartPosition >= i;

                                        boolean selectionResult = mListFragment
                                                .getSelectionConnection().setSelected(mListFragment
                                                        .getAdapterImpl().getItem(i), selected);

                                        EditableListAdapter.ViewHolder viewHolder
                                                = (EditableListAdapter.ViewHolder)
                                                rv.findViewHolderForAdapterPosition(i);

                                        if (viewHolder != null)
                                            viewHolder.getView().setSelected(selectionResult && selected);
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

                rv.scrollBy(scrollX, scrollY);
            }
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept)
    {
        mSelectionActivated = false;
        mActivationWaiting = false;
        mStartPosition = -1;
        mLastPosition = -1;
        mConsistentX = 0;
    }
}

