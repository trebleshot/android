package com.genonbeta.TrebleShot.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
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
		super(context);
	}

	public SquareRelativeLayout(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		initialize(attrs);
	}

	public SquareRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		initialize(attrs);
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SquareRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
	{
		super(context, attrs, defStyleAttr, defStyleRes);
		initialize(attrs);
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

	private void initialize(AttributeSet attrs)
	{
		for (int i = 0; i < attrs.getAttributeCount(); i++) {
			if (attrs.getAttributeNameResource(i) == R.attr.baseOnSmallerLength)
				mBaseOnSmaller = attrs.getAttributeBooleanValue(i, false);
		}
	}
}
