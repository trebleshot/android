package com.genonbeta.TrebleShot.adapter;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class SimpleFragmentPagerAdapter extends FragmentPagerAdapter
{
	private Context mContext;
	private Fragment[] mFragments;
	private String[] mTitles;

	public SimpleFragmentPagerAdapter(FragmentManager fm, Context context, Fragment[] fragments, String[] titles)
	{
		super(fm);

		mContext = context;
		mFragments = fragments;
		mTitles = titles;
	}

	@Override
	public int getCount()
	{
		return mFragments.length;
	}

	@Override
	public Fragment getItem(int id)
	{
		return mFragments[id];
	}

	@Override
	public CharSequence getPageTitle(int position)
	{
		return mTitles[position];
	}
}
