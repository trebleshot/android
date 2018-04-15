package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.DefaultFragmentPagerAdapter;
import com.genonbeta.TrebleShot.app.Fragment;
import com.genonbeta.TrebleShot.ui.callback.TabLayoutSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;

/**
 * created by: veli
 * date: 11/04/18 20:52
 */
public class ConnectDevicesFragment
		extends Fragment
		implements TabLayoutSupport, TitleSupport
{
	private boolean mInitialized = false;
	private ViewPager mViewPager;
	private TabLayout mTabLayout;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		mInitialized = false;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		return mViewPager = (ViewPager) inflater.inflate(R.layout.layout_connect_devices, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		tryToInitialize();
	}

	@Override
	public boolean onTabLayout(TabLayout tabLayout)
	{
		mTabLayout = tabLayout;
		tryToInitialize();
		return true;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_connectDevices);
	}

	protected void tryToInitialize()
	{
		if (mInitialized || mTabLayout == null || mViewPager == null)
			return;

		DefaultFragmentPagerAdapter pagerAdapter = new DefaultFragmentPagerAdapter(getContext(), getChildFragmentManager());
		NetworkDeviceListFragment deviceListFragment = new NetworkDeviceListFragment();
		HotspotStatusFragment statusFragment = new HotspotStatusFragment();
		CodeConnectFragment connectFragment = new CodeConnectFragment();

		mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

		pagerAdapter.add(deviceListFragment, mTabLayout);
		pagerAdapter.add(statusFragment, mTabLayout);
		pagerAdapter.add(connectFragment, mTabLayout);

		mViewPager.setAdapter(pagerAdapter);
		mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

		mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
		{
			@Override
			public void onTabSelected(TabLayout.Tab tab)
			{
				mViewPager.setCurrentItem(tab.getPosition());
			}

			@Override
			public void onTabUnselected(final TabLayout.Tab tab)
			{

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab)
			{

			}
		});

		mInitialized = true;
	}
}
