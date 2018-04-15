package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.Toolbar;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.adapter.DefaultFragmentPagerAdapter;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.fragment.ApplicationListFragment;
import com.genonbeta.TrebleShot.fragment.ImageListFragment;
import com.genonbeta.TrebleShot.fragment.MusicListFragment;
import com.genonbeta.TrebleShot.fragment.VideoListFragment;
import com.genonbeta.TrebleShot.ui.callback.SharingActionModeCallback;
import com.genonbeta.TrebleShot.widget.EditableListAdapter;
import com.genonbeta.TrebleShot.widget.PowerfulActionMode;

import java.util.ArrayList;

/**
 * created by: veli
 * date: 13/04/18 19:45
 * p.s.: if anyone could do all this without making them generified I would be much surprised.
 */
public class ContentSharingActivity extends Activity
{
	public static final String TAG = ContentSharingActivity.class.getSimpleName();

	private static ArrayList mSavedSelectionLists;

	private PowerfulActionMode.SelectorConnection mSelectorConnection;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_content_sharing);

		final Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		final PowerfulActionMode actionMode = findViewById(R.id.activity_content_sharing_action_mode);
		final TabLayout tabLayout = findViewById(R.id.activity_content_sharing_tab_layout);
		final ViewPager viewPager = findViewById(R.id.activity_content_sharing_view_pager);

		final EditableListFragment appFragment = new ApplicationListFragment();
		final EditableListFragment musicFragment = new MusicListFragment();
		final EditableListFragment photoFragment = new ImageListFragment();
		final EditableListFragment videoFragment = new VideoListFragment();

		final SharingActionModeCallback selectionCallback = new SharingActionModeCallback(null);
		mSelectorConnection = new PowerfulActionMode.SelectorConnection(actionMode, selectionCallback);

		final EditableListFragment.LayoutClickListener groupLayoutClickListener
				= new EditableListFragment.LayoutClickListener()
		{
			@Override
			public boolean onLayoutClick(EditableListFragment listFragment, EditableListAdapter.EditableViewHolder holder, boolean longClick)
			{
				if (longClick)
					return listFragment.onDefaultClickAction(holder);

				return mSelectorConnection.setSelected(holder);
			}
		};

		final DefaultFragmentPagerAdapter pagerAdapter = new DefaultFragmentPagerAdapter(this, getSupportFragmentManager())
		{
			@Override
			public Object instantiateItem(ViewGroup container, int position)
			{
				EditableListFragment fragment = (EditableListFragment) super.instantiateItem(container, position);

				fragment.setSelectionCallback(selectionCallback);
				fragment.setSelectorConnection(mSelectorConnection);
				fragment.setLayoutClickListener(groupLayoutClickListener);

				if (viewPager.getCurrentItem() == position)
					selectionCallback.updateProvider(fragment);

				return fragment;
			}
		};

		actionMode.setContainerLayout(findViewById(R.id.activity_content_sharing_action_mode_layout));
		tabLayout.setTabGravity(TabLayout.GRAVITY_CENTER);

		pagerAdapter.add(appFragment, tabLayout);
		pagerAdapter.add(musicFragment, tabLayout);
		pagerAdapter.add(photoFragment, tabLayout);
		pagerAdapter.add(videoFragment, tabLayout);

		viewPager.setAdapter(pagerAdapter);
		viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabLayout));

		tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener()
		{
			@Override
			public void onTabSelected(TabLayout.Tab tab)
			{
				viewPager.setCurrentItem(tab.getPosition());
				selectionCallback.updateProvider((EditableListFragmentImpl) pagerAdapter.getItem(viewPager.getCurrentItem()));
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
	protected void onStart()
	{
		super.onStart();

		if (mSelectorConnection.getSelectedItemList().size() > 0
				&& mSelectorConnection.selectionActive())
			mSelectorConnection.getMode().start(mSelectorConnection.getCallback());
	}
}
