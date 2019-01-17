package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.MenuItem;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.tabs.TabLayout;

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
        createTabs(tabLayout, true, true);
    }

    public void createTabs(TabLayout tabLayout, boolean icons, boolean text)
    {
        if (getCount() > 0)
            for (int iterator = 0; iterator < getCount(); iterator++) {
                StableItem stableItem = getStableItem(iterator);
                Fragment fragment = getItem(iterator);
                TabLayout.Tab tab = tabLayout.newTab();

                if (fragment instanceof IconSupport && icons)
                    tab.setIcon(((IconSupport) fragment).getIconRes());

                if (!stableItem.iconOnly && text)
                    if (stableItem.title != null && stableItem.title.length() > 0)
                        tab.setText(stableItem.title);
                    else if (fragment instanceof TitleSupport)
                        tab.setText(((TitleSupport) fragment).getTitle(getContext()));

                tabLayout.addTab(tab);
            }
    }

    public void createTabs(BottomNavigationView bottomNavigationView)
    {
        if (getCount() > 0)
            for (int iterator = 0; iterator < getCount(); iterator++) {
                StableItem stableItem = getStableItem(iterator);
                Fragment fragment = getItem(iterator);
                CharSequence menuTitle = null;

                if (stableItem.title != null && stableItem.title.length() > 0)
                    menuTitle = stableItem.title;
                else if (fragment instanceof TitleSupport)
                    menuTitle = ((TitleSupport) fragment).getTitle(getContext());
                else
                    menuTitle = String.valueOf(iterator);

                MenuItem menuItem = bottomNavigationView.getMenu()
                        .add(0, iterator, iterator, menuTitle);

                if (fragment instanceof IconSupport)
                    menuItem.setIcon(((IconSupport) fragment).getIconRes());
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
        public long itemId;
        public String clazzName;
        public Bundle arguments;
        public String title;
        public boolean iconOnly;
        protected Fragment mInitiatedItem;
        protected int mCurrentPosition = -1;

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
}
