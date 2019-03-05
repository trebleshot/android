package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.fragment.external.InAppDonationItemListFragment;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

/**
 * created by: veli
 * date: 7/12/18 10:32 PM
 */
public class DonationActivity extends Activity
{
	private Animation mAnimation;
	private TextView mDeveloperText;
	private String[] mTexts;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_donation);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		mDeveloperText = findViewById(R.id.developerText);
		mTexts = getString(R.string.text_prefaceDonation).split(";");

		if (getSupportActionBar() != null)
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		InAppDonationItemListFragment contributorsListFragment = (InAppDonationItemListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_about_contributors_fragment);

		if (contributorsListFragment != null)
			contributorsListFragment.getListView().setNestedScrollingEnabled(false);

		AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);

		alphaAnimation.setDuration(3000);
		alphaAnimation.setRepeatCount(Animation.INFINITE);
		alphaAnimation.setRepeatMode(Animation.REVERSE);
		alphaAnimation.setAnimationListener(new Animation.AnimationListener()
		{
			int mAnimPoint;
			int mTextPoint;

			@Override
			public void onAnimationStart(Animation animation)
			{
				mAnimPoint = 0;
				mTextPoint = 0;

				mDeveloperText.setText(mTexts[mTextPoint]);
				changeSpeed();
			}

			@Override
			public void onAnimationEnd(Animation animation)
			{

			}

			@Override
			public void onAnimationRepeat(Animation animation)
			{
				mAnimPoint++;

				if (mAnimPoint % 2 == 0) {
					mAnimPoint = 0;
					mTextPoint = mTextPoint + 1 >= mTexts.length ? 0 : mTextPoint + 1;
					mDeveloperText.setText(mTexts[mTextPoint]);

					changeSpeed();
				}
			}

			void changeSpeed() {
				mAnimation.setDuration(30 * mDeveloperText.getText().length());
			}
		});

		mAnimation = alphaAnimation;
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		mDeveloperText.setAnimation(mAnimation);
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mDeveloperText.setAnimation(null);
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
