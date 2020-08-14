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

import android.graphics.Rect;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

/**
 * created by: Veli
 * date: 10.11.2018 00:15
 * This header was missing and I copied from TransferListFragment and accidentally the dates were nearly identical
 */

public class PaddingItemDecoration extends RecyclerView.ItemDecoration
{
    private int mPadding;
    private boolean mActive = true;
    private boolean mEdgeSpace = true;
    private boolean mHorizontalView = false;

    public PaddingItemDecoration(int padding)
    {
        this(padding, true, false);
    }

    public PaddingItemDecoration(int padding, boolean edgeSpace, boolean horizontalView)
    {
        setPadding(padding);
        setHorizontalView(horizontalView);
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

        Rect thisRect = new Rect();

        if (mPadding >= 2) {
            int paddingRect = mPadding / 2;
            int paddingNormal = mPadding;

            if (mEdgeSpace) {
                thisRect.left = spanIndex == 0 ? paddingNormal : paddingRect;
                thisRect.right = spanIndex == spanCount ? paddingNormal : paddingRect;
                thisRect.bottom = mPadding;

                if (position <= spanCount && spanIndex == position)
                    thisRect.top = mPadding;
            } else {
                thisRect.left = spanIndex == 0 ? 0 : paddingRect;
                thisRect.right = spanIndex == spanCount ? 0 : paddingRect;

                if (position > spanCount || spanIndex != position)
                    thisRect.top = mPadding;
            }
        }

        outRect.left = mHorizontalView ? thisRect.top : thisRect.left;
        outRect.right = mHorizontalView ? thisRect.bottom : thisRect.right;
        outRect.top = mHorizontalView ? thisRect.left : thisRect.top;
        outRect.bottom = mHorizontalView ? thisRect.right : thisRect.bottom;
    }

    public void setActive(boolean active)
    {
        mActive = active;
    }

    public void setEdgeSpace(boolean edgeSpace)
    {
        mEdgeSpace = edgeSpace;
    }

    public void setHorizontalView(boolean isHorizontal)
    {
        mHorizontalView = isHorizontal;
    }

    public void setPadding(int padding)
    {
        mPadding = padding;
    }
}