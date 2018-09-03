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
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;

/**
 * created by: veli
 * date: 11/04/18 20:52
 */
public class ConnectDevicesFragment
		extends com.genonbeta.android.framework.app.Fragment
		implements TitleSupport, SnackbarSupport, com.genonbeta.android.framework.app.FragmentImpl
{
	private NetworkDeviceSelectedListener mDeviceSelectedListener;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		final View view = inflater.inflate(R.layout.layout_connect_devices, container, false);
		final TabLayout tabLayout = view.findViewById(R.id.activity_transaction_tab_layout);
		final ViewPager viewPager = view.findViewById(R.id.activity_transaction_view_pager);

		DefaultFragmentPagerAdapter pagerAdapter = new DefaultFragmentPagerAdapter(getContext(), getChildFragmentManager());
		NetworkDeviceListFragment deviceListFragment = new NetworkDeviceListFragment();
		NetworkStatusFragment statusFragment = new NetworkStatusFragment();
		CodeConnectFragment connectFragment = new CodeConnectFragment();

		deviceListFragment.setDeviceSelectedListener(new NetworkDeviceSelectedListener()
		{
			@Override
			public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection)
			{
				return mDeviceSelectedListener != null
						&& mDeviceSelectedListener.onNetworkDeviceSelected(networkDevice, connection);
			}

			@Override
			public boolean isListenerEffective()
			{
				return mDeviceSelectedListener != null;
			}
		});

		NetworkDeviceSelectedListener selectedListener = new NetworkDeviceSelectedListener()
		{
			@Override
			public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection)
			{
				viewPager.setCurrentItem(0);
				return mDeviceSelectedListener != null
						&& mDeviceSelectedListener.onNetworkDeviceSelected(networkDevice, connection);
			}

			@Override
			public boolean isListenerEffective()
			{
				return true;
			}
		};

		statusFragment.setDeviceSelectedListener(selectedListener);
		connectFragment.setDeviceSelectedListener(selectedListener);

		tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

		pagerAdapter.add(deviceListFragment, tabLayout);
		pagerAdapter.add(statusFragment, tabLayout);
		pagerAdapter.add(connectFragment, tabLayout);

		viewPager.setAdapter(pagerAdapter);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
		{
			@Override
			public void onTabSelected(TabLayout.Tab tab)
			{
				viewPager.setCurrentItem(tab.getPosition());
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

		return view;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_connectDevices);
	}

	public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
	{
		mDeviceSelectedListener = listener;
	}
}
