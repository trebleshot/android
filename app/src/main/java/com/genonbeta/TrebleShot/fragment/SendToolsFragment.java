package com.genonbeta.TrebleShot.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;

public class SendToolsFragment extends Fragment
{
	private AbstractMediaListFragment mApps;
	private AbstractMediaListFragment mMusic;
	private AbstractMediaListFragment mVideos;
	private AbstractMediaListFragment mFragment;
	private TextView mAppsView;
	private TextView mMusicView;
	private TextView mVideosView;
	private SearchView mSearchView;
	private SearchComposer mSearchComposer = new SearchComposer();

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		View view = inflater.inflate(R.layout.fragment_send_tools, container, false);
		
		mAppsView = (TextView)view.findViewById(R.id.showApps);
		mMusicView = (TextView)view.findViewById(R.id.showMusic);
		mVideosView = (TextView)view.findViewById(R.id.showVideos);
		
		return view;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		mApps = new ApplicationListFragment();
		mMusic = new MusicListFragment();
		mVideos = new VideoListFragment();
		
		mAppsView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View p1)
				{
					showApps();
				}
			}
		);

		mMusicView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View p1)
				{
					showMusic();
				}
			}
		);

		mVideosView.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View p1)
				{
					showVideos();
				}
			}
		);
		
		showApps();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
	{
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.search_menu, menu);
		
		mSearchView = (SearchView)menu.findItem(R.id.search).getActionView();
		
		setupSearchView();
	}
	
	public void setupSearchView()
	{
		mSearchView.setOnQueryTextListener(mSearchComposer);
	}
	
	public void showApps()
	{
		if (mFragment != mApps)
			changeFragment(mApps);
	}

	public void showMusic()
	{
		if (mFragment != mMusic)
			changeFragment(mMusic);
	}

	public void showVideos()
	{
		if (mFragment != mVideos)
			changeFragment(mVideos);
	}
	
	public boolean changeFragment(AbstractMediaListFragment fragment)
	{
		FragmentTransaction ft = getFragmentManager().beginTransaction();
		
		if (mFragment != null)
		{
			if (mFragment.isLoading())
				return false;
				
			mFragment.warnBeforeRemove();
			ft.remove(mFragment);
		}
		
		AbstractMediaListFragment findOlds = (AbstractMediaListFragment)getFragmentManager().findFragmentByTag("currentFragment");
		
		if (findOlds != null)
		{
			if (findOlds.isLoading())
				return false;
			
			findOlds.warnBeforeRemove();
			ft.remove(findOlds);
		}
			
		if (fragment != null)
			ft.add(R.id.fragmentContainer, fragment, "currentFragment");

		ft.commit();
			
		mFragment = fragment;
		
		return true;
	}
	
	private class SearchComposer implements SearchView.OnQueryTextListener
	{
		@Override
		public boolean onQueryTextSubmit(String word)
		{
			mFragment.search(word);
			return false;
		}

		@Override
		public boolean onQueryTextChange(String word)
		{
			mFragment.search(word);
			return false;
		}
	}
}
