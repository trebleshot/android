package com.genonbeta.TrebleShot.view;

import android.graphics.drawable.InsetDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.Utils;
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.DefaultBubbleBehavior;
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.ScrollerViewProvider;
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.ViewBehavior;
import com.genonbeta.android.framework.widget.recyclerview.fastscroll.provider.VisibilityAnimationManager;

/**
 * created by: veli
 * date: 10.04.2018 19:52
 */

public class LongTextBubbleFastScrollViewProvider extends ScrollerViewProvider
{
    private View mBubble;
    private View mHandle;

    @Override
    public View provideHandleView(ViewGroup container)
    {
        mHandle = new View(getContext());

        int verticalInset = getScroller().isVertical() ? 0 : getContext().getResources().getDimensionPixelSize(com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_inset);
        int horizontalInset = !getScroller().isVertical() ? 0 : getContext().getResources().getDimensionPixelSize(com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_inset);
        InsetDrawable handleBg = new InsetDrawable(ContextCompat.getDrawable(getContext(), com.genonbeta.android.framework.R.drawable.genfw_fastscroll_default_handle), horizontalInset, verticalInset, horizontalInset, verticalInset);
        Utils.setBackground(mHandle, handleBg);

        int handleWidth = getContext().getResources().getDimensionPixelSize(getScroller().isVertical() ? com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_clickable_width : com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_height);
        int handleHeight = getContext().getResources().getDimensionPixelSize(getScroller().isVertical() ? com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_height : com.genonbeta.android.framework.R.dimen.genfw_fastscroll_handle_clickable_width);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(handleWidth, handleHeight);

        mHandle.setLayoutParams(params);

        return mHandle;
    }

    @Override
    public View provideBubbleView(ViewGroup container)
    {
        mBubble = LayoutInflater.from(getContext()).inflate(R.layout.abstract_layout_fast_scroll_long_text_bubble_text_view, container, false);
        return mBubble;
    }

    @Override
    public TextView provideBubbleTextView()
    {
        return (TextView) mBubble;
    }

    @Override
    public int getBubbleOffset()
    {
        return (int) (getScroller().isVertical() ? (float) mHandle.getHeight() / 2f - (float) mBubble.getHeight() / 2f : (float) mHandle.getWidth() / 2f - (float) mBubble.getWidth() / 2);
    }

    @Override
    protected ViewBehavior provideHandleBehavior()
    {
        return null;
    }

    @Override
    protected ViewBehavior provideBubbleBehavior()
    {
        return new DefaultBubbleBehavior(new VisibilityAnimationManager.Builder(mBubble).withPivotX(1f).withPivotY(1f).build());
    }
}