package com.genonbeta.TrebleShot.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ConnectionManagerActivity;
import com.genonbeta.TrebleShot.activity.ContentSharingActivity;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.android.framework.ui.callback.SnackbarSupport;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;

public class HomeFragment
        extends com.genonbeta.android.framework.app.Fragment
        implements TitleSupport, SnackbarSupport, com.genonbeta.android.framework.app.FragmentImpl
{
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
    {
        final View view = inflater.inflate(R.layout.layout_home_fragment, container, false);

        final ViewPager viewPager = view.findViewById(R.id.layout_home_view_pager);
        final BottomNavigationView bottomNavigationView = view.findViewById(R.id.layout_home_bottom_navigation_view);
        final SmartFragmentPagerAdapter pagerAdapter = new SmartFragmentPagerAdapter(getContext(), getChildFragmentManager());

        pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(0, TransferGroupListFragment.class, null));

        pagerAdapter.createTabs(bottomNavigationView);
        viewPager.setAdapter(pagerAdapter);

        Menu bottomMenu = bottomNavigationView.getMenu();

        bottomMenu.add(0, 1, 1, getString(R.string.butn_share))
                .setIcon(R.drawable.ic_file_upload_white_24dp);
        bottomMenu.add(0, 2, 2, getString(R.string.butn_receive))
                .setIcon(R.drawable.ic_file_download_white_24dp);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener()
        {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem)
            {
                if (menuItem.getOrder() == 1)
                    startActivity(new Intent(getContext(), ContentSharingActivity.class));
                else if (menuItem.getOrder() == 2)
                    startActivity(new Intent(getContext(), ConnectionManagerActivity.class));

                return false;
            }
        });

        return view;
    }

    @Override
    public CharSequence getTitle(Context context)
    {
        return context.getString(R.string.text_home);
    }
}
