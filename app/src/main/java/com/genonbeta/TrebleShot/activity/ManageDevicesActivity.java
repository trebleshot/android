package com.genonbeta.TrebleShot.activity;

import android.os.Bundle;
import android.view.MenuItem;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.app.Activity;

import androidx.annotation.Nullable;

public class ManageDevicesActivity extends Activity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_devices);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_close_white_24dp);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        int id = item.getItemId();

        if (id == android.R.id.home)
            finish();
        else
            return super.onOptionsItemSelected(item);

        return true;
    }
}
