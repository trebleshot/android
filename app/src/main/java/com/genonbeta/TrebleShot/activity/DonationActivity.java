package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.fragment.external.GitHubContributorsListFragment;

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
	}
}
