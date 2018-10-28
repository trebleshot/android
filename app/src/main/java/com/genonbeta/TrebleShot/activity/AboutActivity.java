package com.genonbeta.TrebleShot.activity;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.config.AppConfig;
import com.genonbeta.TrebleShot.config.Keyword;
import com.genonbeta.TrebleShot.fragment.external.GitHubContributorsListFragment;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.util.UpdateUtils;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

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

        findViewById(R.id.orgIcon).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_REPO_ORG)));
            }
        });

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

        findViewById(R.id.activity_about_changelog_layout).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(AboutActivity.this, ChangelogActivity.class));
            }
        });

        findViewById(R.id.activity_about_telegram_layout).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                startActivity(new Intent(Intent.ACTION_VIEW).setData(Uri.parse(AppConfig.URI_TELEGRAM_CHANNEL)));
            }
        });

        findViewById(R.id.activity_about_option_fourth_layout).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (Keyword.Flavor.googlePlay.equals(AppUtils.getBuildFlavor())) {
                    try {
                        startActivity(new Intent(AboutActivity.this, Class.forName("com.genonbeta.TrebleShot.activity.DonationActivity")));
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                } else
                    UpdateUtils.checkForUpdates(AboutActivity.this, UpdateUtils.getDefaultUpdater(AboutActivity.this), true, null);
            }
        });

        findViewById(R.id.activity_about_third_party_libraries_layout).setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                startActivity(new Intent(AboutActivity.this, ThirdPartyLibrariesActivity.class));
            }
        });

        GitHubContributorsListFragment contributorsListFragment = (GitHubContributorsListFragment) getSupportFragmentManager().findFragmentById(R.id.activity_about_contributors_fragment);

        if (contributorsListFragment != null)
            contributorsListFragment.getListView().setNestedScrollingEnabled(false);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
        } else
            return super.onOptionsItemSelected(item);

        return true;
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        findViewById(R.id.logo).setAnimation(AnimationUtils.loadAnimation(this, R.anim.rotate));

        // calling this in the onCreate sequence causes theming issues
        if (!Keyword.Flavor.googlePlay.equals(AppUtils.getBuildFlavor())
                && UpdateUtils.hasNewVersion(getApplicationContext()))
            highlightUpdater((TextView) findViewById(R.id.activity_about_option_fourth_text),
                    UpdateUtils.getAvailableVersion(getApplicationContext()));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        findViewById(R.id.logo).setAnimation(null);
    }

    private void highlightUpdater(TextView textView, String availableVersion)
    {
        textView.setTextColor(ContextCompat.getColor(getApplicationContext(), AppUtils.getReference(AboutActivity.this, R.attr.colorAccent)));
        textView.setText(R.string.text_newVersionAvailable);
    }

    public void preparePortal(final View view, final Animation animation)
    {
        if (view.isShown()) {
            view.setTag(new Object());

            new Handler().postDelayed(new Runnable()
            {
                @Override
                public void run()
                {
                    long currentDuration = animation.getDuration();

                    if (currentDuration < 1200) {
                        animation.setDuration(currentDuration + 1);
                        preparePortal(view, animation);
                    } else {
                        animation.setRepeatCount(1);
                        AppUtils.requestPortal(AboutActivity.this);
                    }
                }
            }, 10);
        }
    }

    public void requestPortal(View view)
    {
        if (view instanceof ImageView) {
            final Animation animation = findViewById(R.id.logo).getAnimation();

            if (animation != null)
                if (view.getTag() == null) {
                    if (animation.getDuration() - 50 >= 100)
                        animation.setDuration(animation.getDuration() - 50);
                    else {
                        preparePortal(view, animation);

                        ImageViewCompat.setImageTintList((ImageView) view, ColorStateList
                                .valueOf(ContextCompat.getColor(AboutActivity.this,
                                        AppUtils.getReference(AboutActivity.this, R.attr.colorDonation))));
                    }
                }
        }
    }
}
