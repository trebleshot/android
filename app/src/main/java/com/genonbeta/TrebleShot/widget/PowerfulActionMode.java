package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.genonbeta.TrebleShot.R;

/**
 * created by: Veli
 * date: 19.11.2017 10:01
 */

public class PowerfulActionMode extends Toolbar
{
	private View mContainerLayout;
	private MenuInflater mMenuInflater;
	private ArrayMap<Callback, Holder> mActiveActionModes = new ArrayMap<>();

	public PowerfulActionMode(Context context)
	{
		super(context);
		initialize();
	}

	public PowerfulActionMode(Context context, @Nullable AttributeSet attrs)
	{
		super(context, attrs);
		initialize();
	}

	public PowerfulActionMode(Context context, @Nullable AttributeSet attrs, int defStyleAttr)
	{
		super(context, attrs, defStyleAttr);
		initialize();
	}

	protected void initialize()
	{
		mMenuInflater = new MenuInflater(getContext());
	}

	public boolean check(@NonNull Callback callback, int position, boolean select)
	{
		if (!mActiveActionModes.containsKey(callback))
			return false;

		callback.getActionModeListView().setItemChecked(position, select);
		callback.onItemChecked(getContext(), this, position, select);

		return true;
	}

	public void enableFor(final Callback callback)
	{
		callback.getActionModeListView()
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
				{
					@Override
					public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
					{
						if (PowerfulActionMode.this.start(callback)) {
							PowerfulActionMode.this.check(callback, i, true);
							return true;
						}

						return false;
					}
				});
	}

	public void finish(@NonNull final Callback callback)
	{
		final Holder holder = mActiveActionModes.get(callback);

		if (holder != null) {
			callback.getActionModeListView().setOnItemClickListener(holder.overriddenClickListener);

			for (int position = 0; position < callback.getActionModeListView().getCount(); position++)
				callback.getActionModeListView().setItemChecked(position, false);

			callback.onFinish(getContext(), this);
			mActiveActionModes.remove(callback);
			reload(callback);

			new Handler(Looper.getMainLooper()).post(new Runnable()
			{
				@Override
				public void run()
				{
					try {
						// IDK but this somehow or rather works, Thanks androod!
						Thread.sleep(100);
						callback.getActionModeListView().setChoiceMode(holder.previousChoiceMode);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			});
		}
	}

	public View getContainerLayout()
	{
		return mContainerLayout;
	}

	public MenuInflater getMenuInflater()
	{
		return mMenuInflater;
	}

	public boolean reload(final Callback callback)
	{
		getMenu().clear();

		if (!mActiveActionModes.containsKey(callback)) {
			updateVisibility(GONE);
			finish(callback);
			return false;
		}

		updateVisibility(VISIBLE);

		setNavigationIcon(R.drawable.ic_close_white_24dp);
		setNavigationContentDescription(R.string.butn_close);
		setNavigationOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				finish(callback);
			}
		});

		boolean result = callback.onCreateActionMenu(getContext(), this, getMenu());

		// As we can't define the !?*_- listener with ease I had to hack into it using this
		if (result) {
			MenuItem.OnMenuItemClickListener defListener = new MenuItem.OnMenuItemClickListener()
			{
				@Override
				public boolean onMenuItemClick(MenuItem menuItem)
				{
					boolean didTrigger = callback.onActionMenuItemSelected(getContext(), PowerfulActionMode.this, menuItem);

					if (didTrigger)
						finish(callback);

					return didTrigger;
				}
			};

			for (int menuPos = 0; menuPos < getMenu().size(); menuPos++)
				getMenu()
						.getItem(menuPos)
						.setOnMenuItemClickListener(defListener);
		}

		return result;
	}

	public void setContainerLayout(View containerLayout)
	{
		this.mContainerLayout = containerLayout;
	}

	public boolean start(@NonNull final Callback callback)
	{
		if (!callback.onPrepareActionMenu(getContext(), this) || mActiveActionModes.containsKey(callback)) {
			finish(callback);
			return false;
		}

		Holder holder = new Holder();

		holder.overriddenClickListener = callback.getActionModeListView().getOnItemClickListener();
		holder.previousChoiceMode = callback.getActionModeListView().getChoiceMode();


		callback.getActionModeListView().setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE);

		callback.getActionModeListView().setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int i, long l)
			{
				boolean check = callback.getActionModeListView().isItemChecked(i);
				PowerfulActionMode.this.check(callback, i, check);
			}
		});

		mActiveActionModes.put(callback, holder);

		return reload(callback);
	}

	protected void updateVisibility(int visibility)
	{
		int animationId = visibility == VISIBLE ? android.R.anim.fade_in : android.R.anim.fade_out;
		View view = getContainerLayout() == null ? this : getContainerLayout();

		if (visibility == VISIBLE) {
			view.setVisibility(visibility);
			view.setAnimation(AnimationUtils.loadAnimation(getContext(), animationId));
		} else {
			view.setAnimation(AnimationUtils.loadAnimation(getContext(), animationId));
			view.setVisibility(visibility);
		}
	}

	public interface Callback
	{
		AbsListView getActionModeListView();

		boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode);

		boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu);

		boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item);

		void onItemChecked(Context context, PowerfulActionMode actionMode, int position, boolean isSelected);

		void onFinish(Context context, PowerfulActionMode actionMode);
	}

	public static class Holder
	{
		public AdapterView.OnItemClickListener overriddenClickListener;
		public int previousChoiceMode;
	}
}
