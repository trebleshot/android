package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.fragment.external.InAppDonationItemListFragment;

/**
 * created by: veli
 * date: 7/12/18 10:32 PM
 */
public class DonationActivity extends Activity
{
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donation);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		if (getSupportActionBar() != null)
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		InAppDonationItemListFragment contributorsListFragment = (InAppDonationItemListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_about_contributors_fragment);

		if (contributorsListFragment != null)
			contributorsListFragment.getListView().setNestedScrollingEnabled(false);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();

		if (id == android.R.id.home)
			onBackPressed();
		else
			return super.onOptionsItemSelected(item);

		return true;
	}
}