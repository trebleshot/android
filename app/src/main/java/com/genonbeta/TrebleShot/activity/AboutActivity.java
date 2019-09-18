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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.fragment.external.GitHubContributorsListFragment;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;

public class AboutActivity extends Activity
{
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        findViewById(R.id.orgIcon).setOnClickListener(v -> startActivity(new Intent(
                Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_REPO_ORG))));

        findViewById(R.id.activity_about_see_source_layout).setOnClickListener(
                view -> startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_REPO_APP))));

        findViewById(R.id.activity_about_translate_layout).setOnClickListener(view -> startActivity(
                new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TRANSLATE))));

        findViewById(R.id.activity_about_changelog_layout).setOnClickListener(view -> startActivity(
                new Intent(AboutActivity.this, ChangelogActivity.class)));

        findViewById(R.id.activity_about_telegram_layout).setOnClickListener(view -> startActivity(
                new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TELEGRAM_CHANNEL))));

        findViewById(R.id.activity_about_option_fourth_layout).setOnClickListener(view -> {
            if (Keyword.Flavor.googlePlay.equals(AppUtils.getBuildFlavor())) {
                try {
                    startActivity(new Intent(AboutActivity.this, Class.forName("com.genonbeta.TrebleShot.activity.DonationActivity")));
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            } else
                UpdateUtils.checkForUpdates(AboutActivity.this, UpdateUtils.getDefaultUpdater(AboutActivity.this), true, null);
        });

        findViewById(R.id.activity_about_third_party_libraries_layout).setOnClickListener(
                v -> startActivity(new Intent(AboutActivity.this, ThirdPartyLibrariesActivity.class)));

        GitHubContributorsListFragment contributorsListFragment = (GitHubContributorsListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_about_contributors_fragment);

        if (contributorsListFragment != null)
            contributorsListFragment.getListView().setNestedScrollingEnabled(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        getMenuInflater().inflate(R.menu.actions_about, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        } else if (id == R.id.actions_about_feedback) {
            AppUtils.createFeedbackIntent(AboutActivity.this);
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        // calling this in the onCreate sequence causes theming issues
        if (!Keyword.Flavor.googlePlay.equals(AppUtils.getBuildFlavor())
                && UpdateUtils.hasNewVersion(getApplicationContext()))
            highlightUpdater(findViewById(R.id.activity_about_option_fourth_text),
                    UpdateUtils.getAvailableVersion(getApplicationContext()));
    }

    private void highlightUpdater(TextView textView, String availableVersion)
    {
        textView.setTextColor(ContextCompat.getColor(getApplicationContext(), AppUtils.getReference(AboutActivity.this, R.attr.colorAccent)));
        textView.setText(R.string.text_newVersionAvailable);
    }
}
