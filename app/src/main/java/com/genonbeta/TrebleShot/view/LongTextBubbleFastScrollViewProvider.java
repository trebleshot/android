package com.genonbeta.TrebleShot.view;

/*
import com.futuremind.recyclerviewfastscroll.Utils;
import com.futuremind.recyclerviewfastscroll.viewprovider.DefaultBubbleBehavior;
import com.futuremind.recyclerviewfastscroll.viewprovider.ScrollerViewProvider;
import com.futuremind.recyclerviewfastscroll.viewprovider.ViewBehavior;
import com.futuremind.recyclerviewfastscroll.viewprovider.VisibilityAnimationManager;*/

/**
 * created by: veli
 * date: 10.04.2018 19:52
 */

// FIXME: 10/28/18
/*
public class LongTextBubbleFastScrollViewProvider extends ScrollerViewProvider
{
	private View mBubble;
	private View mHandle;

	@Override
	public View provideHandleView(ViewGroup container)
	{
		mHandle = new View(getContext());

		int verticalInset = getScroller().isVertical() ? 0 : getContext().getResources().getDimensionPixelSize(com.futuremind.recyclerviewfastscroll.R.dimen.fastscroll__handle_inset);
		int horizontalInset = !getScroller().isVertical() ? 0 : getContext().getResources().getDimensionPixelSize(com.futuremind.recyclerviewfastscroll.R.dimen.fastscroll__handle_inset);
		InsetDrawable handleBg = new InsetDrawable(ContextCompat.getDrawable(getContext(), com.futuremind.recyclerviewfastscroll.R.drawable.fastscroll__default_handle), horizontalInset, verticalInset, horizontalInset, verticalInset);
		Utils.setBackground(mHandle, handleBg);

		int handleWidth = getContext().getResources().getDimensionPixelSize(getScroller().isVertical() ? com.futuremind.recyclerviewfastscroll.R.dimen.fastscroll__handle_clickable_width : com.futuremind.recyclerviewfastscroll.R.dimen.fastscroll__handle_height);
		int handleHeight = getContext().getResources().getDimensionPixelSize(getScroller().isVertical() ? com.futuremind.recyclerviewfastscroll.R.dimen.fastscroll__handle_height : com.futuremind.recyclerviewfastscroll.R.dimen.fastscroll__handle_clickable_width);
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
}*/