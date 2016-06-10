package com.genonbeta.TrebleShot.fragment;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.TrebleShotActivity;
import com.genonbeta.TrebleShot.adapter.SimpleFragmentPagerAdapter;

public class SwipableMainActivityFragment extends Fragment {
    private ViewPager mPager;
    private TabLayout mTabLayout;
    private Fragment mDevicesFragment;
    private Fragment mFilesFragment;
    private Fragment mSendToolsFragment;
    private SimpleFragmentPagerAdapter mFragmentPager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_swipable_main, container, false);

        this.mPager = (ViewPager) view.findViewById(R.id.fragment_swipable_main_view_pager);
        this.mTabLayout = (TabLayout) view.findViewById(R.id.fragment_swipable_main_tab_layout);

        this.mDevicesFragment = Fragment.instantiate(getActivity(), NetworkDeviceListFragment.class.getName(), savedInstanceState);
        this.mFilesFragment = Fragment.instantiate(getActivity(), ReceivedFilesListFragment.class.getName(), savedInstanceState);
        this.mSendToolsFragment = Fragment.instantiate(getActivity(), SendToolsFragment.class.getName(), savedInstanceState);

        this.mFragmentPager = new SimpleFragmentPagerAdapter(getFragmentManager(), getActivity(), new Fragment[]{mDevicesFragment, mFilesFragment, mSendToolsFragment}, new String[]{getString(R.string.device_list), getString(R.string.received), getString(R.string.share)});

        this.mPager.setAdapter(this.mFragmentPager);
        this.mTabLayout.setupWithViewPager(this.mPager);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getActivity().getIntent() != null && TrebleShotActivity.OPEN_RECEIVED_FILES_ACTION.equals(getActivity().getIntent().getAction())) {
            mPager.setCurrentItem(1, true);
            getActivity().getIntent().setAction(null);
        }
    }
}
