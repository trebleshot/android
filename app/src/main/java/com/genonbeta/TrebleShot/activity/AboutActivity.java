package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.fragment.external.GitHubContributorsListFragment;
import com.genonbeta.TrebleShot.util.UpdateUtils;

import velitasali.updatewithgithub.GitHubUpdater;

public class AboutActivity extends Activity
{
	private TextView mTextUpdates;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_about);

		Toolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		final GitHubUpdater updater = UpdateUtils.getDefaultUpdater(getApplicationContext());
		mTextUpdates = findViewById(R.id.activity_about_update_text);

		findViewById(R.id.fab).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setType("*/*");
				intent.setData(Uri.parse("mailto:" + AppConfig.EMAIL_DEVELOPER));
				intent.putExtra(Intent.EXTRA_EMAIL, AppConfig.EMAIL_DEVELOPER);
				intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.text_appName));

				startActivity(Intent.createChooser(intent, getString(R.string.text_application)));
			}
		});

		findViewById(R.id.activity_about_see_source_layout).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_REPO_APP)));
			}
		});

		findViewById(R.id.activity_about_translate_layout).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TRANSLATE)));
			}
		});

		findViewById(R.id.activity_about_update_layout).setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View view)
			{
				UpdateUtils.checkForUpdates(AboutActivity.this, updater, true, null);
			}
		});

		if (UpdateUtils.hasNewVersion(getApplicationContext()))
			highlightUpdater(UpdateUtils.getAvailableVersion(getApplicationContext()));
		else
			UpdateUtils.checkForUpdates(getApplicationContext(), updater, false, new GitHubUpdater.OnInfoAvailableListener()
			{
				@Override
				public void onInfoAvailable(boolean newVersion, String versionName, String title, String description, String releaseDate)
				{
					if (newVersion)
						highlightUpdater(versionName);
				}
			});

		GitHubContributorsListFragment contributorsListFragment = (GitHubContributorsListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_about_contributors_fragment);

		if (contributorsListFragment != null)
			contributorsListFragment.getListView().setNestedScrollingEnabled(false);
	}

	private void highlightUpdater(String availableVersion)
	{
		mTextUpdates.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.colorAccent));
		mTextUpdates.setText(R.string.text_newVersionAvailable);
	}
}
