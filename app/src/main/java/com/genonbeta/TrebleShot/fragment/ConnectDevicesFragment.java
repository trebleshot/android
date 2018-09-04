package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.DefaultFragmentPagerAdapter;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
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
	private DefaultFragmentPagerAdapter mPagerAdapter;
	private ViewPager mViewPager;
	private TabLayout mTabLayout;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
	{
		final View view = inflater.inflate(R.layout.layout_connect_devices, container, false);
		mViewPager = view.findViewById(R.id.activity_transaction_view_pager);

		final NetworkDeviceSelectedListener selectionListenerDevices = new NetworkDeviceSelectedListener()
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
		};

		final NetworkDeviceSelectedListener selectionListener = new NetworkDeviceSelectedListener()
		{
			@Override
			public boolean onNetworkDeviceSelected(NetworkDevice networkDevice, @Nullable NetworkDevice.Connection connection)
			{
				showDevices();

				return mDeviceSelectedListener != null
						&& mDeviceSelectedListener.onNetworkDeviceSelected(networkDevice, connection);
			}

			@Override
			public boolean isListenerEffective()
			{
				return true;
			}
		};

		mTabLayout = view.findViewById(R.id.activity_transaction_tab_layout);
		mPagerAdapter = new DefaultFragmentPagerAdapter(getContext(), getChildFragmentManager())
		{
			@NonNull
			@Override
			public Object instantiateItem(ViewGroup container, int position)
			{
				Fragment fragment = (Fragment) super.instantiateItem(container, position);

				if (fragment instanceof NetworkDeviceListFragment)
					((NetworkDeviceListFragment) fragment).setDeviceSelectedListener(selectionListenerDevices);
				else if (fragment instanceof CodeConnectFragment)
					((CodeConnectFragment) fragment).setDeviceSelectedListener(selectionListener);
				else if (fragment instanceof NetworkStatusFragment)
					((NetworkStatusFragment) fragment).setDeviceSelectedListener(selectionListener);

				return fragment;
			}

			@Override
			public long getItemId(int position)
			{
				return getItem(position).getClass().getName().hashCode();
			}

			@Override
			public void notifyDataSetChanged()
			{
				super.notifyDataSetChanged();

				getTabLayout().removeAllTabs();

				for (Fragment fragment : getFragments()) {
					if (fragment instanceof IconSupport) {
						TabLayout.Tab thisTab = getTabLayout()
								.newTab()
								.setIcon(((IconSupport) fragment).getIconRes());

						DrawableCompat.setTint(thisTab.getIcon(), ContextCompat.getColor(getContext(), R.color.layoutTintColor));

						getTabLayout().addTab(thisTab);
					} else if (fragment instanceof TitleSupport)
						getTabLayout().addTab(getTabLayout()
								.newTab()
								.setText(((TitleSupport) fragment).getTitle(getContext())));
				}
			}
		};

		NetworkDeviceListFragment deviceListFragment = new NetworkDeviceListFragment();
		NetworkStatusFragment statusFragment = new NetworkStatusFragment();
		CodeConnectFragment connectFragment = new CodeConnectFragment();

		mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

		mPagerAdapter.add(deviceListFragment);
		mPagerAdapter.add(statusFragment);
		mPagerAdapter.add(connectFragment);

		mViewPager.setAdapter(mPagerAdapter);
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

		return view;
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);
		mPagerAdapter.notifyDataSetChanged();
	}

	public DefaultFragmentPagerAdapter getPagerAdapter()
	{
		return mPagerAdapter;
	}

	public TabLayout getTabLayout()
	{
		return mTabLayout;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_connectDevices);
	}

	public ViewPager getViewPager()
	{
		return mViewPager;
	}

	public void setDeviceSelectedListener(NetworkDeviceSelectedListener listener)
	{
		mDeviceSelectedListener = listener;
	}

	public void showDevices() {
		synchronized (getPagerAdapter().getFragments()) {
			int iterator = 0;

			for (Fragment fragment : getPagerAdapter().getFragments()) {
				if (fragment instanceof NetworkDeviceListFragment) {
					mViewPager.setCurrentItem(iterator);
					break;
				}

				iterator ++;
			}
		}
	}
}
