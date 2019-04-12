package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.Nullable;
import androidx.annotation.TransitionRes;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;
import androidx.transition.TransitionManager;

/**
 * created by: veli
 * date: 4/6/19 4:57 PM
 */
public class WebShareActivity extends Activity
{
    public static final String EXTRA_WEBSERVER_START_REQUIRED = "extraStartRequired";

    private FloatingActionButton mFAB;
    private IntentFilter mFilter = new IntentFilter();
    private BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            if (CommunicationService.ACTION_WEBSHARE_STATUS.equals(intent.getAction()))
                updateWebShareStatus(intent.getBooleanExtra(CommunicationService.EXTRA_STATUS_STARTED,
                        false));
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web_share);

        mFilter.addAction(CommunicationService.ACTION_WEBSHARE_STATUS);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null)
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFAB = findViewById(R.id.content_fab);
        mFAB.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                toggleWebShare(false);
            }
        });

        if (getIntent() != null && getIntent().hasExtra(EXTRA_WEBSERVER_START_REQUIRED)
                && getIntent().getBooleanExtra(EXTRA_WEBSERVER_START_REQUIRED, false))
            toggleWebShare(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        registerReceiver(mReceiver, mFilter);
        requestWebShareStatus();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        unregisterReceiver(mReceiver);
        mFAB.setAnimation(null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }

    public void requestWebShareStatus()
    {
        AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
                .setAction(CommunicationService.ACTION_REQUEST_WEBSHARE_STATUS));
    }

    public void toggleWebShare(boolean forceStart)
    {
        Intent intent = new Intent(this, CommunicationService.class)
                .setAction(CommunicationService.ACTION_TOGGLE_WEBSHARE);

        if (forceStart)
            intent.putExtra(CommunicationService.EXTRA_TOGGLE_WEBSHARE_START_ALWAYS, true);

        AppUtils.startForegroundService(this, intent);
    }

    public void updateWebShareStatus(boolean running)
    {
        mFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                running ? R.color.colorError : R.color.colorSecondary)));
        mFAB.setImageResource(running ? R.drawable.ic_pause_white_24dp
                : R.drawable.ic_play_arrow_white_24dp);

        if (mFAB.getLayoutParams() instanceof CoordinatorLayout.LayoutParams) {
            ((CoordinatorLayout.LayoutParams) mFAB.getLayoutParams()).gravity = running
                    ? Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL
                    : Gravity.CENTER;

            mFAB.setLayoutParams(mFAB.getLayoutParams());

            if (mFAB.getParent() != null && mFAB.getParent() instanceof ViewGroup)
                TransitionManager.beginDelayedTransition((ViewGroup) mFAB.getParent());
        }

        if (running) {
            mFAB.setAnimation(null);
        } else {
            mFAB.setVisibility(View.VISIBLE);
            mFAB.setAnimation(AnimationUtils.loadAnimation(this, R.anim.pulse));
        }
    }
}
