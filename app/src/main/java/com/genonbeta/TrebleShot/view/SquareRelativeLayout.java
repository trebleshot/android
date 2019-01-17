package com.genonbeta.TrebleShot.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

import com.genonbeta.TrebleShot.R;

/**
 * created by: Veli
 * date: 27.03.2018 22:32
 */

public class SquareRelativeLayout extends RelativeLayout
{
    private boolean mBaseOnSmaller = false;

    public SquareRelativeLayout(Context context)
    {
        this(context, null);
    }

    public SquareRelativeLayout(Context context, AttributeSet attrs)
    {
        this(context, attrs, 0);
    }

    public SquareRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);

        final TypedArray typedAttributes = context.getTheme()
                .obtainStyledAttributes(attrs, R.styleable.SquareRelativeLayout, defStyleAttr, 0);

        mBaseOnSmaller = typedAttributes.getBoolean(R.styleable.SquareRelativeLayout_baseOnSmallerLength, mBaseOnSmaller);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        // Set a square layout.
        int chosenLength = widthMeasureSpec;

        if (mBaseOnSmaller)
            chosenLength = widthMeasureSpec <= heightMeasureSpec
                    ? widthMeasureSpec
                    : heightMeasureSpec;

        super.onMeasure(chosenLength, chosenLength);
    }
}
