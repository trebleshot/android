package com.genonbeta.TrebleShot.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.RelativeLayout;

import com.genonbeta.TrebleShot.R;

/**
 * created by: Veli
 * date: 27.03.2018 22:32
 */

public class ComparativeRelativeLayout extends RelativeLayout
{
    private boolean mAlwaysUseWidth = true;
    private boolean mBaseOnSmaller = false;
    private int mTallerExtraLength = 0;

    public ComparativeRelativeLayout(Context context)
    {
        this(context, null);
    }

    public ComparativeRelativeLayout(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public ComparativeRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        final TypedArray typedAttributes = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.ComparativeRelativeLayout, defStyleAttr, 0);

        mBaseOnSmaller = typedAttributes.getBoolean(
                R.styleable.ComparativeRelativeLayout_baseOnSmallerLength, mBaseOnSmaller);
        mTallerExtraLength = typedAttributes.getDimensionPixelSize(
                R.styleable.ComparativeRelativeLayout_tallerLengthExtra, mTallerExtraLength);
        mAlwaysUseWidth = typedAttributes.getBoolean(
                R.styleable.ComparativeRelativeLayout_alwaysUseWidth, mAlwaysUseWidth);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // Set a proportional layout.
        if (mBaseOnSmaller) {
            if (widthMeasureSpec > heightMeasureSpec)
                widthMeasureSpec = heightMeasureSpec + mTallerExtraLength;
            else if (heightMeasureSpec > widthMeasureSpec)
                heightMeasureSpec = widthMeasureSpec + mTallerExtraLength;
        } else if (mAlwaysUseWidth)
            heightMeasureSpec = widthMeasureSpec + mTallerExtraLength;
        else
            widthMeasureSpec = heightMeasureSpec + mTallerExtraLength;

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
