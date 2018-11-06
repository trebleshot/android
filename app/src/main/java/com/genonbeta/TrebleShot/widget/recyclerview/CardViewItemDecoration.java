package com.genonbeta.TrebleShot.widget.recyclerview;

import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

// preserves items with equal distance
public class CardViewItemDecoration extends RecyclerView.ItemDecoration
{
    private final int mPadding;
    private final int mGridSize;

    public CardViewItemDecoration(int padding)
    {
        this(padding, -1);
    }

    public CardViewItemDecoration(int padding, int gridSize)
    {
        mPadding = padding;
        mGridSize = gridSize;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
    {
        final int itemPosition = parent.getChildAdapterPosition(view);

        if (itemPosition == RecyclerView.NO_POSITION)
            return;

        final RecyclerView.ViewHolder viewHolder = parent.getChildViewHolder(view);

        if (mGridSize == -1) {
            outRect.left = mPadding;
            outRect.right = mPadding;
        } else {
            if ((double) (itemPosition % mGridSize) == 0.0) {
                outRect.left = mPadding;
            }

            outRect.right = mPadding;
        }

        outRect.top = itemPosition == 0 ? mPadding : 0;
        outRect.bottom = mPadding;
    }
}