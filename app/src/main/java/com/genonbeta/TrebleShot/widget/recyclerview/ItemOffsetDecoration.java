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
 * This header was missing and I copied from TransferGroupListFragment and accidentally the dates were nearly identical
 * This header was missing and I copied from TransferListFragment and accidentally the dates were nearly identical
 */

public class ItemOffsetDecoration extends RecyclerView.ItemDecoration
{
    private final int mPadding;
    private final boolean mEdgeSpace;
    private final boolean mHorizontalView;
    private final Rect mTmpRect = new Rect();

    public ItemOffsetDecoration(int padding, boolean edgeSpace, boolean horizontalView)
    {
        mPadding = padding > 1 ? padding / 2 : padding;
        mEdgeSpace = edgeSpace;
        mHorizontalView = horizontalView;
    }

    @Override
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, @NonNull RecyclerView parent,
                               @NonNull RecyclerView.State state)
    {
        if (parent.getAdapter() == null)
            return;

        int size = parent.getAdapter().getItemCount();
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

        mTmpRect.set(outRect);
        mTmpRect.left = mEdgeSpace || spanIndex != 0 ? mPadding : 0;
        mTmpRect.right = mEdgeSpace || spanIndex != spanCount ? mPadding : 0;
        mTmpRect.top = mEdgeSpace || position != 0 ? mPadding : 0;
        mTmpRect.bottom = mEdgeSpace || position + 1 != size ? mPadding : 0;

        outRect.left = mHorizontalView ? mTmpRect.top : mTmpRect.left;
        outRect.right = mHorizontalView ? mTmpRect.bottom : mTmpRect.right;
        outRect.top = mHorizontalView ? mTmpRect.left : mTmpRect.top;
        outRect.bottom = mHorizontalView ? mTmpRect.right : mTmpRect.bottom;
    }

    public void prepare(RecyclerView parent)
    {
        if (mEdgeSpace) {
            parent.setPadding(mPadding, mPadding, mPadding, mPadding);
            parent.setClipToPadding(false);
        }
    }
}
