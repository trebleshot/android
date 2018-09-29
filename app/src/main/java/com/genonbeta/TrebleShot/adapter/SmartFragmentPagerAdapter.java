package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 11/04/18 21:53
 */
public class SmartFragmentPagerAdapter extends FragmentPagerAdapter
{
	private List<StableItem> mItems = new ArrayList<>();
	private Context mContext;

	public SmartFragmentPagerAdapter(Context context, FragmentManager fm)
	{
		super(fm);
		mContext = context;
	}

	public void onItemInstantiated(StableItem item)
	{
	}

	public void add(StableItem fragment)
	{
		mItems.add(fragment);
	}

	public void add(int position, StableItem fragment)
	{
		mItems.add(position, fragment);
	}

	public void createTabs(TabLayout tabLayout)
	{
		if (getCount() > 0)
			for (int iterator = 0; iterator < getCount(); iterator++) {
				StableItem stableItem = getStableItem(iterator);
				Fragment fragment = getItem(iterator);
				TabLayout.Tab tab = tabLayout.newTab();

				if (fragment instanceof IconSupport)
					tab.setIcon(((IconSupport) fragment).getIconRes());

				if (!(stableItem.iconOnly && fragment instanceof IconSupport))
					if (stableItem.title != null && stableItem.title.length() > 0)
						tab.setText(stableItem.title);
					else if (fragment instanceof TitleSupport)
						tab.setText(((TitleSupport) fragment).getTitle(getContext()));

				tabLayout.addTab(tab);
			}
	}

	@NonNull
	@Override
	public Object instantiateItem(@NonNull ViewGroup container, int position)
	{
		Fragment fragment = (Fragment) super.instantiateItem(container, position);

		StableItem stableItem = getStableItem(position);
		stableItem.mInitiatedItem = fragment;
		stableItem.mCurrentPosition = position;

		if (fragment instanceof ShowingChangeListener)
			((ShowingChangeListener) fragment).getTabSelectionOracle()
					.setTabContext(true);

		onItemInstantiated(stableItem);

		return fragment;
	}

	public Context getContext()
	{
		return mContext;
	}

	@Override
	public int getCount()
	{
		return mItems.size();
	}

	public List<StableItem> getFragments()
	{
		return mItems;
	}

	@Override
	public long getItemId(int position)
	{
		return getStableItem(position).itemId;
	}

	@Override
	public Fragment getItem(int position)
	{
		StableItem stableItem = getStableItem(position);
		Fragment instantiatedItem = stableItem.getInitiatedItem();

		if (instantiatedItem == null)
			instantiatedItem = Fragment.instantiate(getContext(), stableItem.clazzName);

		instantiatedItem.setArguments(stableItem.arguments);

		return instantiatedItem;
	}

	@Nullable
	@Override
	public CharSequence getPageTitle(int position)
	{
		Fragment fragment = getItem(position);

		return fragment instanceof TitleSupport
				? ((TitleSupport) fragment).getTitle(getContext())
				: super.getPageTitle(position);
	}

	public StableItem getStableItem(int position)
	{
		return mItems.get(position);
	}

	public void notifyShowingChanges(final TabLayout.Tab tab, final boolean showing)
	{
		final StableItem item = getStableItem(tab.getPosition());

		if (item.getInitiatedItem() != null
				&& item.getInitiatedItem() instanceof ShowingChangeListener)
			new Handler(Looper.myLooper()).postDelayed(new Runnable()
			{
				@Override
				public void run()
				{
					if (item.getInitiatedItem().isAdded()) {
						ShowingChangeListener changeListener = (ShowingChangeListener) item.getInitiatedItem();

						changeListener.getTabSelectionOracle()
								.showing(showing);
					}
				}
			}, 400);
	}

	public interface ShowingChangeListener
	{
		TabSelectionOracle getTabSelectionOracle();

		void onNotifyShowingChange();
	}

	public static class TabLayoutSelectedListener implements TabLayout.OnTabSelectedListener
	{
		private SmartFragmentPagerAdapter mAdapter;

		public TabLayoutSelectedListener(SmartFragmentPagerAdapter adapter)
		{
			mAdapter = adapter;
		}

		@Override
		public void onTabSelected(TabLayout.Tab tab)
		{
			mAdapter.notifyShowingChanges(tab, true);
		}

		@Override
		public void onTabUnselected(TabLayout.Tab tab)
		{
			mAdapter.notifyShowingChanges(tab, false);
		}

		@Override
		public void onTabReselected(TabLayout.Tab tab)
		{

		}
	}

	public static class StableItem implements Parcelable
	{
		public static final Parcelable.Creator<StableItem> CREATOR = new Creator<StableItem>()
		{
			@Override
			public StableItem createFromParcel(Parcel source)
			{
				return new StableItem(source);
			}

			@Override
			public StableItem[] newArray(int size)
			{
				return new StableItem[size];
			}
		};

		protected Fragment mInitiatedItem;
		protected int mCurrentPosition = -1;

		public long itemId;
		public String clazzName;
		public Bundle arguments;
		public String title;
		public boolean iconOnly;

		public StableItem(long itemId, String clazzName, @Nullable Bundle arguments)
		{
			this.itemId = itemId;
			this.clazzName = clazzName;
			this.arguments = arguments;
		}

		public StableItem(long itemId, Class<? extends Fragment> clazz, @Nullable Bundle arguments)
		{
			this(itemId, clazz.getName(), arguments);
		}

		public StableItem(Parcel source)
		{
			this(source.readLong(), source.readString(), source.readBundle());

			setTitle(source.readString());
			setIconOnly(source.readInt() == 1);
		}

		public int getCurrentPosition()
		{
			return mCurrentPosition;
		}

		public Fragment getInitiatedItem()
		{
			return mInitiatedItem;
		}

		public StableItem setIconOnly(boolean iconOnly)
		{
			this.iconOnly = iconOnly;
			return this;
		}

		public StableItem setTitle(String title)
		{
			this.title = title;
			return this;
		}

		@Override
		public int describeContents()
		{
			return 0;
		}

		@Override
		public void writeToParcel(Parcel dest, int flags)
		{
			dest.writeLong(itemId);
			dest.writeString(clazzName);
			dest.writeBundle(arguments);
			dest.writeString(title);
			dest.writeInt(iconOnly ? 1 : 0);
		}
	}

	public static class TabSelectionOracle
	{
		private boolean mShowing = false;
		private boolean mTabContext = false;
		private boolean mInCycle = false;
		private ShowingChangeListener mListener;

		public TabSelectionOracle(@NonNull ShowingChangeListener listener)
		{
			mListener = listener;
		}

		public boolean isInCycle()
		{
			return mInCycle;
		}

		public boolean isResuming()
		{
			return isInCycle() && (isShowing() || !isTabContext());
		}

		public boolean isShowing()
		{
			return mShowing;
		}

		public boolean isTabContext()
		{
			return mTabContext;
		}

		public TabSelectionOracle cycle(boolean inCycle)
		{
			mInCycle = inCycle;

			if (!isTabContext() || isShowing())
				mListener.onNotifyShowingChange();

			return this;
		}

		public TabSelectionOracle showing(boolean showing)
		{
			mShowing = showing;
			mListener.onNotifyShowingChange();

			return this;
		}

		public TabSelectionOracle setTabContext(boolean tabContext)
		{
			mTabContext = tabContext;
			return this;
		}
	}
}
