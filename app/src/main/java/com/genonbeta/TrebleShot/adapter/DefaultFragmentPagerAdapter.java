package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.design.widget.TabLayout;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.app.Fragment;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 11/04/18 21:53
 */
public class DefaultFragmentPagerAdapter extends FragmentPagerAdapter
{
	private List<Fragment> mFragments = new ArrayList<>();
	private Context mContext;

	public DefaultFragmentPagerAdapter(Context context, FragmentManager fm)
	{
		super(fm);
		mContext = context;
	}

	public void add(Fragment fragment)
	{
		mFragments.add(fragment);
	}

	public void add(Fragment fragment, TabLayout tabLayout)
	{
		add(fragment);

		if (fragment instanceof TitleSupport)
			tabLayout.addTab(tabLayout.newTab().setText(((TitleSupport) fragment).getTitle(getContext())));
	}

	@Override
	public Object instantiateItem(ViewGroup container, int position)
	{
		Fragment fragment = (Fragment) super.instantiateItem(container, position);

		mFragments.set(position, fragment);

		return fragment;
	}


	public Context getContext()
	{
		return mContext;
	}

	public List<Fragment> getFragments()
	{
		return mFragments;
	}

	@Override
	public Fragment getItem(int position)
	{
		return mFragments.get(position);
	}

	@Override
	public int getCount()
	{
		return mFragments.size();
	}
}
