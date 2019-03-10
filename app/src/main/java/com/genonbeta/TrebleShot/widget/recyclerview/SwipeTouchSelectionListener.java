package com.genonbeta.TrebleShot.widget.recyclerview;

import android.view.MotionEvent;
import android.view.View;

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
public class SwipeTouchSelectionListener<T extends Editable> implements RecyclerView.OnItemTouchListener
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
            mActivationWaiting = true;
            mConsistentX = (int) e.getX();
            mConsistentY = (int) e.getY();
        } else if (MotionEvent.ACTION_MOVE == e.getAction() && mActivationWaiting
                && (mConsistentX != (int) e.getX() || mConsistentY != (int) e.getY())) {
            mSelectionActivated = e.getEventTime() - e.getDownTime() > 200;
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

                                        mListFragment.getSelectionConnection().setSelected(
                                                mListFragment.getAdapterImpl().getItem(i), selected);

                                        EditableListAdapter.ViewHolder viewHolder
                                                = (EditableListAdapter.ViewHolder)
                                                rv.findViewHolderForAdapterPosition(i);

                                        if (viewHolder != null)
                                            viewHolder.getView().setSelected(selected);
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

            int scrollY = 0;
            int scrollX = 0;

            {
                float viewHeight = rv.getHeight();
                float viewPinPoint = viewHeight / 4;

                if (viewHeight - viewPinPoint < e.getY()) {
                    scrollY = (int) (30 * (e.getY() / viewHeight));
                } else if (viewPinPoint > e.getY()) {
                    scrollY = (int) (-30 / Math.max(e.getY() / viewPinPoint, 1f));
                }
            }

            {
                float viewWidth = rv.getWidth();
                float viewPinPoint = viewWidth / 4;

                if (viewWidth - viewPinPoint < e.getX()) {
                    scrollX = (int) (30 * (e.getX() / viewWidth));
                } else if (viewPinPoint > e.getX()) {
                    scrollX = (int) (-30 / Math.max(e.getX() / viewPinPoint, 1f));
                }
            }

            rv.scrollBy(scrollX, scrollY);
        }
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept)
    {
        mSelectionActivated = false;
        mActivationWaiting = false;
        mStartPosition = -1;
        mLastPosition = -1;
    }
}

