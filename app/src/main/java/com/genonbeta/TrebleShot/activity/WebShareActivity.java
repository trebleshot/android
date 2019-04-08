package com.genonbeta.TrebleShot.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

/**
 * created by: veli
 * date: 4/6/19 4:57 PM
 */
public class WebShareActivity extends Activity
{
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
                toggleWebShare();
            }
        });
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

    public void toggleWebShare()
    {
        AppUtils.startForegroundService(this, new Intent(this, CommunicationService.class)
                .setAction(CommunicationService.ACTION_TOGGLE_WEBSHARE));
    }

    public void updateWebShareStatus(boolean running)
    {
        mFAB.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this,
                running ? R.color.colorError : R.color.colorSecondary)));
        mFAB.setImageResource(running ? R.drawable.ic_pause_white_24dp
                : R.drawable.ic_play_arrow_white_24dp);
    }
}
