package com.genonbeta.TrebleShot.widget.recyclerview;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * created by: Veli
 * date: 10.11.2018 00:15
 * This header was missing and I copied from TransferGroupListFragment and accidentally the dates were nearly identical
 */

public class PaddingItemDecoration extends RecyclerView.ItemDecoration
{
    private int mPadding;
    private boolean mActive = true;
    private boolean mEdgeSpace = true;

    public PaddingItemDecoration(int padding)
    {
        this(padding, true);
    }

    public PaddingItemDecoration(int padding, boolean edgeSpace)
    {
        setPadding(padding);
        setEdgeSpace(edgeSpace);
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
    {
        if (!mActive || parent.getAdapter() == null)
            return;

        int position = parent.getChildAdapterPosition(view);

        if (position < 0)
            return;

        int spanIndex = 0;
        int spanCount = 1;

        if (parent.getLayoutManager() instanceof GridLayoutManager) {
            GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
            int layoutSpanCount = layoutManager.getSpanCount();
            spanIndex = layoutManager.getSpanSizeLookup().getSpanIndex(position, layoutSpanCount);
            spanCount = layoutSpanCount - layoutManager.getSpanSizeLookup().getSpanSize(position);
        }

        if (mPadding >= 2) {
            int paddingRect = mPadding / 2;
            int paddingNormal = mPadding;

            if (mEdgeSpace) {
                outRect.left = spanIndex == 0 ? paddingNormal : paddingRect;
                outRect.right = spanIndex == spanCount ? paddingNormal : paddingRect;
                outRect.bottom = mPadding;

                if (position == 0)
                    outRect.top = mPadding;
            } else {
                outRect.left = spanIndex == 0 ? 0 : paddingRect;
                outRect.right = spanIndex == spanCount ? 0 : paddingRect;

                if (position > 0)
                    outRect.top = mPadding;
            }
        }
    }

    public void setActive(boolean active)
    {
        mActive = active;
    }

    public void setEdgeSpace(boolean edgeSpace)
    {
        mEdgeSpace = edgeSpace;
    }

    public void setPadding(int padding)
    {
        mPadding = padding;
    }
}