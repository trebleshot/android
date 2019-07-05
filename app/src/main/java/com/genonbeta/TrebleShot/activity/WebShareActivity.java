/*
 * Copyright (C) 2019 Veli Tasalı
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.fragment.ActiveConnectionListFragment;
import com.genonbeta.TrebleShot.fragment.NetworkDeviceListFragment;
import com.genonbeta.TrebleShot.fragment.ShareableListFragment;
import com.genonbeta.TrebleShot.fragment.FileExplorerFragment;
import com.genonbeta.TrebleShot.fragment.TransferFileExplorerFragment;
import com.genonbeta.TrebleShot.fragment.TransferListFragment;
import com.genonbeta.TrebleShot.object.NetworkDevice;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.List;

/**
 * created by: veli
 * date: 4/6/19 4:57 PM
 */
public class WebShareActivity extends Activity
{
    public static final String EXTRA_WEBSERVER_START_REQUIRED = "extraStartRequired";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_share);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final TabLayout tabLayout = findViewById(R.id.tab_layout);
        final ViewPager viewPager = findViewById(R.id.view_pager);
        final SmartFragmentPagerAdapter pagerAdapter = new SmartFragmentPagerAdapter(this,
                getSupportFragmentManager());

        Bundle transferListArgs = new Bundle();
        transferListArgs.putLong(TransferListFragment.ARG_GROUP_ID, AppConfig.ID_GROUP_WEB_SHARE);

        ArrayList<String> hiddenDeviceTypes = new ArrayList<>();
        hiddenDeviceTypes.add(NetworkDevice.Type.NORMAL.toString());

        Bundle deviceListArgs = new Bundle();
        deviceListArgs.putStringArrayList(NetworkDeviceListFragment.ARG_HIDDEN_DEVICES_LIST,
                hiddenDeviceTypes);

        pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(0, ActiveConnectionListFragment.class, null));
        pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(1, TransferFileExplorerFragment.class,
                transferListArgs).setTitle(getString(R.string.text_receivedFiles)));
        pagerAdapter.add(new SmartFragmentPagerAdapter.StableItem(2, NetworkDeviceListFragment.class,
                deviceListArgs).setTitle(getString(R.string.text_clients)));

        pagerAdapter.createTabs(tabLayout, false, true);
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

        if (getIntent() != null && getIntent().hasExtra(EXTRA_WEBSERVER_START_REQUIRED)
                && getIntent().getBooleanExtra(EXTRA_WEBSERVER_START_REQUIRED, false))
            AppUtils.toggleWebShare(this,true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }
}
