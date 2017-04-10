package com.genonbeta.TrebleShot.support;

import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.support.annotation.LayoutRes;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

public class AppCompatPreferenceFragment extends PreferenceFragment
{
	private AppCompatDelegate mDelegate;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		getDelegate().installViewFactory();
		getDelegate().onCreate(savedInstanceState);

		super.onCreate(savedInstanceState);
	}

	public ActionBar getSupportActionBar()
	{
		return getDelegate().getSupportActionBar();
	}

	public void setSupportActionBar(@Nullable Toolbar toolbar)
	{
		getDelegate().setSupportActionBar(toolbar);
	}

	public MenuInflater getMenuInflater()
	{
		return getDelegate().getMenuInflater();
	}

	public void setContentView(@LayoutRes int layoutResID)
	{
		getDelegate().setContentView(layoutResID);
	}

	public void setContentView(View view)
	{
		getDelegate().setContentView(view);
	}

	public void setContentView(View view, ViewGroup.LayoutParams params)
	{
		getDelegate().setContentView(view, params);
	}

	public void addContentView(View view, ViewGroup.LayoutParams params)
	{
		getDelegate().addContentView(view, params);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
		getDelegate().onConfigurationChanged(newConfig);
	}

	@Override
	public void onStop()
	{
		super.onStop();
		getDelegate().onStop();
	}

	@Override
	public void onDestroy()
	{
		super.onDestroy();
		getDelegate().onDestroy();
	}

	public void invalidateOptionsMenu()
	{
		getDelegate().invalidateOptionsMenu();
	}

	private AppCompatDelegate getDelegate()
	{
		if (mDelegate == null)
			mDelegate = AppCompatDelegate.create(getActivity(), null);

		return mDelegate;
	}
}
