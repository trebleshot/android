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

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.ActionMenuView;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter;
import com.genonbeta.TrebleShot.adapter.SmartFragmentPagerAdapter.StableItem;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragmentBase;
import com.genonbeta.TrebleShot.fragment.*;
import com.genonbeta.TrebleShot.ui.callback.SharingPerformerMenuCallback;
import com.genonbeta.TrebleShot.util.SelectionUtils;
import com.genonbeta.TrebleShot.widget.EditableListAdapterBase;
import com.genonbeta.android.framework.ui.PerformerMenu;
import com.genonbeta.android.framework.util.actionperformer.IPerformerEngine;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngine;
import com.genonbeta.android.framework.util.actionperformer.PerformerEngineProvider;
import com.google.android.material.tabs.TabLayout;

/**
 * created by: veli
 * date: 13/04/18 19:45
 */
public class ContentSharingActivity extends Activity implements PerformerEngineProvider
{
    public static final String TAG = ContentSharingActivity.class.getSimpleName();

    private Activity.OnBackPressedListener mBackPressedListener;
    private final PerformerEngine mPerformerEngine = new PerformerEngine();
    private final SharingPerformerMenuCallback mMenuCallback = new SharingPerformerMenuCallback(this,
            this);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_content_sharing);
        ActionMenuView menuView = findViewById(R.id.menu_view);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        PerformerMenu performerMenu = new PerformerMenu(this, mMenuCallback);

        mMenuCallback.setCancellable(false);
        performerMenu.load(menuView.getMenu());
        performerMenu.setUp(mPerformerEngine);

        final TabLayout tabLayout = findViewById(R.id.activity_content_sharing_tab_layout);
        final ViewPager viewPager = findViewById(R.id.activity_content_sharing_view_pager);

        final SmartFragmentPagerAdapter pagerAdapter = new SmartFragmentPagerAdapter(this,
                getSupportFragmentManager())
        {
            @Override
            public void onItemInstantiated(StableItem item)
            {
                Fragment fragment = item.getInitiatedItem();

                if (fragment instanceof EditableListFragmentBase<?>) {
                    EditableListFragmentBase<?> fragmentImpl = (EditableListFragmentBase<?>) fragment;

                    if (viewPager.getCurrentItem() == item.getCurrentPosition())
                        attachListeners(fragmentImpl);
                }
            }
        };

        Bundle arguments = new Bundle();
        arguments.putBoolean(FileExplorerFragment.ARG_SELECT_BY_CLICK, true);
        arguments.putBoolean(FileExplorerFragment.ARG_HAS_BOTTOM_SPACE, true);

        pagerAdapter.add(new StableItem(0, ApplicationListFragment.class, arguments));
        pagerAdapter.add(new StableItem(1, FileExplorerFragment.class, arguments).setTitle(getString(
                R.string.text_files)));
        pagerAdapter.add(new StableItem(2, AudioListFragment.class, arguments));
        pagerAdapter.add(new StableItem(3, ImageListFragment.class, arguments));
        pagerAdapter.add(new StableItem(4, VideoListFragment.class, arguments));

        pagerAdapter.createTabs(tabLayout, false, true);
        viewPager.setAdapter(pagerAdapter);
        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
        {
            @Override
            public void onTabSelected(TabLayout.Tab tab)
            {
                viewPager.setCurrentItem(tab.getPosition());
                Fragment fragment = pagerAdapter.getItem(tab.getPosition());

                if (fragment instanceof EditableListFragmentBase<?>) {
                    EditableListFragmentBase<?> editableListFragment = (EditableListFragmentBase<?>) fragment;
                    EditableListAdapterBase<?> adapter = editableListFragment.getAdapterImpl();

                    attachListeners(editableListFragment);

                    if (editableListFragment.getAdapterImpl() != null)
                        new Handler(Looper.getMainLooper()).postDelayed(adapter::syncAllAndNotify, 200);
                }
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
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    public void onBackPressed()
    {
        if (mBackPressedListener == null || !mBackPressedListener.onBackPressed()) {
            if (SelectionUtils.getTotalSize(mPerformerEngine) > 0) {
                new AlertDialog.Builder(this)
                        .setMessage(R.string.ques_cancelSelection)
                        .setNegativeButton(R.string.butn_no, null)
                        .setPositiveButton(R.string.butn_yes, (dialog, which) -> finish())
                        .show();
            } else
                super.onBackPressed();
        }
    }

    public void attachListeners(EditableListFragmentBase<?> fragment)
    {
        mMenuCallback.setForegroundConnection(fragment.getEngineConnection());
        mBackPressedListener = fragment instanceof OnBackPressedListener ? (OnBackPressedListener) fragment : null;
    }

    @Nullable
    @Override
    public IPerformerEngine getPerformerEngine()
    {
        return mPerformerEngine;
    }
}
