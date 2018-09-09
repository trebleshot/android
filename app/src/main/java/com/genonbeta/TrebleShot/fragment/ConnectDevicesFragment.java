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
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.ui.callback.NetworkDeviceSelectedListener;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 11/04/18 20:52
 */
public class ConnectDevicesFragment
		extends com.genonbeta.android.framework.app.Fragment
		implements TitleSupport, SnackbarSupport, com.genonbeta.android.framework.app.FragmentImpl
{
	public static final String EXTRA_CDF_FRAGMENT_NAMES_FRONT = "extraCdfFragmentNamesFront";
	public static final String EXTRA_CDF_FRAGMENT_NAMES_BACK = "extraCdfFragmentNamesBack";

	private NetworkDeviceSelectedListener mDeviceSelectedListener;
	private SmartFragmentPagerAdapter mPagerAdapter;
	private ViewPager mViewPager;
	private TabLayout mTabLayout;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		if (getActivity() instanceof Activity.OnPreloadArgumentWatcher)
		{
			if (getArguments()  == null)
				setArguments(new Bundle());

			getArguments().putAll(((Activity.OnPreloadArgumentWatcher) getActivity()).passPreloadingArguments());
		}
	}

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
		mPagerAdapter = new SmartFragmentPagerAdapter(getContext(), getChildFragmentManager())
		{
			@Override
			public void onItemInstantiated(StableItem item)
			{
				if (item.getInitiatedItem() instanceof NetworkDeviceListFragment)
					((NetworkDeviceListFragment) item.getInitiatedItem()).setDeviceSelectedListener(selectionListenerDevices);
				else if (item.getInitiatedItem() instanceof CodeConnectFragment)
					((CodeConnectFragment) item.getInitiatedItem()).setDeviceSelectedListener(selectionListener);
				else if (item.getInitiatedItem() instanceof NetworkStatusFragment)
					((NetworkStatusFragment) item.getInitiatedItem()).setDeviceSelectedListener(selectionListener);
			}
		};


		loadIntoSmartPagerAdapterUsingKey(mPagerAdapter, getArguments(), EXTRA_CDF_FRAGMENT_NAMES_FRONT);

		mPagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(0, NetworkDeviceListFragment.class, null));
		mPagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(1, NetworkStatusFragment.class, null));
		mPagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(2, CodeConnectFragment.class, null));

		mPagerAdapter.createTabs(mTabLayout);
		mTabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

		loadIntoSmartPagerAdapterUsingKey(mPagerAdapter, getArguments(), EXTRA_CDF_FRAGMENT_NAMES_BACK);

		mViewPager.setAdapter(mPagerAdapter);
		mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

		mTabLayout.addOnTabSelectedListener(new SmartFragmentPagerAdapter.TabLayoutSelectedListener(mPagerAdapter)
		{
			@Override
			public void onTabSelected(TabLayout.Tab tab)
			{
				super.onTabSelected(tab);
				mViewPager.setCurrentItem(tab.getPosition());
			}
		});

		return view;
	}

	public SmartFragmentPagerAdapter getPagerAdapter()
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

	public void loadIntoSmartPagerAdapterUsingKey(SmartFragmentPagerAdapter pagerAdapter, Bundle args, String key)
	{
		if (args == null || !args.containsKey(key))
			return;

		ArrayList<SmartFragmentPagerAdapter.StableItem> thisParcelables = args.getParcelableArrayList(key);

		if (thisParcelables == null)
			return;

		for (SmartFragmentPagerAdapter.StableItem thisItem : thisParcelables)
			pagerAdapter.add(thisItem);
	}

	public void showDevices()
	{
		showFragment(NetworkDeviceListFragment.class);
	}

	public void showFragment(Class clazz) {
		synchronized (getPagerAdapter().getFragments()) {
			int iterator = 0;

			for (SmartFragmentPagerAdapter.StableItem stableItem : getPagerAdapter().getFragments()) {
				if (clazz.getName().equals(stableItem.clazzName)) {
					mViewPager.setCurrentItem(iterator);
					break;
				}

				iterator++;
			}
		}
	}
}
