package com.genonbeta.TrebleShot.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.ArrayMap;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.object.Selectable;
import com.genonbeta.TrebleShot.object.Shareable;

import java.util.ArrayList;

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

	public <T extends Selectable> boolean check(@NonNull Callback<T> callback, T selectable, boolean selected)
	{
		if (!hasActive(callback))
			return false;

		selectable.setSelectableSelected(selected);

		if (selected)
			getHolder(callback).getSelectionList().add(selectable);
		else
			getHolder(callback).getSelectionList().remove(selectable);

		callback.onItemChecked(getContext(), this, selectable);

		return true;
	}

	public <T extends Selectable> void enableFor(final SelectorConnection<T> selectorConnection)
	{
		selectorConnection.getCallback().getActionModeListView()
				.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
				{
					@Override
					public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l)
					{
						if (PowerfulActionMode.this.start(selectorConnection.getCallback())) {
							selectorConnection.setSelected(selectorConnection.getCallback().getSelectableList().get(i));
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
			callback.onFinish(getContext(), this);

			mActiveActionModes.remove(callback);

			reload(callback);
		}
	}

	public boolean hasActive(Callback callback)
	{
		return mActiveActionModes.containsKey(callback);
	}

	public View getContainerLayout()
	{
		return mContainerLayout;
	}

	public MenuInflater getMenuInflater()
	{
		return mMenuInflater;
	}

	public <T extends Selectable> Holder<T> getHolder(Callback<T> callback)
	{
		return mActiveActionModes.get(callback);
	}

	protected void initialize()
	{
		mMenuInflater = new MenuInflater(getContext());
	}

	public boolean reload(final Callback callback)
	{
		getMenu().clear();

		if (callback == null || !mActiveActionModes.containsKey(callback)) {
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
		mContainerLayout = containerLayout;
	}

	public <T extends Selectable> boolean start(@NonNull final Callback<T> callback)
	{
		if (!callback.onPrepareActionMenu(getContext(), this) || mActiveActionModes.containsKey(callback)) {
			finish(callback);
			return false;
		}

		mActiveActionModes.put(callback, new Holder());

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

	public interface Callback<T extends Selectable>
	{
		AbsListView getActionModeListView();

		ArrayList<T> getSelectableList();

		boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode);

		boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu);

		boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item);

		void onItemChecked(Context context, PowerfulActionMode actionMode, T selectable);

		void onFinish(Context context, PowerfulActionMode actionMode);
	}

	public static class SelectorConnection<T extends Selectable>
	{
		private PowerfulActionMode mMode;
		private Callback<T> mCallback;

		public SelectorConnection(PowerfulActionMode mode, Callback<T> callback)
		{
			mMode = mode;
			mCallback = callback;
		}

		public Callback<T> getCallback()
		{
			return mCallback;
		}

		public PowerfulActionMode getMode()
		{
			return mMode;
		}

		public ArrayList<T> getSelectedItemList()
		{
			Holder<T> holder = getMode().getHolder(getCallback());

			return holder == null ? new ArrayList<T>() : holder.getSelectionList();
		}

		public boolean isSelected(T selectable)
		{
			Holder<T> holder = getMode().getHolder(getCallback());

			return holder != null && holder.getSelectionList().contains(selectable);
		}

		public boolean selectionActive()
		{
			return getMode().hasActive(getCallback());
		}

		public boolean setSelected(T selectable)
		{
			return setSelected(selectable, !isSelected(selectable));
		}

		public boolean setSelected(T selectable, boolean selected)
		{
			// if it is already the same
			if (selected == isSelected(selectable))
				return selected;

			if (!getMode().hasActive(getCallback()))
				getMode().start(getCallback());

			return getMode().check(getCallback(), selectable, selected);
		}

		public boolean removeSelected(T selectable)
		{
			if (!getMode().hasActive(getCallback()))
				return false;

			return getMode().getHolder(getCallback())
					.getSelectionList()
					.remove(selectable);
		}
	}

	public static class Holder<T extends Selectable>
	{
		private final ArrayList<T> mSelectionList = new ArrayList<>();

		public ArrayList<T> getSelectionList()
		{
			synchronized (mSelectionList) {
				return mSelectionList;
			}
		}
	}
}
